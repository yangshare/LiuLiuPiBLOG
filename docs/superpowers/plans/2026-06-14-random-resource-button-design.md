# 随机资源「添加图片链接」按钮交互优化 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 修改 `liuliupi-ui/src/components/admin/webEdit.vue` 的「随机资源」标签页，将「随机头像」和「随机封面」的内联 URL 输入改为弹窗输入，统一按钮文案为「添加图片链接」，并优化本地上传入口的视觉区分。

**架构：** 在 `webEdit.vue` 内维护 `addUrlType`、`addUrlDialogVisible`、`addUrlValue`、`addUrlError` 四个数据字段控制弹窗状态；新增 `showAddUrlDialog`、`confirmAddUrl`、`validateImageUrl` 方法处理打开、校验与提交；模板中新增 `el-dialog` 弹窗承载 `el-input` 输入，上传区上方增加说明标题与虚线分隔。

**技术栈：** Vue 2 + Element UI + @vue/cli-plugin-unit-jest + @vue/test-utils

---

## 关键工程决策（执行前必读）

### 决策 1：不改动 `ImageUrlInput` 组件

本次需求只涉及「随机资源」标签页中多图片 URL 的批量添加，与基础信息标签页中 `ImageUrlInput` 的单图片 URL 输入 + 上传弹窗场景不同。`ImageUrlInput` 用于单条图片 URL 的输入和上传回填，不满足批量添加与弹窗内校验的需求，因此本次直接在 `webEdit.vue` 内实现弹窗，不复用 `ImageUrlInput`。

### 决策 2：校验仅作为前端友好提示

规格说明校验「仅作为友好提示，不阻塞后端提交」。因此校验失败时只在弹窗内显示 `el-form-item` 错误信息，不关闭弹窗；校验通过后才将 URL 推入数组并关闭弹窗。

### 决策 3：取消/关闭弹窗保留输入

用户可能误点取消或关闭弹窗，保留输入可以降低重复输入成本。确认添加成功后再清空 `addUrlValue` 和 `addUrlError`。

### 决策 4：保持旧的内联输入框数据字段直到最后一步再删除

`inputRandomAvatarVisible`、`inputRandomAvatarValue`、`inputRandomCoverVisible`、`inputRandomCoverValue` 等旧字段在任务 1-8 中仍然存在于 `data()` 内，避免一次性改动过大。任务 9 专门清理这些废弃字段和对应方法，并在清理后立即运行完整测试确认无回归。

---

## 范围检查

本计划只修改 `liuliupi-ui/src/components/admin/webEdit.vue` 及其单元测试 `liuliupi-ui/tests/unit/webEdit.spec.js`。不改动 `ImageUrlInput`、不改动 `uploadPicture`、不改动后端 API、不改动数据模型。

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `liuliupi-ui/src/components/admin/webEdit.vue` | 修改 | 实现弹窗输入、按钮文案、上传区标题、空状态提示 |
| `liuliupi-ui/tests/unit/webEdit.spec.js` | 修改 | 新增弹窗、按钮、校验、空状态等单元测试 |

---

## 任务 1：搭建弹窗数据模型与 `showAddUrlDialog` 方法

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

> 所有命令在 `liuliupi-ui` 目录下执行。

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('opens add url dialog with correct type', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.showAddUrlDialog('avatar')
    expect(wrapper.vm.addUrlType).toBe('avatar')
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)

    wrapper.vm.showAddUrlDialog('cover')
    expect(wrapper.vm.addUrlType).toBe('cover')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
cd liuliupi-ui
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，报错 `showAddUrlDialog is not a function` 或 `addUrlType` / `addUrlDialogVisible` 未定义。

- [ ] **步骤 3：实现弹窗数据模型与打开方法**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的 `data()` 中新增字段：

```js
        addUrlType: '', // 'avatar' | 'cover'
        addUrlDialogVisible: false,
        addUrlValue: '',
        addUrlError: '',
```

