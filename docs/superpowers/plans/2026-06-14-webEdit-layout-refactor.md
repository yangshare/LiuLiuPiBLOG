# webEdit 后台管理页面布局优化 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 `liuliupi-ui/src/components/admin/webEdit.vue` 从垂直堆叠长表单重构为 3 个 `el-tabs` 标签页，图片字段改为紧凑的 URL 输入 + 缩略图 + 上传按钮组合，所有保存按钮统一为「保存」并右下角对齐。

**架构：** 新增可复用 `ImageUrlInput` 组件封装图片 URL 输入、预览和上传弹窗；`webEdit.vue` 外层使用 `el-tabs` 分为「基础信息」「公告」「随机资源」三个标签页；每个标签页内部保持现有数据模型和 API 调用逻辑不变。

**技术栈：** Vue 2 + Element UI + @vue/cli-plugin-unit-jest + @vue/test-utils

---

## 关键工程决策（执行前必读）

### 决策 1：先搭建最小单元测试能力

项目当前没有前端测试框架。本计划首先安装 `@vue/cli-plugin-unit-jest` 和 `@vue/test-utils`，并新增 `jest.config.js` 和 `test:unit` script。该能力仅服务于本次新增的 `ImageUrlInput` 组件，不强制覆盖旧代码，避免范围蔓延。

### 决策 2：`ImageUrlInput` 采用 v-model 设计

组件对外暴露 `value` prop 和 `input` 事件，遵循 Vue 2 的 `v-model` 约定。上传成功后通过 `$emit('input', url)` 回填 URL，使父组件可以像普通输入框一样使用：

```vue
<ImageUrlInput v-model="webInfo.backgroundImage" label="背景" prefix="webBackgroundImage" :maxSize="3" :maxNumber="1" />
```

### 决策 3：上传按钮使用弹窗承载 `uploadPicture`

`uploadPicture` 组件本身包含较大的拖拽上传区域，直接嵌入表单行会拉高行高。`ImageUrlInput` 的「上传」按钮点击后弹出 `el-dialog`，在弹窗内放置 `uploadPicture`，上传成功后关闭弹窗并回填 URL。这样满足紧凑布局要求，同时复用现有上传逻辑。

### 决策 4：随机资源页共用一个保存按钮

随机资源标签页内包含随机名称、随机头像、随机封面三个子模块，但它们同属于「随机资源」这一业务概念。本计划将这三个子模块的保存逻辑合并为一个「保存」按钮，调用 `updateWebInfo({ id, randomName, randomAvatar, randomCover })`。

---

## 范围检查

本计划聚焦单一前端页面（`webEdit.vue`）的布局重构，不改动后端 API、不引入新的 UI 库、不改变状态管理逻辑。新增的测试能力服务于本次组件，不影响其他页面。

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `liuliupi-ui/package.json` | 修改 | 添加 `test:unit` script 和 Jest 相关依赖 |
| `liuliupi-ui/jest.config.js` | 创建 | Jest 配置，使用 Vue CLI 预设 |
| `liuliupi-ui/src/components/admin/common/ImageUrlInput.vue` | 创建 | 可复用图片 URL 输入组件 |
| `liuliupi-ui/tests/unit/ImageUrlInput.spec.js` | 创建 | `ImageUrlInput` 单元测试 |
| `liuliupi-ui/src/components/admin/webEdit.vue` | 修改 | 重构为 3 个标签页布局 |

---

## 任务 1：搭建前端单元测试能力

**文件：**
- 修改：`liuliupi-ui/package.json`
- 创建：`liuliupi-ui/jest.config.js`

> 所有命令在 `liuliupi-ui` 目录下执行。

- [ ] **步骤 1：安装测试依赖**

运行：

```bash
cd liuliupi-ui
npm install -D @vue/cli-plugin-unit-jest@~4.5.0 @vue/test-utils@^1.3.0
```

预期：安装成功，`package.json` 中出现 `@vue/cli-plugin-unit-jest` 和 `@vue/test-utils`。

- [ ] **步骤 2：添加 `test:unit` script**

