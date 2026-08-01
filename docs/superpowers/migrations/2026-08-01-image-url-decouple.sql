-- 图片 URL 域名解耦迁移
-- 使用前：备份数据库、确认 @old 与历史数据完全一致，并在维护窗口执行。
-- 本脚本只迁移确认属于图片的字段；article.video_url 与 resource_path.url 保持不变。

SET @old := 'https://file.yangshare.com/';

-- 预检：所有命中行数
SELECT 'article.article_cover' AS field_name, COUNT(*) AS hit
FROM article WHERE article_cover LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'article.article_content', COUNT(*)
FROM article WHERE article_content LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'user.avatar', COUNT(*)
FROM `user` WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'web_info.background_image', COUNT(*)
FROM web_info WHERE background_image LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'web_info.avatar', COUNT(*)
FROM web_info WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'web_info.random_avatar', COUNT(*)
FROM web_info WHERE random_avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'web_info.random_cover', COUNT(*)
FROM web_info WHERE random_cover LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'push_notification.cover', COUNT(*)
FROM push_notification WHERE cover LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource_path.cover', COUNT(*)
FROM resource_path WHERE cover LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource.path', COUNT(*)
FROM resource WHERE path LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'family.bg_cover/man_cover/woman_cover', COUNT(*)
FROM family WHERE CONCAT_WS('|', bg_cover, man_cover, woman_cover) LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'tree_hole.avatar', COUNT(*)
FROM tree_hole WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'im_chat_group.avatar', COUNT(*)
FROM im_chat_group WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'comment.comment_content', COUNT(*)
FROM comment WHERE comment_content LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'im_chat_user_message.content', COUNT(*)
FROM im_chat_user_message WHERE content LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'im_chat_user_group_message.content', COUNT(*)
FROM im_chat_user_group_message WHERE content LIKE CONCAT('%', @old, '%');

-- 预检：resource.path 去域名后不能出现唯一键冲突。
SELECT REPLACE(path, @old, '') AS target_path, COUNT(*) AS duplicates
FROM resource
GROUP BY REPLACE(path, @old, '')
HAVING COUNT(*) > 1;

-- 确认上述两个预检结果后再执行更新。
START TRANSACTION;

UPDATE article SET article_cover = REPLACE(article_cover, @old, '')
WHERE article_cover LIKE CONCAT('%', @old, '%');
UPDATE article SET article_content = REPLACE(article_content, @old, '')
WHERE article_content LIKE CONCAT('%', @old, '%');
UPDATE `user` SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%');
UPDATE web_info SET background_image = REPLACE(background_image, @old, ''),
                    avatar = REPLACE(avatar, @old, ''),
                    random_avatar = REPLACE(random_avatar, @old, ''),
                    random_cover = REPLACE(random_cover, @old, '')
WHERE CONCAT_WS('|', background_image, avatar, random_avatar, random_cover)
      LIKE CONCAT('%', @old, '%');
UPDATE push_notification SET cover = REPLACE(cover, @old, '')
WHERE cover LIKE CONCAT('%', @old, '%');
UPDATE resource_path SET cover = REPLACE(cover, @old, '')
WHERE cover LIKE CONCAT('%', @old, '%');
UPDATE resource SET path = REPLACE(path, @old, '')
WHERE path LIKE CONCAT('%', @old, '%');
UPDATE family SET bg_cover = REPLACE(bg_cover, @old, ''),
                 man_cover = REPLACE(man_cover, @old, ''),
                 woman_cover = REPLACE(woman_cover, @old, '')
WHERE CONCAT_WS('|', bg_cover, man_cover, woman_cover) LIKE CONCAT('%', @old, '%');
UPDATE tree_hole SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%');
UPDATE im_chat_group SET avatar = REPLACE(avatar, @old, '')
WHERE avatar LIKE CONCAT('%', @old, '%');
UPDATE comment SET comment_content = REPLACE(comment_content, @old, '')
WHERE comment_content LIKE CONCAT('%', @old, '%');
UPDATE im_chat_user_message SET content = REPLACE(content, @old, '')
WHERE content LIKE CONCAT('%', @old, '%');
UPDATE im_chat_user_group_message SET content = REPLACE(content, @old, '')
WHERE content LIKE CONCAT('%', @old, '%');

COMMIT;

-- 迁移后残留检查；应全部为 0。
SELECT 'article.article_cover' AS field_name, COUNT(*) AS residual
FROM article WHERE article_cover LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'article.article_content', COUNT(*)
FROM article WHERE article_content LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'user.avatar', COUNT(*)
FROM `user` WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource.path', COUNT(*)
FROM resource WHERE path LIKE CONCAT('%', @old, '%');
