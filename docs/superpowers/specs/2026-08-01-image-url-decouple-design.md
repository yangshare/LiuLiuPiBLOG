# 图片 URL 域名解耦设计

## 背景

站点用户上传的图片（文章封面、正文配图、头像、评论图、照片墙、资源表等）统一存储于七牛云。当前实现中，图片上传成功后会把「七牛 CDN 域名 + key」拼成**完整 URL 写入数据库**：

- 前端七牛上传成功处:`url = sysConfig['qiniu.downloadUrl'] + key`
- 后端本地上传:`visitPath = downloadUrl + path`

最近七牛 CDN 域名发生变更，而库里存的是带旧域名的完整 URL，导致**整站文章（及头像、评论图等）图片全部失效**。改 `sys_config` 里的 `qiniu.downloadUrl` 也救不回库里已存的完整 URL，只能逐条改库。

### 现状架构事实

- `CustomEnvironmentPostProcessor` 在启动时把整张 `sys_config` 表加载为 Spring **最高优先级** PropertySource（`config_key → config_value`）。因此 `@Value("${qiniu.downloadUrl}")` 实际读的是数据库 `sys_config` 的值，`application.yml` 里 `qiniu.downloadUrl:` 仅为占位空值。
- 项目已有「前缀可配 + 渲染时拼接」的成熟模式：`webStaticResourcePrefix`（存在 `sys_config`），前端大量位置用 `$store.state.sysConfig['webStaticResourcePrefix'] + '相对路径'` 渲染 emoji、`assets/` 静态图、背景视频等。换静态资源域名只需改配置。
- 但**用户上传图片这条线没有套用同一模式**，仍在上传时拼死完整 URL 存库 —— 这正是本次要解决的问题。

## 目标

1. **修复历史数据**：把库里带旧 CDN 域名的完整 URL 转为不含域名的相对路径，使图片在新域名下恢复显示。
2. **未来域名变更不再改库**：以后七牛 CDN 域名（或本地下载域名）变化时，只改配置一处，全站图片立即跟随，无需修改数据库、无需重新上传。

## 非目标

- 不支持「同一部署同时混用本地与七牛两种图床」。运行时只使用一种存储（由 `defaultStoreType` 决定），与现有使用方式一致。
- 不迁移本地存储的历史数据。当前 `store.type = qiniu`、历史图片全在七牛，本地存储仅做代码层面的对称改造（防御未来启用时踩同样的坑），不存在需要迁移的本地历史数据。
- 不改动 `webStaticResourcePrefix` 静态资源机制。
- 不做后台可视化迁移界面，历史数据迁移使用裸 SQL 脚本 + 备份。
- 不引入新的图片域名配置项，复用已有的存储下载域名（`qiniu.downloadUrl` / `local.downloadUrl`）。

## 方案选择

选择 **根治型方案：相对路径/key 存库 + 渲染时按存储类型拼接下载域名 + 一次性迁移历史数据**。

前缀来源**复用现有存储下载域名**，不新增配置、不复用 `webStaticResourcePrefix`：

| 方案 | 取舍 |
|---|---|
| ✅ 复用存储下载域名（采用） | 零新增配置；语义准确（图片访问域名 = 存储下载域名）；与后端上传拼接逻辑对称（上传拼一次、渲染拼一次，同源）。仅需补发 `local.downloadUrl` 到前端。 |
| ❌ 新增 `imageResourcePrefix` | 显式独立，但与 `qiniu.downloadUrl`/`local.downloadUrl` 重叠，需多处维护同一域名。 |
| ❌ 复用 `webStaticResourcePrefix` | 语义错位：它管的是前端打包的静态资源（emoji/`assets/`/视频），与用户上传图片的图床域名物理上常不同域（本地存储必然不同），强行复用会引入隐式耦合。 |

## 详细设计

### 1. 数据约定

#### 1.1 path 格式

库里所有图片字段统一存**不含域名的相对路径**，不再出现 `https://cdn.xxx.com/key`：

| 存储 | 存什么 | 示例 |
|---|---|---|
| 七牛 | key | `article/u117000011234567890.jpg` |
| 本地 | relativePath | `commentPicture/u1xxx.jpg` |

