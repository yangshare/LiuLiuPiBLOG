-- 图片 URL 域名解耦迁移
-- =====================================================================
-- 执行前置（务必逐项完成，不可跳过）：
--   1. 在维护窗口执行；执行前停止文章/评论/头像/资源等写入（上传、编辑、删除）。
--   2. 先完整备份数据库：
--        mysqldump -u<user> -p liuliupi_blog > backup_20260801.sql
--   3. 把下面 @old 设为「实际历史 CDN 域名前缀」，且必须与库里写入的域名【逐字一致】
--      （协议 http/https、大小写、尾斜杠都要匹配）。关键约束：
--        a. @old 必须以 / 结尾（如 'https://file.yangshare.com/'）。切勿去掉尾斜杠：
--           无尾斜杠会让 '...com/a.jpg' 变成 '/a.jpg'（带前导斜杠），违反 key/relativePath
--           约定，并破坏 resource 删除链路与唯一键冲突检测（A-2）。
--        b. REPLACE 按字节大小写敏感匹配；若库里域名大小写不一（如 HTTPS://），必须按实际
--           大小写逐个设置 @old 重跑，否则 A-1 命中但阶段 C 不替换、阶段 D 残留非零。
--        c. @old 不得为空：空值会使 LIKE '%%' 命中所有非 NULL 行、触发全表 UPDATE。
--      若历史上存在多个域名，必须逐个设置 @old 后【重跑本脚本全部步骤】（从「预检」开始），
--      每个域名独立跑一遍。切勿只跑一次就认为完成。
--   4. 本脚本只迁移确认属于图片的字段；article.video_url 与 resource_path.url 保持不变。
--   5. 执行前确认当前连接的是目标数据库：先运行 SELECT DATABASE(); 核对（应为 liuliupi_blog
--      或你的实际库名）。本脚本未硬编码库名，切勿在 GUI 中误选其他 schema 执行。
-- =====================================================================

-- 真实历史域名（liuliupi_blog 库 2026-08-01 执行时确认）共 4 个变体，逐个设置 @old 重跑全部步骤：
--   1. 'https://oss.yangshare.com/'   主力，116+ 行（article_cover/content、resource、user、tree_hole、family）
--   2. 'https://qiniu.yangshare.com/' 5 行（web_info 4 字段 + article_content）
--   3. 'http://qiniu.yangshare.com/'  4 行（注意 http，article_cover/content）
--   4. 'http://oss.yangshare.com/'    1 行（藏于含 https://oss 的同一 article_content，首遍清 https 后才暴露）
-- 第三方外链（github/jsdelivr/gitee/cdn-doocs/alist 等约 30+ 条）不含上述前缀，REPLACE 不会误伤。
-- 注意：本库 collation 不统一（18 表 utf8mb4_general_ci，push_notification 为 utf8mb4_0900_ai_ci），
--       故所有 LIKE 右侧已加 COLLATE utf8mb4_general_ci 显式对齐；REPLACE 仍按字节精确替换不受影响。
SET @old := 'https://oss.yangshare.com/';

-- -------------------------------------------------------------------
-- 阶段 A：dry-run 预检（事务外执行）。执行者必须人工核对下列输出后再进入阶段 C。
-- -------------------------------------------------------------------

-- A-1：各图片字段命中行数（应与迁移后残留检查一一对应）
SELECT 'article.article_cover' AS field_name, COUNT(*) AS hit
FROM article WHERE article_cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'article.article_content', COUNT(*)
FROM article WHERE article_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'user.avatar', COUNT(*)
FROM `user` WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.background_image', COUNT(*)
FROM web_info WHERE background_image LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.avatar', COUNT(*)
FROM web_info WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.random_avatar', COUNT(*)
FROM web_info WHERE random_avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.random_cover', COUNT(*)
FROM web_info WHERE random_cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'push_notification.cover', COUNT(*)
FROM push_notification WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource_path.cover', COUNT(*)
FROM resource_path WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource.path', COUNT(*)
FROM resource WHERE path LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'family.bg_cover/man_cover/woman_cover', COUNT(*)
FROM family WHERE CONCAT_WS('|', bg_cover, man_cover, woman_cover) LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'tree_hole.avatar', COUNT(*)
FROM tree_hole WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_group.avatar', COUNT(*)
FROM im_chat_group WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'comment.comment_content', COUNT(*)
FROM comment WHERE comment_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_user_message.content', COUNT(*)
FROM im_chat_user_message WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_user_group_message.content', COUNT(*)
FROM im_chat_user_group_message WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

-- A-2：resource.path 去域名后的唯一键冲突预检。
--     输出必须为 0 行；若有冲突，必须先人工合并，禁止执行阶段 C 的 UPDATE。
SELECT REPLACE(path, @old, '') AS target_path, COUNT(*) AS duplicates
FROM resource
GROUP BY REPLACE(path, @old, '')
HAVING COUNT(*) > 1;

-- A-3：显式排除项预检——确认这些「非图片」字段不会被本脚本触碰。
--      预期：video_url / resource_path.url 即便含 @old，下方 UPDATE 也不会改动它们。
SELECT 'article.video_url(excluded,expect-unchanged)' AS field_name, COUNT(*) AS hit
FROM article WHERE video_url LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource_path.url(excluded,expect-unchanged)', COUNT(*)
FROM resource_path WHERE url LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

-- A-4：web_info 随机字段为 JSON 字符串，确认替换后仍可解析。
--      预期：JSON_VALID = 1（若某行为 0，记录 id 后单独人工处理）。
SELECT id,
       JSON_VALID(random_avatar) AS valid_random_avatar,
       JSON_VALID(random_cover)  AS valid_random_cover