在 `methods` 中新增方法：

```js
      showAddUrlDialog(type) {
        this.addUrlType = type
        this.addUrlDialogVisible = true
      },
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 添加随机资源 URL 弹窗数据模型与 showAddUrlDialog 方法"
```

---

## 任务 2：替换按钮文案为「添加图片链接」

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('renders add image link buttons in random resource cards', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const buttons = wrapper.findAll('el-button-stub')
    const addLinkButtons = buttons.filter(b => b.text() === '添加图片链接')
    expect(addLinkButtons).toHaveLength(2)
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，断言期望 2 个「添加图片链接」按钮，实际为 0。

- [ ] **步骤 3：替换按钮模板**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的「随机头像」卡片中，将：

```vue
          <el-input
            class="input-new-tag"
            v-if="inputRandomAvatarVisible"
            v-model="inputRandomAvatarValue"
            ref="saveRandomAvatarInput"
            size="small"
            @keyup.enter.native="handleInputRandomAvatarConfirm"
            @blur="handleInputRandomAvatarConfirm">
          </el-input>
          <el-button v-else class="button-new-tag" size="small" @click="showRandomAvatarInput">+ 随机头像</el-button>
```

替换为：

```vue
          <el-button
            class="button-new-tag"
            size="small"
            type="primary"
            plain
            icon="el-icon-link"
            @click="showAddUrlDialog('avatar')"
          >
            添加图片链接
          </el-button>
```

在「随机封面」卡片中，将：

```vue
          <el-input
            class="input-new-tag"
            v-if="inputRandomCoverVisible"
            v-model="inputRandomCoverValue"
            ref="saveRandomCoverInput"
            size="small"
            @keyup.enter.native="handleInputRandomCoverConfirm"
            @blur="handleInputRandomCoverConfirm">
          </el-input>
          <el-button v-else class="button-new-tag" size="small" @click="showRandomCoverInput">+ 随机封面</el-button>
```

替换为：

```vue
          <el-button
            class="button-new-tag"
            size="small"
            type="primary"
            plain
            icon="el-icon-link"
            @click="showAddUrlDialog('cover')"
          >
            添加图片链接
          </el-button>
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 随机资源按钮文案改为「添加图片链接」"
```

---

## 任务 3：点击按钮触发弹窗打开

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('opens add url dialog when add image link button clicked', async () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const buttons = wrapper.findAll('el-button-stub')
    const addLinkButton = buttons.filter(b => b.text() === '添加图片链接').at(0)
    await addLinkButton.trigger('click')
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)
    expect(wrapper.vm.addUrlType).toBe('avatar')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，点击后 `addUrlDialogVisible` 仍为 `false`（若弹窗数据模型已实现但按钮未绑定点击事件则为此表现）。

- [ ] **步骤 3：确认按钮已绑定 `@click` 事件**

任务 2 中已将按钮 `@click="showAddUrlDialog('avatar')"` 和 `@click="showAddUrlDialog('cover')"` 写入模板。本步骤无需新增代码，只需确认按钮绑定正确。

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 点击「添加图片链接」按钮打开弹窗"
```

---

## 任务 4：实现弹窗内输入并确认添加合法 URL

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('adds valid avatar url to randomAvatar on confirm', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'avatar',
          addUrlValue: 'https://example.com/avatar.jpg',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomAvatar).toEqual(['https://example.com/avatar.jpg'])
    expect(wrapper.vm.addUrlDialogVisible).toBe(false)
    expect(wrapper.vm.addUrlValue).toBe('')
    expect(wrapper.vm.addUrlError).toBe('')
  })

  it('adds valid cover url to randomCover on confirm', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'cover',
          addUrlValue: 'https://example.com/cover.png',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomCover).toEqual(['https://example.com/cover.png'])
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，报错 `confirmAddUrl is not a function`。

- [ ] **步骤 3：实现弹窗模板与确认添加方法**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的「随机资源」标签页末尾、`</el-tab-pane>` 之前新增弹窗：

```vue
        <el-dialog
          title="添加图片链接"
          :visible.sync="addUrlDialogVisible"
          width="420px"
          append-to-body
        >
          <el-form>
            <el-form-item :error="addUrlError">
              <el-input
                v-model="addUrlValue"
                placeholder="https://example.com/avatar.png"
                size="small"
              />
            </el-form-item>
          </el-form>
          <div class="dialog-tip">支持 jpg、png、gif、webp、svg 等常见图片格式。</div>
          <span slot="footer">
            <el-button size="small" @click="addUrlDialogVisible = false">取消</el-button>
            <el-button size="small" type="primary" @click="confirmAddUrl">确认添加</el-button>
          </span>
        </el-dialog>
```

在 `methods` 中新增 `validateImageUrl` 和 `confirmAddUrl`：

```js
      validateImageUrl(url) {
        if (!url) {
          return '请输入有效的图片链接'
        }
        if (!/^https?:\/\//i.test(url)) {
          return '请输入有效的图片链接'
        }
        if (!/\.(jpg|jpeg|png|gif|webp|svg)$/i.test(url)) {
          return '请输入有效的图片链接'
        }
        return ''
      },
      confirmAddUrl() {
        const error = this.validateImageUrl(this.addUrlValue)
        if (error) {
          this.addUrlError = error
          return
        }
        if (this.addUrlType === 'avatar') {
          this.randomAvatar.push(this.addUrlValue)
        } else if (this.addUrlType === 'cover') {
          this.randomCover.push(this.addUrlValue)
        }
        this.addUrlDialogVisible = false
        this.addUrlValue = ''
        this.addUrlError = ''
      },
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 实现添加图片链接弹窗与确认添加逻辑"
```

---

## 任务 5：实现弹窗内 URL 校验

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it.each([
    [''],
    ['not-a-url'],
    ['ftp://example.com/avatar.jpg'],
    ['https://example.com/avatar.txt']
  ])('does not add invalid url "%s" and keeps dialog open', async (url) => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'avatar',
          addUrlValue: url,
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomAvatar).toEqual([])
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)
    expect(wrapper.vm.addUrlError).toBe('请输入有效的图片链接')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，非法 URL 被错误地加入数组，或 `addUrlError` 为空。

- [ ] **步骤 3：确认校验逻辑**

任务 4 中已实现的 `validateImageUrl` 和 `confirmAddUrl` 已包含校验逻辑。本步骤无需新增代码，只需确认实现正确。

校验规则：
- 非空。
- 以 `http://` 或 `https://` 开头（不区分大小写）。
- 后缀为 `jpg|jpeg|png|gif|webp|svg` 之一（不区分大小写）。

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 添加图片链接弹窗 URL 校验"
```

---

## 任务 6：取消/关闭弹窗保留输入

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('keeps input value when dialog is cancelled', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          addUrlValue: 'https://example.com/keep.jpg',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.addUrlDialogVisible = false
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.addUrlValue).toBe('https://example.com/keep.jpg')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，关闭弹窗后 `addUrlValue` 被清空（若已有自动清空逻辑）。

- [ ] **步骤 3：确保取消/关闭不清空输入**

任务 4 中的模板已将取消按钮绑定到 `@click="addUrlDialogVisible = false"`，`confirmAddUrl` 仅在成功确认时清空 `addUrlValue` 和 `addUrlError`。确认没有在其他地方（如 `watch` 或 `close` 事件）清空输入。

本步骤无需新增代码，只需确认实现符合要求。

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 取消/关闭图片链接弹窗保留输入"
```

---

## 任务 7：上传区标题与分隔线

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('renders upload section divider with title', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const dividers = wrapper.findAll('.upload-divider')
    expect(dividers).toHaveLength(2)
    expect(dividers.at(0).text()).toContain('或上传本地图片')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，找不到 `.upload-divider` 元素。

- [ ] **步骤 3：添加上传区标题与分隔线样式**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的「随机头像」卡片中，将：

```vue
          <uploadPicture prefix="randomAvatar" style="margin: 10px" @addPicture="addRandomAvatar"
                         :maxSize="1"
                         :maxNumber="5"></uploadPicture>
```

替换为：

```vue
          <div class="upload-divider">或上传本地图片（一次最多 5 张，每张不超过 1M）</div>
          <uploadPicture prefix="randomAvatar" style="margin: 10px" @addPicture="addRandomAvatar"
                         :maxSize="1"
                         :maxNumber="5"></uploadPicture>
```

在「随机封面」卡片中，将：

```vue
          <uploadPicture prefix="randomCover" style="margin: 10px" @addPicture="addRandomCover"
                         :maxSize="2"
                         :maxNumber="5"></uploadPicture>
```

替换为：

```vue
          <div class="upload-divider">或上传本地图片（一次最多 5 张，每张不超过 1M）</div>
          <uploadPicture prefix="randomCover" style="margin: 10px" @addPicture="addRandomCover"
                         :maxSize="2"
                         :maxNumber="5"></uploadPicture>
```

在 `<style scoped>` 中新增：

```css
  .upload-divider {
    border-top: 1px dashed #dcdfe6;
    padding-top: 16px;
    margin-top: 16px;
    margin-bottom: 10px;
    font-size: 12px;
    color: #909399;
  }

  .dialog-tip {
    font-size: 12px;
    color: #909399;
    margin-top: -8px;
    line-height: 1.5;
  }
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 随机资源上传区添加标题与分隔线"
```

---

## 任务 8：空状态提示

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败的测试**

在 `liuliupi-ui/tests/unit/webEdit.spec.js` 中新增测试：

```js
  it('shows empty tip when random image list is empty', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: []
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const tips = wrapper.findAll('.empty-tip')
    expect(tips).toHaveLength(2)
    expect(tips.at(0).text()).toBe('暂无图片，点击下方按钮添加。')
  })
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，找不到 `.empty-tip` 元素。

- [ ] **步骤 3：添加空状态提示**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的「随机头像」卡片中，将图片网格区域：

```vue
          <div class="random-image-grid">
            <div :key="i"
                 class="random-image-item"
                 v-for="(avatar, i) in randomAvatar">
              ...
            </div>
          </div>
```

替换为：

```vue
          <div class="random-image-grid">
            <div :key="i"
                 class="random-image-item"
                 v-for="(avatar, i) in randomAvatar">
              ...
            </div>
            <div v-if="randomAvatar.length === 0" class="empty-tip">暂无图片，点击下方按钮添加。</div>
          </div>
```

在「随机封面」卡片中做同样修改，将 `randomAvatar` 替换为 `randomCover`。

在 `<style scoped>` 中新增：

```css
  .empty-tip {
    grid-column: 1 / -1;
    font-size: 14px;
    color: #909399;
    text-align: center;
    padding: 20px 0;
  }
```

- [ ] **步骤 4：运行测试验证通过**

运行：

```bash
npm run test:unit tests/unit/webEdit.spec.js
```

预期：所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 随机资源图片网格添加空状态提示"
```

---

## 任务 9：清理旧的内联输入框逻辑

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`

- [ ] **步骤 1：确认新测试已覆盖旧逻辑替代场景**

确认任务 1-8 中的测试已经覆盖：
- 点击按钮打开弹窗。
- 输入合法 URL 并确认后添加到对应数组。
- 输入非法 URL 不添加。

无需新增测试。

- [ ] **步骤 2：删除旧数据字段**

在 `liuliupi-ui/src/components/admin/webEdit.vue` 的 `data()` 中删除以下字段：

```js
        inputRandomAvatarVisible: false,
        inputRandomAvatarValue: "",
        inputRandomCoverVisible: false,
        inputRandomCoverValue: "",
```

- [ ] **步骤 3：删除旧方法**

在 `methods` 中删除以下方法：

```js
      handleInputRandomAvatarConfirm() {
        if (this.inputRandomAvatarValue) {
          this.randomAvatar.push(this.inputRandomAvatarValue);
        }
        this.inputRandomAvatarVisible = false;
        this.inputRandomAvatarValue = '';
      },
      showRandomAvatarInput() {
        this.inputRandomAvatarVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomAvatarInput.$refs.input.focus();
        });
      },
      handleInputRandomCoverConfirm() {
        if (this.inputRandomCoverValue) {
          this.randomCover.push(this.inputRandomCoverValue);
        }
        this.inputRandomCoverVisible = false;
        this.inputRandomCoverValue = '';
      },
      showRandomCoverInput() {
        this.inputRandomCoverVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomCoverInput.$refs.input.focus();
        });
      },