在 `liuliupi-ui/package.json` 的 `scripts` 块中新增一行：

```json
"test:unit": "vue-cli-service test:unit"
```

修改后 `scripts` 应类似：

```json
"scripts": {
  "serve": "set NODE_OPTIONS=--openssl-legacy-provider & vue-cli-service serve",
  "build": "set NODE_OPTIONS=--openssl-legacy-provider & vue-cli-service build",
  "lint": "vue-cli-service lint",
  "test:unit": "vue-cli-service test:unit"
}
```

- [ ] **步骤 3：创建 Jest 配置**

创建 `liuliupi-ui/jest.config.js`，内容：

```js
module.exports = {
  preset: '@vue/cli-plugin-unit-jest'
}
```

- [ ] **步骤 4：验证测试命令**

运行：

```bash
npm run test:unit
```

预期：命令可执行，因暂无测试文件，可能提示 "No tests found" 或 "Test Suites: 1 skipped, 0 of 1 total"。只要命令不报错即可。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/package.json liuliupi-ui/jest.config.js
git commit -m "chore(test): 搭建 liuliupi-ui 单元测试能力"
```

---

## 任务 2：实现 ImageUrlInput 组件（TDD）

**文件：**
- 创建：`liuliupi-ui/src/components/admin/common/ImageUrlInput.vue`
- 创建：`liuliupi-ui/tests/unit/ImageUrlInput.spec.js`

- [ ] **步骤 1：编写失败的测试**

创建 `liuliupi-ui/tests/unit/ImageUrlInput.spec.js`：

```js
import { shallowMount } from '@vue/test-utils'
import ImageUrlInput from '@/components/admin/common/ImageUrlInput.vue'

describe('ImageUrlInput.vue', () => {
  it('renders input, thumb and upload button', () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: 'https://example.com/bg.jpg' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    expect(wrapper.find('.image-url-input').exists()).toBe(true)
    expect(wrapper.find('el-input-stub').exists()).toBe(true)
    expect(wrapper.find('el-image-stub').exists()).toBe(true)
    expect(wrapper.find('el-button-stub').exists()).toBe(true)
  })

  it('emits input event when url changes', async () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: '' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    wrapper.find('el-input-stub').vm.$emit('input', 'https://example.com/new.jpg')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted().input).toBeTruthy()
    expect(wrapper.emitted().input[0]).toEqual(['https://example.com/new.jpg'])
  })

  it('emits input event when upload succeeds', async () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: '' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    wrapper.find('upload-picture-stub').vm.$emit('addPicture', 'https://example.com/uploaded.jpg')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted().input).toBeTruthy()
    expect(wrapper.emitted().input[0]).toEqual(['https://example.com/uploaded.jpg'])
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/ImageUrlInput.spec.js
```

预期：FAIL，报错组件不存在或找不到模块。

- [ ] **步骤 3：实现 ImageUrlInput 组件**

创建 `liuliupi-ui/src/components/admin/common/ImageUrlInput.vue`：

```vue
<template>
  <div class="image-url-input">
    <el-input
      :value="value"
      @input="$emit('input', $event)"
      :placeholder="placeholder"
      class="image-url-input-field"
    />
    <el-image
      v-if="value"
      :src="value"
      :preview-src-list="[value]"
      fit="cover"
      class="image-url-input-thumb"
    />
    <el-button
      type="primary"
      plain
      size="small"
      @click="dialogVisible = true"
      class="image-url-input-btn"
    >
      上传
    </el-button>

    <el-dialog
      :title="label + '上传'"
      :visible.sync="dialogVisible"
      width="420px"
      append-to-body
    >
      <upload-picture
        v-bind="$attrs"
        @addPicture="handleUploadSuccess"
      />
    </el-dialog>
  </div>
</template>

<script>
const uploadPicture = () => import('@/components/common/uploadPicture')