FROM web_info
WHERE random_avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
   OR random_cover  LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

-- -------------------------------------------------------------------
-- 阶段 B：确认 A-2 冲突为 0、A-4 全部 JSON_VALID=1 后，方可执行阶段 C。
-- 【失败必须回滚】阶段 C 任一 UPDATE 报错，立即执行 ROLLBACK 并停止，禁止继续执行后续
--                语句或 COMMIT（否则会部分提交，造成部分迁移）。用 mysql 客户端时确保
--                未启用 --force（默认遇错即停）；GUI 工具需手动监控并 ROLLBACK。
-- -------------------------------------------------------------------

START TRANSACTION;

UPDATE article SET article_cover = REPLACE(article_cover, @old, '')
WHERE article_cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE article SET article_content = REPLACE(article_content, @old, '')
WHERE article_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE `user` SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE web_info SET background_image = REPLACE(background_image, @old, ''),
                    avatar = REPLACE(avatar, @old, ''),
                    random_avatar = REPLACE(random_avatar, @old, ''),
                    random_cover = REPLACE(random_cover, @old, '')
WHERE CONCAT_WS('|', background_image, avatar, random_avatar, random_cover)
      LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE push_notification SET cover = REPLACE(cover, @old, '')
WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE resource_path SET cover = REPLACE(cover, @old, '')
WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE resource SET path = REPLACE(path, @old, '')
WHERE path LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE family SET bg_cover = REPLACE(bg_cover, @old, ''),
                 man_cover = REPLACE(man_cover, @old, ''),
                 woman_cover = REPLACE(woman_cover, @old, '')
WHERE CONCAT_WS('|', bg_cover, man_cover, woman_cover) LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE tree_hole SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE im_chat_group SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE comment SET comment_content = REPLACE(comment_content, @old, '')
WHERE comment_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE im_chat_user_message SET content = REPLACE(content, @old, '')
WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;
UPDATE im_chat_user_group_message SET content = REPLACE(content, @old, '')
WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

COMMIT;

-- -------------------------------------------------------------------
-- 阶段 D：迁移后残留检查。所有 residual 必须为 0（覆盖全部图片字段，与 A-1 预检一一对应）。
--         若任一行 > 0，说明该字段被漏迁或 REPLACE 未命中，按字段排查并补迁。
-- -------------------------------------------------------------------
SELECT 'article.article_cover' AS field_name, COUNT(*) AS residual
FROM article WHERE article_cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'article.article_content', COUNT(*)
FROM article WHERE article_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'user.avatar', COUNT(*)
FROM `user` WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.background_image', COUNT(*)
FROM web_info WHERE background_image LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.avatar', COUNT(*)
FROM web_info WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.random_avatar', COUNT(*)
FROM web_info WHERE random_avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'web_info.random_cover', COUNT(*)
FROM web_info WHERE random_cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'push_notification.cover', COUNT(*)
FROM push_notification WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource_path.cover', COUNT(*)
FROM resource_path WHERE cover LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource.path', COUNT(*)
FROM resource WHERE path LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'family.bg_cover/man_cover/woman_cover', COUNT(*)
FROM family WHERE CONCAT_WS('|', bg_cover, man_cover, woman_cover) LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'tree_hole.avatar', COUNT(*)
FROM tree_hole WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_group.avatar', COUNT(*)
FROM im_chat_group WHERE avatar LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'comment.comment_content', COUNT(*)
FROM comment WHERE comment_content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_user_message.content', COUNT(*)
FROM im_chat_user_message WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'im_chat_user_group_message.content', COUNT(*)
FROM im_chat_user_group_message WHERE content LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

-- D-2：再次确认排除项确实未被改动。
SELECT 'article.video_url(must-be-unchanged)' AS field_name, COUNT(*) AS residual
FROM article WHERE video_url LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci
UNION ALL SELECT 'resource_path.url(must-be-unchanged)', COUNT(*)
FROM resource_path WHERE url LIKE CONCAT('%', @old, '%') COLLATE utf8mb4_general_ci;

-- =====================================================================
-- 执行记录（liuliupi_blog，2026-08-01）
-- =====================================================================
-- 备份：mysqldump --single-transaction 全量备份至 E:\db-backups\liuliupi_blog\（项目外，未入库）。
-- 执行方式：本机无 mysql client，通过 docker run --rm mysql:8.0.38 连接 -h 192.168.6.3 -P 3308，
--           分 4 个 @old 各跑一遍 A→C→D。
-- 结果（4 个域名累计 135 行，每遍 D 残留均为 0）：
--   @old = 'https://oss.yangshare.com/'   → 125 行，D 残留 0
--   @old = 'https://qiniu.yangshare.com/' →   5 行，D 残留 0（web_info JSON_VALID 保持 1）
--   @old = 'http://qiniu.yangshare.com/'  →   4 行，D 残留 0
--   @old = 'http://oss.yangshare.com/'    →   1 行，D 残留 0
-- 终极校验：16 字段对 (oss|qiniu).yangshare.com 大小写不敏感 REGEXP 残留全 0。
-- 端到端：迁移后 key 经 imageSrc 拼接 qiniu.downloadUrl(https://qiniu.yangshare.com/) HTTP HEAD 抽样均 200；
--         OSS 原域名已失效（ERR），迁移同时修复了这些断链。
-- 说明：迁移前所有图片字段为完整 URL，imageSrc 走透传分支，qiniu.downloadUrl 拼接分支从未被
--       生产数据触发；迁移后该分支首次启用并验证可用。