#### 1.2 渲染拼接 —— `$common.imageSrc(path)`

新增工具函数，主站与 IM 各一份（`liuliupi-ui/src/utils/common.js`、`liuliupi-im-ui/src/utils/common.js`）：

```js
function imageSrc(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path        // 完整URL（外部图/漏迁历史）原样返回，保兼容
  const storeType = localStorage.getItem('defaultStoreType')
  const sysConfig = store.state.sysConfig
  const prefix = storeType === 'qiniu'
    ? sysConfig['qiniu.downloadUrl']
    : sysConfig['local.downloadUrl']
  return prefix.replace(/\/$/, '') + '/' + path.replace(/^\//, '')
}
```

- `http(s)://` 开头直接返回 —— 兼容外部图床与漏迁的历史完整 URL。
- `storeType` 取自已有的 `localStorage.defaultStoreType`。
- `qiniu.downloadUrl` 已在 `sys_config`，前端 `sysConfig` 可直接取，零改动。

### 2. 上传链路改造（改存 key，5 处）

**七牛（4 处，去掉 `qiniu.downloadUrl +` 拼接，只存 key）：**

| 文件 | 行 |
|---|---|
| `liuliupi-ui/src/components/common/uploadPicture.vue` | 151 |
| `liuliupi-ui/src/components/admin/postEdit.vue` | 230 |
| `liuliupi-ui/src/components/comment/graffiti.vue` | 312 |
| `liuliupi-im-ui/src/components/common/uploadPicture.vue` | 85 |

**本地（1 处，后端去掉 downloadUrl 前缀）：**

| 文件 | 行 | 改动 |
|---|---|---|
| `liuliupi-server/src/main/java/com/liuliupi/utils/storage/LocalUtil.java` | 88 | `setVisitPath(downloadUrl + path)` → `setVisitPath(path)` |

前端 local 分支 `uploadPicture.vue:149` `url = response.data` 自动跟随后端返回，无需单独改。

> `ImageUrlInput.vue`（封面输入框）、`$common.saveResource`（写 `resource.path`）均透传上传结果，自动拿到 key，不需单独改。

### 3. 渲染层改造

#### 3.1 字段直渲染

把 `:src="x"` 换成 `:src="$common.imageSrc(x)"`：

- 文章封面 `articleCover`：详情 `article.vue` + 列表页
- 用户头像 `avatar`：header / aside / 评论头像等多处
- 照片墙 `resourcePath.cover`：`photo.vue:11`
- 文章视频 `videoUrl`：⚠️待确认是否也在七牛（见「实现待核对项」）

> 实现时需 grep 全量覆盖所有 `:src` / `v-html` 涉及图片字段处，3.1/3.2 仅列出已定位的关键点。

#### 3.2 markdown / v-html 内容里的图片

不能直接换 `:src`，要在**渲染源头**给图片 src 拼前缀。抽出共享函数 `applyImagePrefix(md)`，在 markdown-it 实例上覆盖 image renderer rule：

| 内容 | 渲染位置 | 处理 |
|---|---|---|
| 文章正文 | `article.vue:521` `new MarkdownIt()` | 注入 image rule，给 `![alt](key)` 的 src 拼前缀 |
| 首页公告 | `index.vue:259` `new MarkdownIt()` | 同上（imageSrc 兼容完整 URL，外部图不受影响） |
| 评论 | `comment.vue:52,78` `v-html` | 评论图为 `[name,url]` 转 `<img>`，在转换处对 url 拼前缀 |
| 树洞/历程 | `treeHole.vue:17`、`process.vue:21` `v-html` | 同评论，在内容中图片 url 出现处拼前缀 |

> `article.vue` 与 `index.vue` 的 markdown-it image 前缀处理复用同一共享函数。

### 4. 后端：补发 `local.downloadUrl`

`qiniu.downloadUrl` 在 `sys_config` 表，后端 `@Value` 与前端 `sysConfig` 均可获取。`local.downloadUrl` 仅在 `application.yml`，**前端 `sysConfig` 拿不到**。

