# 公告 Markdown 编辑与渲染改造设计

## 背景

当前后台「网站设置 → 公告」使用 `el-tag` 一条条维护公告条目，保存为 JSON 数组字符串。这种形式：
- 编辑体验差，新增/删除/排序都不方便；
- 前端无法预测公告以何种样式展示；
- 公告中混杂了「推送标题 / 推送封面 / 推送链接」等特殊条目，被首页解析成弹窗推送，职责不清晰。

因此将公告改为 Markdown 编辑与渲染，并把推送功能独立维护。

## 目标

1. 后台公告使用 Markdown 编辑器维护，前台使用 Markdown 渲染展示。
2. 首页弹窗推送从公告中剥离，独立成表、独立配置。
3. 后台「公告」标签页保持与其他标签页一致的单一保存按钮。
4. 旧数据不再兼容，由管理员在后台重新编辑公告。

## 非目标

- 公告编辑器不支持图片本地上传，仅支持文字、链接和 Markdown 基础语法。
- 不实现推送历史、定时推送等高级功能。
- 不做旧公告数据的自动迁移。

## 方案选择

选择 **方案 2：推送配置独立成表**。虽然改动比「在 WebInfo 内新增字段」大，但能把公告和推送完全解耦，后续扩展更灵活。

## 详细设计

### 1. 数据模型

#### 1.1 `web_info` 表

| 字段 | 变更 |
|---|---|
| `notices` | 由 JSON 数组字符串改为 Markdown 纯文本字符串。 |

#### 1.2 新增 `push_notification` 表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int PK | 主键 |
| title | varchar(200) | 推送标题 |
| cover | varchar(500) | 封面图 URL |
| url | varchar(500) | 点击跳转链接 |
| enabled | tinyint | 是否启用（0/1） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

使用方式：按单条配置使用，始终读取 `enabled = 1` 的第一条记录。若不存在或未启用，首页不弹窗。

### 2. 后端设计

#### 2.1 新增实体

- `PushNotification.java`

#### 2.2 新增 Mapper / Service / ServiceImpl

- `PushNotificationMapper.java`
- `PushNotificationService.java`
- `PushNotificationServiceImpl.java`

#### 2.3 新增 Controller

`PushNotificationController.java`

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/pushNotification/getPushNotification` | GET | 公开 | 前台获取当前启用的推送 |
| `/admin/pushNotification/getPushNotification` | GET | 需登录 | 后台获取当前推送配置 |
| `/admin/pushNotification/savePushNotification` | POST | 需登录 | 保存/更新推送配置 |

保存策略：表中只保留一条记录。若已存在则更新，不存在则插入。

#### 2.4 现有代码调整

- `WebInfoController` 不再对 `notices` 做 `JSON.parse` / `JSON.stringify` 处理。
- `WebInfoHandlerInterceptor` 等拦截器路径无需调整。

### 3. 前端后台：`webEdit.vue`

「公告」标签页改为上下结构：

#### 3.1 公告 Markdown 编辑区

```vue
<mavon-editor v-model="webInfo.notices" />
```

- 不绑定 `@imgAdd`，不支持图片本地上传。
- 初始化时 `webInfo.notices` 直接读取后端返回的字符串。

#### 3.2 推送设置卡片

表单字段：
- 推送标题
- 封面链接
- 跳转链接
- 是否启用（`el-switch`）

#### 3.3 保存交互

标签页底部保留一个统一的「保存」按钮，与其他 tab 保持一致。

点击后串行或并行调用：
1. `POST /webInfo/updateWebInfo` —— 保存 `id` 和 `notices`。
2. `POST /admin/pushNotification/savePushNotification` —— 保存推送配置。

两个请求分别处理结果：全部成功提示「保存成功」；任一失败提示对应错误（如「公告保存失败」或「推送保存失败」），并保留编辑状态，不自动刷新。

### 4. 前端前台：`index.vue`

#### 4.1 公告展示

移除现有的小喇叭 + 多条公告垂直排列，改为宽版公告板：

```vue
<div class="announcement-board background-opacity">
  <div class="announcement-header">
    <i class="fa fa-volume-up"></i>
    <span>公告</span>
  </div>
  <div class="announcement-body" v-html="noticeHtml"></div>