```

- [ ] **步骤 4：运行完整测试确认无回归**

运行：

```bash
npm run test:unit
```

预期：`ImageUrlInput` 和 `webEdit` 的所有测试 PASS。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue
git commit -m "refactor(ui): 移除随机资源旧内联 URL 输入逻辑"
```

---

## 任务 10：运行 lint 与完整测试套件

**文件：**
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue`（如有 lint 问题）
- 测试：`liuliupi-ui/tests/unit/webEdit.spec.js`（如有 lint 问题）

- [ ] **步骤 1：运行 lint**

运行：

```bash
npm run lint
```

预期：无错误，无警告。如有警告，按提示修复。

- [ ] **步骤 2：运行完整单元测试**

运行：

```bash
npm run test:unit
```

预期：所有测试 PASS。

- [ ] **步骤 3：Commit（仅当步骤 1/2 有修复时）**

如果 lint 或测试修复没有改动代码则跳过此步骤。如果有改动：

```bash
git add liuliupi-ui/src/components/admin/webEdit.vue liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "style(ui): 修复 webEdit 随机资源弹窗 lint 问题"
```

---

## 自检

### 1. 规格覆盖度

| 规格需求 | 实现任务 |
|---|---|
| 按钮文案为「添加图片链接」，带链接图标 | 任务 2 |
| 点击按钮弹出标题为「添加图片链接」的对话框 | 任务 3、任务 4 |
| 输入合法 URL 并确认后出现在对应图片网格中 | 任务 4 |
| 输入空/非法 URL 时弹窗内显示错误提示，不关闭弹窗 | 任务 5 |
| 上传区上方显示说明标题 | 任务 7 |
| 保存随机资源时新 URL 正常提交到后端 | 任务 4（数据进入数组），现有 `saveRandomResources` 不变 |
| 原有本地上传功能无回归 | 任务 9 清理后测试覆盖 |
| 随机名称模块不受本次改动影响 | 任务 9 仅删除头像/封面相关旧逻辑 |

无遗漏。

### 2. 占位符扫描

本计划无 "TODO"、"待定"、"后续实现"、"补充细节"、"添加适当错误处理" 等占位符。每个步骤均包含具体代码或命令。

### 3. 类型一致性

- `addUrlType` 只取 `'avatar'` 或 `'cover'`，与 `showAddUrlDialog(type)` 参数一致。
- `confirmAddUrl` 仅对 `addUrlType === 'avatar'` 或 `'cover'` 时操作对应数组，其他情况不操作。
- `validateImageUrl` 返回空字符串表示校验通过，非空字符串表示错误信息，与 `el-form-item` 的 `:error` prop 类型一致。
- `saveRandomResources()` 提交字段名 `randomAvatar`、`randomCover` 保持不变。

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-06-14-random-resource-button-design.md`。两种执行方式：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代。需要调用 `superpowers:subagent-driven-development` 技能。

**2. 内联执行** - 在当前会话中使用 `superpowers:executing-plans` 执行任务，批量执行并设有检查点供审查。

**选哪种方式？**