在 `/sysConfig/listSysConfig` 返回结果中，额外 put 一项 `local.downloadUrl`（值取自后端 `@Value("${local.downloadUrl}")`）。保持本地配置仍统一在 `application.yml`，不割裂到数据库。

> 本地存储当前未启用，此补发为「未来切到本地存储时前端能拿到前缀」的完整性保障。

### 5. 历史数据迁移

#### 5.1 涉及的表/字段

| 表 | 字段 | 说明 |
|---|---|---|
| `article` | `article_cover` | 文章封面 |
| `article` | `article_content` | 正文 markdown（图片 URL 内嵌） |
| `article` | `video_url` | 视频（⚠️待确认是否七牛） |
| `` `user` `` | `avatar` | 用户头像 |
| `comment` | `comment_content` | 评论 `[name,url]`（表名/字段名待核对 `Comment` 实体） |
| `resource_path` | `cover`, `url` | 照片墙 / 收藏 |
| `resource` | `path` | 所有上传资源记录 |
| `im_chat_user_message` | `content` | IM 用户消息 `[username,url]` |
| `im_chat_user_group_message` | `content` | IM 群消息 |
| `web_info` | `randomCover` / 背景图 | ⚠️存储方式（JSON？逗号分隔？）待核对 |

排除项：`wei_yan`（树洞/微言）`content` 为纯文本，无图片字段。

#### 5.2 迁移机制

核心：`REPLACE(field, '旧CDN域名/', '')` —— 纯字符串替换。无论字段是 markdown、`[name,url]` 还是纯 URL，把旧域名前缀替换为空即留 key。历史数据均为七牛，仅需 strip 七牛旧 CDN 域名。

#### 5.3 SQL 骨架

```sql
-- 0. 先备份!!  mysqldump -u root -p liuliupi_blog > backup_20260801.sql
-- 0.1 设置旧CDN域名（改成实际值，含 https:// 和结尾 /）
SET @old := 'https://file.yangshare.com/';

-- 1. dry-run 预览（只查不更新）
SELECT 'article_content' AS f, COUNT(*) AS hit FROM article WHERE article_content LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'article_cover',  COUNT(*) FROM article WHERE article_cover  LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'user.avatar',    COUNT(*) FROM `user` WHERE avatar LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource.path',  COUNT(*) FROM resource WHERE path LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'im_user_msg',    COUNT(*) FROM im_chat_user_message WHERE content LIKE CONCAT('%', @old, '%');
-- ...其余各表同构

-- 2. 执行 strip（dry-run 确认无误后）
UPDATE article SET article_content = REPLACE(article_content, @old, '') WHERE article_content LIKE CONCAT('%', @old, '%');
UPDATE article SET article_cover   = REPLACE(article_cover,   @old, '') WHERE article_cover   LIKE CONCAT('%', @old, '%');
UPDATE `user`  SET avatar          = REPLACE(avatar,          @old, '') WHERE avatar          LIKE CONCAT('%', @old, '%');
UPDATE resource SET path           = REPLACE(path,            @old, '') WHERE path            LIKE CONCAT('%', @old, '%');
UPDATE im_chat_user_message        SET content = REPLACE(content, @old, '') WHERE content LIKE CONCAT('%', @old, '%');
-- ...其余各表同构

-- 3. 验证（所有残留应为 0）
SELECT COUNT(*) AS residual FROM article WHERE article_content LIKE CONCAT('%', @old, '%');
-- 若历史上有过多个 CDN 域名：改 @old 重复步骤 1-3
```

### 6. 数据流

新数据上传（改造后）：

```
前端上传
  ├─ 七牛:  存 key            → article.article_cover / article_content / user.avatar / resource.path ...
  └─ 本地:  后端返回 path     → 同上
渲染:  $common.imageSrc(path)  → qiniu.downloadUrl/local.downloadUrl + path
```

域名变更时（改造后）：

```
改 sys_config.qiniu.downloadUrl（或 application.yml 的 local.downloadUrl）
  → 前端 sysConfig 更新 → imageSrc 自动拼新域名 → 全站图片跟随，零改库零重传
```

### 7. 错误处理