export default {
  name: 'ImageUrlInput',
  components: { uploadPicture },
  props: {
    value: {
      type: String,
      default: ''
    },
    label: {
      type: String,
      default: '图片'
    },
    placeholder: {
      type: String,
      default: 'https://'
    }
  },
  data() {
    return {
      dialogVisible: false
    }
  },
  methods: {
    handleUploadSuccess(url) {
      this.$emit('input', url)
      this.dialogVisible = false
    }
  }
}
</script>

<style scoped>
.image-url-input {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.image-url-input-field {
  flex: 1;
  max-width: 420px;
}

.image-url-input-thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.image-url-input-btn {
  flex-shrink: 0;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/ImageUrlInput.spec.js
```

预期：3 个测试全部 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/common/ImageUrlInput.vue liuliupi-ui/tests/unit/ImageUrlInput.spec.js
git commit -m "feat(ui): 添加 ImageUrlInput 图片 URL 输入组件"
```

---

## 任务 3：重构 webEdit.vue 为标签页布局

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`

> 建议先在文件顶部导入 `ImageUrlInput`：
> ```js
> import ImageUrlInput from './common/ImageUrlInput'
> ```
> 并在 `components` 中注册。

- [ ] **步骤 1：用 `el-tabs` 包裹现有分组**

将 `webEdit.vue` 模板中外层 `div` 内的 5 个平级 `div` 替换为 `el-tabs` 结构。保留原 `div` 作为 `el-tab-pane` 的内容：

```vue
<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基础信息" name="basic">
        <!-- 基础信息表单 -->
      </el-tab-pane>

      <el-tab-pane label="公告" name="notice">
        <!-- 公告管理 -->
      </el-tab-pane>

      <el-tab-pane label="随机资源" name="random">
        <!-- 随机资源 -->
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
```

在 `data()` 中新增：

```js
activeTab: 'basic'
```

- [ ] **步骤 2：重构「基础信息」标签页**

在「基础信息」`el-tab-pane` 内：
1. 删除原来的 `el-tag effect="dark"` 分组标题（标签页标签已替代标题）。
2. 保留 `el-form`，但将「背景」和「头像」字段替换为 `ImageUrlInput`：

```vue
<el-form-item label="背景" prop="backgroundImage">
  <ImageUrlInput
    v-model="webInfo.backgroundImage"
    label="背景"
    prefix="webBackgroundImage"
    :maxSize="3"
    :maxNumber="1"
  />
</el-form-item>

<el-form-item label="头像" prop="avatar">
  <ImageUrlInput
    v-model="webInfo.avatar"
    label="头像"
    prefix="webAvatar"
    :maxSize="2"
    :maxNumber="1"
  />
</el-form-item>
```

3. 删除原来背景/头像字段中内联的 `uploadPicture` 和 `el-image` 代码。
4. 删除原「保存基本信息」按钮，改为在表单底部右侧放置一个「保存」按钮：

```vue
<div class="form-actions">
  <el-button type="primary" @click="submitForm('ruleForm')">保存</el-button>
</div>
```

5. 在 `style` 中添加：

```css
.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
```

- [ ] **步骤 3：重构「公告」标签页**

在「公告」`el-tab-pane` 内：
1. 删除原 `el-tag effect="dark"` 标题。
2. 保留 `notices` 的 tag 列表、输入框和添加按钮逻辑。
3. 将保存按钮改为右下角「保存」：

```vue
<div class="form-actions">
  <el-button type="primary" @click="saveNotice()">保存</el-button>
</div>
```

- [ ] **步骤 4：重构「随机资源」标签页**

在「随机资源」`el-tab-pane` 内，将原来的 3 个平级 `div` 用 `el-card` 或带边框容器分为 3 个子模块。每个子模块保留原有逻辑：

```vue
<el-card class="random-resource-card">
  <div slot="header">随机名称</div>
  <!-- 随机名称 tag 列表和输入框 -->
</el-card>

<el-card class="random-resource-card">
  <div slot="header">随机头像</div>
  <!-- 随机头像列表和上传 -->
</el-card>

<el-card class="random-resource-card">
  <div slot="header">随机封面</div>
  <!-- 随机封面列表和上传 -->
</el-card>

<div class="form-actions">
  <el-button type="primary" @click="saveRandomResources()">保存</el-button>
</div>
```

新增方法 `saveRandomResources()`：

```js
saveRandomResources() {
  this.updateWebInfo({
    id: this.webInfo.id,
    randomName: JSON.stringify(this.randomName),
    randomAvatar: JSON.stringify(this.randomAvatar),
    randomCover: JSON.stringify(this.randomCover)
  })
}
```

删除原 `saveRandomName()`、`saveRandomAvatar()`、`saveRandomCover()` 三个方法及其对应的独立保存按钮。

在 `style` 中添加：

```css
.random-resource-card {
  margin-bottom: 20px;
}
.random-resource-card:last-child {
  margin-bottom: 0;
}
```

- [ ] **步骤 5：清理废弃代码**

1. 删除 `addBackgroundImage` 和 `addAvatar` 方法（`ImageUrlInput` 通过 `v-model` 直接更新）。
2. 删除原 `my-tag`、`button-new-tag`、`input-new-tag` 等与旧分组标题相关的样式（如果不再使用）。
3. 保留 `my-icon` 和 `table-td-thumb` 样式，但 `table-td-thumb` 可能不再需要，若确认无其他使用可删除。

- [ ] **步骤 6：运行 lint 检查**

运行：

```bash
npm run lint
```

预期：无错误，无警告。如有警告，按提示修复。

- [ ] **步骤 7：运行单元测试**

运行：

```bash
npm run test:unit
```

预期：`ImageUrlInput` 测试通过。

- [ ] **步骤 8：启动开发服务器手动验证**

运行：

```bash
npm run serve
```

打开浏览器访问后台管理「网站设置」页面，验证：
1. 页面展示 3 个标签页：基础信息、公告、随机资源。
2. 「基础信息」中背景、头像字段为 URL 输入 + 缩略图 + 上传按钮，点击上传按钮弹出上传弹窗。
3. 上传成功后 URL 回填，缩略图更新。
4. 各标签页保存按钮在右下角，文案为「保存」。
5. 「随机资源」标签页只有一个「保存」按钮，点击后同时保存随机名称、头像、封面。
6. 各保存操作成功后刷新数据并提示成功。
7. 页面无报错，控制台无 Vue 警告。

- [ ] **步骤 9：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue
git commit -m "refactor(ui): 重构 webEdit 为标签页布局，统一图片输入组件"
```

---

## 自检

### 1. 规格覆盖度

| 规格需求 | 实现任务 |
|---|---|
| 3 个标签页：基础信息 / 公告 / 随机资源 | 任务 3 步骤 1 |
| 图片字段改为 URL 输入 + 缩略图 + 上传按钮 | 任务 2 + 任务 3 步骤 2 |
| 所有保存按钮文案为「保存」 | 任务 3 步骤 2/3/4 |
| 随机资源页只有一个保存按钮 | 任务 3 步骤 4 |
| 抽出 `ImageUrlInput` 组件 | 任务 2 |
| 保持现有 API 逻辑 | 任务 3 步骤 2/3/4 |

无遗漏。

### 2. 占位符扫描

本计划无 "TODO"、"待定"、"后续实现"、"补充细节"、"添加适当错误处理" 等占位符。每个步骤均包含具体代码或命令。

### 3. 类型一致性

- `ImageUrlInput` 的 prop 为 `value`，事件为 `input`，与父组件 `v-model` 用法一致。
- `saveRandomResources()` 提交字段名与原 `saveRandomName/Avatar/Cover()` 一致，均为 `randomName`、`randomAvatar`、`randomCover`。
- `uploadPicture` 的 prop `prefix`、`maxSize`、`maxNumber` 通过 `v-bind="$attrs"` 透传，保持与原调用一致。

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-06-14-webEdit-layout-refactor.md`。两种执行方式：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代。需要调用 `superpowers:subagent-driven-development` 技能。

**2. 内联执行** - 在当前会话中使用 `superpowers:executing-plans` 执行任务，批量执行并设有检查点供审查。

**选哪种方式？**
