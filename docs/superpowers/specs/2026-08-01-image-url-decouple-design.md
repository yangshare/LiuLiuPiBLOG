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
  if (typeof path !== 'string' || !path.trim()) return ''
  const value = path.trim()
  if (/^(https?:|\/\/|data:|blob:)/i.test(value)) return value
  const storeType = localStorage.getItem('defaultStoreType') || store.state.webInfo.defaultStoreType || 'qiniu'
  const sysConfig = store.state.sysConfig || {}
  const prefix = storeType === 'local'
    ? sysConfig['local.downloadUrl']
    : sysConfig['qiniu.downloadUrl']
  if (!prefix) return ''                         // 配置异步加载时不抛异常
  return prefix.replace(/\/+$/, '') + '/' + value.replace(/^\/+/, '')
}
```

- `http(s)://`、`//`、`data:`、`blob:` 开头直接返回 —— 兼容外部图床与漏迁的历史完整 URL。
- `storeType` 取自已有的 `localStorage.defaultStoreType`。
- `qiniu.downloadUrl` 已在 `sys_config`，前端 `sysConfig` 可直接取，零改动。
- 配置尚未加载或前缀为空时返回空字符串，避免首屏渲染调用 `undefined.replace`。

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

> `ImageUrlInput.vue` 的预览和 `$common.saveResource`（写 `resource.path`）均必须支持 key；预览同样通过 `imageSrc` 渲染。

### 2.1 存储生命周期

路径契约改变后，所有存储实现都只接受 key/relativePath：

- `LocalUtil.saveFile` 返回 relativePath；`deleteFile` 先将 key 解析为 `uploadUrl + relativePath`，删除成功后按 key 删除 `resource` 记录。
- `QiniuUtil.deleteFile` 将 key 直接提交给七牛，成功后按 key 删除 `resource` 记录。
- `QiniuUtil.saveFileInfo` 用 key 与 `resource.path` 比较，并只写入 key，不能重新写回完整 URL。
- 资源删除、资源列表预览、资源同步必须加入回归测试。

### 3. 渲染层改造

#### 3.1 字段直渲染

把 `:src="x"` 换成 `:src="$common.imageSrc(x)"`：

- 文章封面 `articleCover`：详情 `article.vue` + 列表页
- 用户头像 `avatar`：header / aside / 评论头像等多处
- 照片墙 `resourcePath.cover`：`photo.vue:11`
- 文章视频 `videoUrl`：暂不纳入图片解析；确认其加密/存储格式后另行设计媒体 URL 处理

> 实现时需 grep 全量覆盖所有 `:src` / `v-html` 涉及图片字段处，3.1/3.2 仅列出已定位的关键点。

#### 3.2 markdown / v-html 内容里的图片

不能直接换 `:src`，要在**渲染源头**给图片 src 拼前缀。抽出共享函数 `applyImagePrefix(md)`，在 markdown-it 实例上覆盖 image renderer rule：

| 内容 | 渲染位置 | 处理 |
|---|---|---|
| 文章正文 | `article.vue:521` `new MarkdownIt()` | 注入 image rule，给 `![alt](key)` 的 src 拼前缀 |
| 首页公告 | `index.vue:259` `new MarkdownIt()` | 同上（imageSrc 兼容完整 URL，外部图不受影响） |
| 评论 | `comment.vue:52,78` `v-html` | 评论图为 `[name,url]` 转 `<img>`，在转换处对 url 拼前缀 |
| 树洞/历程 | `treeHole.vue:17`、`process.vue:21` `v-html` | 同评论，在内容中图片 url 出现处拼前缀 |

> `article.vue` 与 `index.vue` 的 markdown-it image 前缀处理复用同一共享函数；配置异步加载时需在配置更新后重新渲染 Markdown。

### 4. 后端：补发 `local.downloadUrl`

`qiniu.downloadUrl` 在 `sys_config` 表，后端 `@Value` 与前端 `sysConfig` 均可获取。`local.downloadUrl` 仅在 `application.yml`，**前端 `sysConfig` 拿不到**。

在 `/sysConfig/listSysConfig` 返回结果中，额外 put 一项 `local.downloadUrl`（值取自后端 `@Value("${local.downloadUrl}")`）。保持本地配置仍统一在 `application.yml`，不割裂到数据库。

主站和 IM 启动时都拉取 `/sysConfig/listSysConfig` 并持久化到各自的 Vuex/localStorage，避免 IM 直接打开且没有主站缓存时无法解析 key。

> 本地存储当前未启用，此补发为「未来切到本地存储时前端能拿到前缀」的完整性保障。

### 5. 历史数据迁移

#### 5.1 涉及的表/字段

| 表 | 字段 | 说明 |
|---|---|---|
| `article` | `article_cover` | 文章封面 |
| `article` | `article_content` | 正文 markdown（图片 URL 内嵌） |
| `article` | `video_url` | 视频字段，暂不纳入图片迁移；需另行确认加密/存储格式 |
| `` `user` `` | `avatar` | 用户头像 |
| `comment` | `comment_content` | 评论 `[name,url]` |
| `resource_path` | `cover` | 照片墙 / 收藏封面；`url` 是跳转链接，不迁移 |
| `resource` | `path` | 所有上传资源记录 |
| `im_chat_user_message` | `content` | IM 用户消息 `[username,url]` |
| `im_chat_user_group_message` | `content` | IM 群消息 |
| `im_chat_group` | `avatar` | IM 群头像 |
| `web_info` | `background_image`, `avatar`, `random_avatar`, `random_cover` | 网站图片；随机字段为 JSON 字符串 |
| `push_notification` | `cover` | 首页推送封面 |
| `family` | `bg_cover`, `man_cover`, `woman_cover` | 家庭页图片 |
| `tree_hole` | `avatar` | 树洞头像 |