| 场景 | 处理 |
|---|---|
| `imageSrc` 收到空值 | 返回空字符串，不拼接 |
| `imageSrc` 收到完整 URL（外部图/漏迁历史） | 原样返回 |
| `local.downloadUrl` 未配置（本地未启用） | 不影响 qiniu 分支；启用本地时前端按当前前缀渲染 |
| 迁移漏掉某条记录 | 旧域名仍可达则图仍显示（imageSrc 兼容）；旧域名已失效则该条挂，靠迁移后残留 SELECT 兜底 |
| markdown 渲染异常 | 维持现有降级（显示原始内容），不白屏 |

### 8. 测试策略

#### 8.1 单元测试

- `$common.imageSrc`：完整 URL / 相对 key / local 分支 / qiniu 分支 / 空值，各分支断言。

#### 8.2 迁移验证

- dry-run `SELECT` 各表命中行数；
- 执行后 `SELECT COUNT(*) ... LIKE '%旧域名%'` 残留归零；
- 抽样确认 `article_content` 中 `![alt](key)`、评论 `[name,key]` 格式正确。

#### 8.3 端到端（核心验收）

- 改 `sys_config.qiniu.downloadUrl` 为新域名 → **不碰数据库、不重新上传**，文章封面/正文/头像/评论图全部切到新域名显示。
- 新上传一张图 → 库里存的是 key（不含域名）→ 前端渲染拼新域名显示。

#### 8.4 回归

- 外部图（http 完整 URL）不受影响。
- emoji / `assets/` 静态资源（走 `webStaticResourcePrefix`）不受影响。

## 风险与回退

| 风险 | 应对 |
|---|---|
| 迁移误伤非图片数据 | `REPLACE` 仅针对确定的旧域名字符串；执行前 dry-run 预览命中范围；全程备份。 |
| 迁移后仍有个别图挂 | 先按残留 SELECT 排查是否漏迁/多域名；imageSrc 兼容完整 URL 提供软兼容。 |
| 上线过渡期数据混存（新 key + 旧完整 URL） | imageSrc 对 `http://` 开头原样返回，过渡期两种数据都能正确渲染。 |
| 回退 | `source backup_20260801.sql` 恢复数据库；代码改动按 commit 回滚。 |

## 依赖

- 已有依赖：`markdown-it`（article/index 渲染）、`sys_config` 配置机制、`localStorage.defaultStoreType`
- 后端改动：`LocalUtil.saveFile`、`/sysConfig/listSysConfig` 补发 `local.downloadUrl`
- 数据库：一次性迁移 SQL（无表结构变更）

## 实现待核对项

实现阶段需精确确认以下内容（设计阶段已定位、未 100% 落实）：

- [ ] `article.video_url` 是否存七牛地址、是否纳入渲染拼接与迁移。
- [ ] `Comment` 实体的表名与图片字段名（迁移 SQL 中 `comment` / `comment_content`）。
- [ ] `web_info` 中 `randomCover` / 背景图的存储方式（JSON / 逗号分隔 / 多字段），据此决定迁移 SQL 写法。
- [ ] IM 消息表实际表名（`im_chat_user_message` / `im_chat_user_group_message`）与字段名。
- [ ] 渲染点全量：grep 所有 `:src` / `v-html` 涉及图片字段处，逐一替换为 `imageSrc` 或在渲染源头拼前缀。
- [ ] 旧 CDN 域名确切值（可从 `sys_config.qiniu.downloadUrl` 当前值或历史值获取），含协议与结尾斜杠。

## 验收标准

- [ ] 七牛上传成功后，库里存储的是 key（不含域名）；本地上传返回 path（不含域名）。
- [ ] `$common.imageSrc` 工具函数实现并通过单元测试。
- [ ] 所有图片渲染点（封面/正文/头像/评论/照片墙等）通过 `imageSrc` 或渲染源头拼前缀正确显示。
- [ ] `/sysConfig/listSysConfig` 返回包含 `local.downloadUrl`。
- [ ] 历史数据迁移脚本执行后，库中不含旧 CDN 域名（残留 SELECT 归零）。
- [ ] **核心**：仅修改 `qiniu.downloadUrl` 配置，全站图片切换到新域名，无需改库、无需重新上传。
- [ ] 外部图与静态资源不受影响。