</div>
```

使用 `markdown-it` 渲染：

```js
const md = new MarkdownIt({ breaks: true });
this.noticeHtml = md.render(this.$store.state.webInfo.notices || '');
```

#### 4.2 弹窗推送

移除 `this.$common.pushNotification(...)` 调用，改为：

```js
this.$http.get(this.$constant.baseURL + '/pushNotification/getPushNotification')
  .then((res) => {
    if (!this.$common.isEmpty(res.data)) {
      this.push = res.data;
      // 根据 localStorage 控制是否弹窗
    }
  });
```

#### 4.3 清理

- 删除 `utils/common.js` 中的 `pushNotification` 方法。
- 删除 `index.vue` 中对 `pushNotification` 的引用。

### 5. 数据流

后台保存：

```
webEdit.vue
  ├─ POST /webInfo/updateWebInfo          → web_info.notices = Markdown 文本
  └─ POST /admin/pushNotification/savePushNotification → push_notification 表
```

前台加载：

```
index.vue
  ├─ GET /webInfo/getWebInfo              → store.webInfo.notices → markdown-it 渲染
  └─ GET /pushNotification/getPushNotification  → 弹窗推送
```

### 6. 错误处理

| 场景 | 处理 |
|---|---|
| 保存公告失败 | `$message.error` 提示，不刷新页面 |
| 保存推送失败 | `$message.error` 提示，不刷新页面 |
| 前台获取推送失败 | 静默忽略，不弹窗 |
| Markdown 渲染异常 | 显示原始文本，避免白屏 |
| 旧 JSON 数组数据 | 直接按字符串渲染，管理员需手动重新编辑 |

### 7. 样式约定

- 公告板使用 `.background-opacity` 保持与现有半透明卡片一致。
- Markdown 渲染区复用 `.entry-content` 或新增 `.markdown-content`，统一段落、列表、链接样式。
- 代码块、表格等复杂 Markdown 元素允许渲染，但不做特殊高亮处理（公告场景以文字为主）。

### 8. 测试策略

#### 8.1 后端测试

- `PushNotificationController` 接口单元测试：
  - 保存后查询返回一致；
  - 重复保存只保留一条记录；
  - `enabled = 0` 时前台接口不返回数据。

#### 8.2 前端测试

- `webEdit.vue`：
  - `mavon-editor` 双向绑定 `webInfo.notices`；
  - 保存按钮同时触发两个请求。
- `index.vue`：
  - Markdown 渲染结果符合预期；
  - 推送弹窗从新的独立接口读取。

#### 8.3 端到端测试

- 后台保存 Markdown 公告 → 前台正确渲染；
- 后台保存推送配置 → 首页弹出推荐窗；
- 关闭推送启用开关 → 首页不弹窗。

## 风险与回退

| 风险 | 应对措施 |
|---|---|
| 旧数据直接渲染 JSON 数组会显得混乱 | 属于预期行为，管理员需手动重新编辑。 |
| 推送独立成表增加改动量 | 表结构简单，只保留一条记录，逻辑可控。 |
| 统一保存按钮导致部分失败体验不好 | 分别提示两个接口的保存结果。 |

## 依赖

- 已有依赖：`mavon-editor`、`markdown-it`
- 新增数据库表：`push_notification`
- 新增后端实体与接口：`PushNotification` 相关

## 验收标准

- [ ] 后台「公告」标签页使用 Markdown 编辑器。
- [ ] 后台「公告」标签页包含独立的推送设置表单。
- [ ] 点击保存按钮同时持久化公告和推送配置。
- [ ] 前台首页以宽版公告板渲染 Markdown 公告。
- [ ] 前台首页弹窗推送从独立接口读取，不再解析公告内容。
- [ ] 旧公告 JSON 数组数据被直接当作字符串渲染，管理员可手动修正。
- [ ] 所有相关单元测试通过。