排除项：`wei_yan`（树洞/微言）`content` 为纯文本，无图片字段。

#### 5.2 迁移机制

核心：`REPLACE(field, '旧CDN域名/', '')` —— 只对已确认的图片字段执行。`resource_path.url`（跳转链接）和 `article.video_url`（视频/可能加密）不得套用图片迁移。迁移前必须执行目标 key 冲突预检，并在维护窗口内以事务执行，避免写入竞态和部分提交。

#### 5.3 SQL 脚本

可执行脚本见 [`docs/superpowers/migrations/2026-08-01-image-url-decouple.sql`](E:/外包项目/个人博客站点/LiuLiuPiBLOG/docs/superpowers/migrations/2026-08-01-image-url-decouple.sql)。脚本包含：

1. 先备份数据库，并 dry-run 统计完整字段清单。
2. 预检 `resource.path` 去域名后的唯一键冲突；有冲突时先人工合并，禁止直接执行 UPDATE。
3. 停止写入后 `START TRANSACTION` 执行图片字段迁移；`resource_path.url` 和 `article.video_url` 明确排除。
4. 提交后执行残留检查；历史上存在多个 CDN 域名时，逐个设置 `@old` 重复预检和迁移。

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
- 配置未加载、未知存储类型、协议相对 URL、`data:`/`blob:` URL 不抛异常且结果正确。
- `applyImagePrefix`、`pictureReg`：key、完整 URL、外部协议和配置异步更新后的渲染结果。
- `LocalUtil`/`QiniuUtil`：key 删除、资源记录删除、七牛资源扫描不会重新写入完整 URL。

#### 8.2 迁移验证

- dry-run `SELECT` 各表命中行数；
- `resource.path` 去域名后的目标 key 冲突数必须为 0；
- 执行后 `SELECT COUNT(*) ... LIKE '%旧域名%'` 残留归零；
- 抽样确认 `article_content` 中 `![alt](key)`、评论 `[name,key]` 格式正确，并验证 JSON 随机图片字段仍可解析。

#### 8.3 端到端（核心验收）

- 改 `sys_config.qiniu.downloadUrl` 为新域名 → **不碰数据库、不重新上传**，文章封面/正文/头像/评论图全部切到新域名显示。
- 新上传一张图 → 库里存的是 key（不含域名）→ 前端渲染拼新域名显示。
- 资源管理页删除本地/七牛资源后，对象和 `resource` 记录均被删除。

#### 8.4 回归

- 外部图（http 完整 URL）不受影响。
- emoji / `assets/` 静态资源（走 `webStaticResourcePrefix`）不受影响。

## 风险与回退

| 风险 | 应对 |
|---|---|
| 迁移误伤非图片数据 | `REPLACE` 仅针对确定的旧域名字符串；执行前 dry-run 预览命中范围；全程备份。 |
| 迁移后仍有个别图挂 | 先按残留 SELECT 排查是否漏迁/多域名；imageSrc 兼容完整 URL 提供软兼容。 |
| 上线过渡期数据混存（新 key + 旧完整 URL） | 先部署可读两种格式的渲染代码，再停写迁移；`imageSrc` 对完整 URL 原样返回。 |
| 回退 | `source backup_20260801.sql` 恢复数据库；代码改动按 commit 回滚。 |

## 依赖

- 已有依赖：`markdown-it`（article/index 渲染）、`sys_config` 配置机制、`localStorage.defaultStoreType`
- 后端改动：`LocalUtil`/`QiniuUtil` 生命周期、`/sysConfig/listSysConfig` 补发 `local.downloadUrl`
- 数据库：一次性迁移 SQL（无表结构变更）

## 实现约束

- `article.video_url` 暂不迁移，确认加密/存储格式后另行设计媒体 URL 处理。
- 迁移脚本中的表名和字段已按实体与建表 SQL 核对；执行前仍需将 `@old` 替换为实际历史域名。
- 所有 `:src` / `v-html` 图片字段必须通过 `imageSrc` 或渲染源头处理；静态资源继续使用 `webStaticResourcePrefix`。
- 发布顺序：部署兼容读路径 → 停止上传/编辑写入 → 备份并执行迁移 → 验证残留与冲突 → 开放写入。

## 验收标准

- [ ] 七牛上传成功后，库里存储的是 key（不含域名）；本地上传返回 path（不含域名）。
- [ ] `$common.imageSrc` 工具函数实现并通过单元测试。
- [ ] 所有图片渲染点（封面/正文/头像/评论/照片墙等）通过 `imageSrc` 或渲染源头拼前缀正确显示。
- [ ] `/sysConfig/listSysConfig` 返回包含 `local.downloadUrl`。
- [ ] 历史数据迁移脚本执行后，库中不含旧 CDN 域名（残留 SELECT 归零）。
- [ ] **核心**：仅修改 `qiniu.downloadUrl` 配置，全站图片切换到新域名，无需改库、无需重新上传。
- [ ] 外部图与静态资源不受影响。
