# 图片 URL 域名解耦 实现验证与补全计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 验证工作树中已有的「key 存库 + 渲染时按存储类型拼接下载域名」实现，补齐设计文档要求的单元测试、对齐 IM 端偏差、修复渲染层与迁移脚本的遗漏，最终做到「只改 `qiniu.downloadUrl` 配置即全站切图」。

**架构：** 用户上传图片时只把 key/relativePath 写库（七牛存 key、本地存 relativePath），渲染时由 `$common.imageSrc(path)` 按当前 `defaultStoreType` 从 `sysConfig['qiniu.downloadUrl' | 'local.downloadUrl']` 取前缀拼接；markdown 正文用 `applyImagePrefix` 在 image renderer 注入前缀；评论/树洞的 `[name,url]` 用 `pictureReg` 转图。后端 `LocalUtil`/`QiniuUtil` 只处理 key，删除时按 key 删对象与 `resource` 记录。历史带旧 CDN 域名的完整 URL 由一次性 SQL 剥离为 key。

**技术栈：** Spring Boot 2.7.18 / Java 1.8 + MyBatis-Plus + JUnit 5 + Mockito + AssertJ（`mvn test`）；主站前端 Vue 2 + Element UI + markdown-it + Jest + `@vue/test-utils`（`pnpm test:unit`）；IM 前端 Vue 3 + naive-ui（**无测试框架，靠手动验证**）。

---

## 现状基线（务必先读）

工作树中已有大量**未提交**的实现，与设计文档高度吻合（约 85% 完成）。本计划**不重写这些已完成的实现**，而是在其之上补齐测试、对齐偏差、修复遗漏。已完成的实现见下表（执行任务时这些是「已存在的基线」，不要重复实现）：

| 设计要点 | 现状 | 证据 |
|---|---|---|
| `imageSrc` / `applyImagePrefix` / `pictureReg`（主站） | ✅ 已实现 | `liuliupi-ui/src/utils/common.js:65-112` |
| `imageSrc` / `applyImagePrefix` / `pictureReg`（IM） | ✅ 已实现（兜底链偏差见任务 5） | `liuliupi-im-ui/src/utils/common.js:69-113` |
| 七牛上传 4 处只存 key | ✅ 已实现 | `liuliupi-ui/.../uploadPicture.vue:151`、`admin/postEdit.vue:230`、`comment/graffiti.vue:312`、`liuliupi-im-ui/.../uploadPicture.vue:85`（均为 `url = response.key`） |
| `ImageUrlInput` 预览 + `saveResource` 支持 key | ✅ 已实现 | `liuliupi-ui/.../admin/common/ImageUrlInput.vue:11-12` |
| `:src` 字段直渲染全量 | ✅ 已实现（遗漏 1 处见任务 6） | `article.vue`、`comment.vue`、`photo.vue`、`groupInfo.vue` 等 |
| markdown / v-html 图片前缀 | ✅ 已实现 | `article.vue:354`、`index.vue:265`、`comment.vue:204-219`、`article.vue:482`（集中预处理 process.vue/treeHole） |
| `LocalUtil` 生命周期（saveFile 返 path、deleteFile 删对象+resource） | ✅ 已实现 | `liuliupi-server/.../storage/LocalUtil.java:36-107` |
| `QiniuUtil` 生命周期（deleteFile/saveFileInfo 只处理 key） | ✅ 已实现 | `liuliupi-server/.../storage/QiniuUtil.java:62-189` |
| `/sysConfig/listSysConfig` 补发 `local.downloadUrl` | ✅ 已实现 | `SysConfigController.java:34-47` |
| IM 启动拉取 sysConfig + defaultStoreType | ✅ 已实现 | `liuliupi-im-ui/src/main.js:62-76`、`store/index.js:5-19` |
| 迁移 SQL 字段覆盖 | ✅ 20 字段全覆盖 | `docs/superpowers/migrations/2026-08-01-image-url-decouple.sql`（缺陷见任务 7） |

**本计划要补的缺口（按设计文档判定）：**

1. 单元测试严重不足：`applyImagePrefix`/`pictureReg` 0 用例；`imageSrc` 缺未知存储类型、fallback 优先级、大小写；后端 `LocalUtil`/`QiniuUtil`/`SysConfigController` 0 测试；现有 3 个被改 spec 只补了 `$common.imageSrc` mock、无新断言。
2. IM `imageSrc` 缺 `store.state.webInfo.defaultStoreType` 兜底，IM store 无 `webInfo` state（偏离设计 §1.2）。
3. `love.vue:207` randomFamily 背景未走 `imageSrc`（CSS `url()` 形式，迁移后失效）。
4. 迁移 SQL 残留检查只查 4 个字段（漏 13 列）、无多域名流程、无显式排除预检、无 JSON 校验。

---

## 文件结构

### 主站前端（liuliupi-ui）

| 文件 | 职责 |
|---|---|
| `tests/unit/common.spec.js` | 扩充 `imageSrc` 全分支 + 新增 `applyImagePrefix` / `pictureReg` 单元测试（任务 1）。 |
| `src/components/love.vue` | 修复 randomFamily 背景走 `imageSrc`（任务 6）。 |

### IM 前端（liuliupi-im-ui，无测试框架，手动验证）

| 文件 | 职责 |
|---|---|
| `src/store/index.js` | 新增 `webInfo` state 与 `loadWebInfo` mutation（任务 5）。 |
| `src/main.js` | `/webInfo/getWebInfo` 改为 `commit("loadWebInfo")` 持久化整个 webInfo（任务 5）。 |
| `src/utils/common.js` | `imageSrc` 兜底链补 `store.state.webInfo.defaultStoreType`，对齐设计 §1.2（任务 5）。 |

### 后端（liuliupi-server）

| 文件 | 职责 |
|---|---|
| `src/test/java/com/liuliupi/utils/storage/LocalUtilTest.java` | 验证 `saveFile` 返回 key、`deleteFile` 剥前缀后删对象并删 `resource` 记录（任务 2）。 |
| `src/main/java/com/liuliupi/utils/storage/QiniuUtil.java` | 抽取纯函数 `buildNewResources`，便于单测「只写 key」（任务 3）。 |
| `src/test/java/com/liuliupi/utils/storage/QiniuUtilTest.java` | 验证 `buildNewResources` 生成的 `resource.path` 是 key、不含域名（任务 3）。 |
| `src/test/java/com/liuliupi/controller/SysConfigControllerTest.java` | 验证 `listSysConfig` 返回含 `local.downloadUrl`（任务 4）。 |

### 数据迁移

| 文件 | 职责 |
|---|---|
| `docs/superpowers/migrations/2026-08-01-image-url-decouple.sql` | 补全残留检查（全部字段）、多域名流程、显式排除预检、JSON 校验（任务 7）。 |

---

## 前置：提交工作树基线实现

工作树中 45 个文件的改动尚未提交。先把已完成的实现按子系统提交为基线，使后续每个补全任务能独立、清晰地 commit。这些改动经三个盘点代理确认全部属于本特性。

- [ ] **步骤 1：提交设计文档与迁移脚本基线**

```bash
cd "E:\外包项目\个人博客站点\LiuLiuPiBLOG"
git add docs/superpowers/specs/2026-08-01-image-url-decouple-design.md docs/superpowers/migrations/2026-08-01-image-url-decouple.sql
git commit -m "docs(image): 图片URL域名解耦设计文档与历史数据迁移脚本"
```

- [ ] **步骤 2：提交后端基线**

```bash
cd "E:\外包项目\个人博客站点\LiuLiuPiBLOG"
git add liuliupi-server/src/main/java/com/liuliupi/controller/SysConfigController.java liuliupi-server/src/main/java/com/liuliupi/utils/storage/LocalUtil.java liuliupi-server/src/main/java/com/liuliupi/utils/storage/QiniuUtil.java
git commit -m "feat(server): 存储层只处理key并补发local.downloadUrl到sysConfig"
```

- [ ] **步骤 3：提交主站前端基线**

```bash
cd "E:\外包项目\个人博客站点\LiuLiuPiBLOG"
git add liuliupi-ui/src liuliupi-ui/tests/unit/ImageUrlInput.spec.js liuliupi-ui/tests/unit/index.spec.js liuliupi-ui/tests/unit/webEdit.spec.js
git commit -m "feat(ui): 图片URL解耦—上传存key+渲染拼接下载域名（主站）"
```

- [ ] **步骤 4：提交 IM 前端基线**

```bash
cd "E:\外包项目\个人博客站点\LiuLiuPiBLOG"
git add liuliupi-im-ui/src
git commit -m "feat(im-ui): 图片URL解耦—上传存key+渲染拼接下载域名（IM端）"
```

> 若 `git add liuliupi-ui/src` 之外还有未跟踪的 `tests/unit/common.spec.js`，合并到任务 1 一起提交（本前置不提交它，留给任务 1 扩充后提交）。

---

## 任务 1：主站工具函数单元测试（imageSrc 全分支 + applyImagePrefix + pictureReg）

**文件：**
- 修改：`liuliupi-ui/tests/unit/common.spec.js`

> 实现已存在于 `src/utils/common.js`，本任务补齐测试覆盖。`applyImagePrefix` / `pictureReg` 当前 0 用例，是首要缺口。

- [ ] **步骤 1：扩充 `imageSrc` 缺失分支，并新增 `applyImagePrefix` / `pictureReg` 测试**

将 `tests/unit/common.spec.js` 替换为以下完整内容（保留原有 3 个用例，补充 fallback 优先级、未知存储类型、大小写，并新增两个 describe）：

```javascript
import common from '@/utils/common'
import store from '@/store'
import MarkdownIt from 'markdown-it'

describe('$common.imageSrc', () => {
  beforeEach(() => {
    localStorage.clear()
    store.state.sysConfig = {
      'qiniu.downloadUrl': 'https://cdn.example.com/',
      'local.downloadUrl': 'https://local.example.com/files/'
    }
    store.state.webInfo = { defaultStoreType: 'qiniu' }
  })

  it('keeps absolute and non-http resource URLs unchanged', () => {
    expect(common.imageSrc('https://external.example/a.png')).toBe('https://external.example/a.png')
    expect(common.imageSrc('//external.example/a.png')).toBe('//external.example/a.png')
    expect(common.imageSrc('data:image/png;base64,abc')).toBe('data:image/png;base64,abc')
    expect(common.imageSrc('blob:https://example.com/id')).toBe('blob:https://example.com/id')
  })

  // 协议前缀大小写不敏感（实现正则带 /i）
  it('treats uppercase HTTPS: as absolute URL', () => {
    expect(common.imageSrc('HTTPS://external.example/a.png')).toBe('HTTPS://external.example/a.png')
  })

  it('joins qiniu and local keys without duplicate slashes', () => {
    localStorage.setItem('defaultStoreType', 'qiniu')
    expect(common.imageSrc('/article/a.png')).toBe('https://cdn.example.com/article/a.png')
    localStorage.setItem('defaultStoreType', 'local')
    expect(common.imageSrc('comment/a.png')).toBe('https://local.example.com/files/comment/a.png')
  })

  // fallback 优先级：localStorage 为空时回退到 store.state.webInfo.defaultStoreType
  it('falls back to store webInfo.defaultStoreType when localStorage is empty', () => {
    store.state.webInfo = { defaultStoreType: 'local' }
    expect(common.imageSrc('article/a.png')).toBe('https://local.example.com/files/article/a.png')
  })

  // 未知存储类型（非 local）一律走 qiniu 前缀
  it('uses qiniu prefix for unknown store type', () => {
    localStorage.setItem('defaultStoreType', 'oss')
    expect(common.imageSrc('article/a.png')).toBe('https://cdn.example.com/article/a.png')
  })

  it('returns an empty source while configuration is unavailable', () => {
    store.state.sysConfig = {}
    expect(common.imageSrc('article/a.png')).toBe('')
    expect(common.imageSrc('')).toBe('')
    expect(common.imageSrc(null)).toBe('')
    expect(common.imageSrc(undefined)).toBe('')
    expect(common.imageSrc('   ')).toBe('')
  })
})

describe('$common.applyImagePrefix', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('defaultStoreType', 'qiniu')
    store.state.sysConfig = { 'qiniu.downloadUrl': 'https://cdn.example.com/' }
  })

  it('prepends download url to markdown image key', () => {
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![封面](article/a.png)')
    expect(html).toContain('src="https://cdn.example.com/article/a.png"')
  })

  it('leaves absolute URLs untouched', () => {
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![外链](https://external.example/a.png)')
    expect(html).toContain('src="https://external.example/a.png"')
  })

  // 配置未加载（前缀为空）时 src 被替换为空字符串，不抛异常
  it('does not throw and empties src when prefix missing', () => {
    store.state.sysConfig = {}
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![封面](article/a.png)')
    expect(html).not.toContain('undefined')
    expect(html).not.toContain('cdn.example.com')
  })
})

describe('$common.pictureReg', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('defaultStoreType', 'qiniu')
    store.state.sysConfig = { 'qiniu.downloadUrl': 'https://cdn.example.com/' }
  })

  it('converts [name,key] into an img whose src is prefixed', () => {
    const html = common.pictureReg('看图[风景,article/a.png]结尾')
    expect(html).toContain('src="https://cdn.example.com/article/a.png"')
    expect(html).toContain('title="风景"')
  })

  it('leaves absolute URL in [name,url] untouched', () => {
    const html = common.pictureReg('[外链,https://external.example/a.png]')
    expect(html).toContain('src="https://external.example/a.png"')
  })

  it('returns content unchanged when bracket has no comma', () => {
    expect(common.pictureReg('[无逗号文本]')).toBe('[无逗号文本]')
  })
})
```

- [ ] **步骤 2：运行测试**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/common.spec.js
```

预期：PASS。`imageSrc` / `applyImagePrefix` / `pictureReg` 所有用例通过（实现已存在于 `src/utils/common.js`）。若有用例 FAIL，说明实现与设计不符，按设计文档修正 `src/utils/common.js` 后再跑。

- [ ] **步骤 3：Commit**

```bash
cd liuliupi-ui
git add tests/unit/common.spec.js
git commit -m "test(ui): 补齐imageSrc/applyImagePrefix/pictureReg单元测试"
```

---

## 任务 2：后端 LocalUtil 单元测试

**文件：**
- 创建：`liuliupi-server/src/test/java/com/liuliupi/utils/storage/LocalUtilTest.java`

> 验证设计 §2.1：`saveFile` 返回的 `visitPath` 是 relativePath（不含 downloadUrl 前缀）；`deleteFile` 先剥前缀再删文件，并按 key 删除 `resource` 记录。`LocalUtil` 字段为 `@Value`/`@Autowired` 私有注入，用 `ReflectionTestUtils` 赋值；文件系统用 JUnit 5 `@TempDir`。

- [ ] **步骤 1：编写测试**

创建 `src/test/java/com/liuliupi/utils/storage/LocalUtilTest.java`：

```java
package com.liuliupi.utils.storage;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.liuliupi.entity.Resource;
import com.liuliupi.service.ResourceService;
import com.liuliupi.vo.FileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalUtilTest {

    @Mock
    private ResourceService resourceService;

    @TempDir
    Path tempDir;

    private LocalUtil newLocalUtil(String downloadUrl) {
        LocalUtil localUtil = new LocalUtil();
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";
        ReflectionTestUtils.setField(localUtil, "uploadUrl", uploadUrl);
        ReflectionTestUtils.setField(localUtil, "downloadUrl", downloadUrl);
        return localUtil;
    }

    @Test
    void saveFileReturnsVisitPathWithoutDownloadPrefix() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");

        FileVO fileVO = new FileVO();
        fileVO.setRelativePath("article/test.jpg");
        fileVO.setFile(new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}));

        FileVO result = localUtil.saveFile(fileVO);

        // visitPath 必须是 key（不含域名），写库据此渲染
        assertThat(result.getVisitPath()).isEqualTo("article/test.jpg");
        assertThat(result.getAbsolutePath()).endsWith("article/test.jpg");
        assertThat(result.getVisitPath()).doesNotStartWith("http");
    }

    @Test
    void deleteFileStripsDownloadPrefixThenDeletesFileAndResourceRecord() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";

        // 模拟对象存储中已存在的文件
        new File(uploadUrl + "comment").mkdirs();
        try {
            new File(uploadUrl + "comment/pic.jpg").createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LambdaUpdateChainWrapper<Resource> chain = newLambdaChainMock();

        // 传入带旧域名的完整 URL，验证 stripDownloadPrefix 生效后仍能定位文件
        localUtil.deleteFile(Collections.singletonList("https://cdn.example.com/comment/pic.jpg"));

        assertThat(new File(uploadUrl + "comment/pic.jpg")).doesNotExist();
        // 按 key（剥前缀后）删除 resource 记录
        verify(resourceService).lambdaUpdate();
        verify(chain).eq(Resource::getPath, "comment/pic.jpg");
        verify(chain).remove();
    }

    @Test
    void deleteFileAcceptsBareKeyWithoutPrefix() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";

        new File(uploadUrl + "article").mkdirs();
        try {
            new File(uploadUrl + "article/bare.jpg").createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LambdaUpdateChainWrapper<Resource> chain = newLambdaChainMock();

        localUtil.deleteFile(Collections.singletonList("article/bare.jpg"));

        assertThat(new File(uploadUrl + "article/bare.jpg")).doesNotExist();
        verify(chain).eq(Resource::getPath, "article/bare.jpg");
        verify(chain).remove();
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateChainWrapper<Resource> newLambdaChainMock() {
        LambdaUpdateChainWrapper<Resource> chain = org.mockito.Mockito.mock(LambdaUpdateChainWrapper.class);
        when(resourceService.lambdaUpdate()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        return chain;
    }
}
```

- [ ] **步骤 2：运行测试**

```bash
cd liuliupi-server
mvn test -Dtest=LocalUtilTest -q
```

预期：PASS。三个用例全部通过（实现已存在于 `LocalUtil.java`）。

- [ ] **步骤 3：Commit**

```bash
cd liuliupi-server
git add src/test/java/com/liuliupi/utils/storage/LocalUtilTest.java
git commit -m "test(server): LocalUtil saveFile返key/deleteFile删对象与resource记录"
```

---

## 任务 3：QiniuUtil 抽取 buildNewResources + 单元测试

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/utils/storage/QiniuUtil.java`
- 创建：`liuliupi-server/src/test/java/com/liuliupi/utils/storage/QiniuUtilTest.java`

> 设计 §8.1 要求「七牛资源扫描不会重新写入完整 URL」。`saveFileInfo` 内联了构造 `Resource` 的逻辑，且依赖七牛 SDK 实例化（`Auth.create` / `new BucketManager` / `createFileListIterator`），无法直接单测。把「FileInfo[] → List<Resource>」的纯逻辑抽成静态方法 `buildNewResources`，再单测它。`deleteFile` 的网络交互（真正调七牛删对象）强依赖外部服务，不做单测，由任务 8 端到端验收覆盖。

- [ ] **步骤 1：编写失败测试**

创建 `src/test/java/com/liuliupi/utils/storage/QiniuUtilTest.java`：

```java
package com.liuliupi.utils.storage;

import com.liuliupi.entity.Resource;
import com.qiniu.storage.model.FileInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QiniuUtilTest {

    @Test
    void buildNewResourcesStoresKeyWithoutDownloadPrefix() {
        FileInfo item = new FileInfo();
        item.key = "article/abc.jpg";
        item.fsize = 1024L;
        item.mimeType = "image/jpeg";

        List<Resource> result = QiniuUtil.buildNewResources(new FileInfo[]{item}, Collections.emptyList());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPath()).isEqualTo("article/abc.jpg");
        assertThat(result.get(0).getPath()).doesNotStartWith("http");
    }

    @Test
    void buildNewResourcesSkipsExistingAndZeroSize() {
        FileInfo existing = new FileInfo();
        existing.key = "article/old.jpg";
        existing.fsize = 10L;

        FileInfo zero = new FileInfo();
        zero.key = "article/zero.jpg";
        zero.fsize = 0L;

        List<Resource> result = QiniuUtil.buildNewResources(
                new FileInfo[]{existing, zero},
                Arrays.asList("article/old.jpg"));

        assertThat(result).isEmpty();
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-server
mvn test -Dtest=QiniuUtilTest -q
```

预期：FAIL，`QiniuUtil.buildNewResources` 方法不存在（编译错误）。

- [ ] **步骤 3：抽取 `buildNewResources` 纯函数**

在 `QiniuUtil.java` 中，把 `saveFileInfo` 的 `while` 循环体改为调用新方法。将以下片段：

```java
        List<Resource> resources = new ArrayList<>();

        while (fileListIterator.hasNext()) {
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                if (item.fsize != 0L && !paths.contains(item.key)) {
                    Resource re = new Resource();
                    re.setPath(item.key);
                    re.setType(CommonConst.PATH_TYPE_ASSETS);
                    re.setSize(Integer.valueOf(Long.toString(item.fsize)));
                    re.setMimeType(item.mimeType);
                    re.setStoreType(StoreEnum.QINIU.getCode());
                    re.setUserId(CommonConst.ADMIN_USER_ID);
                    resources.add(re);
                }
            }
        }
```

替换为：

```java
        List<Resource> resources = new ArrayList<>();

        while (fileListIterator.hasNext()) {
            FileInfo[] items = fileListIterator.next();
            resources.addAll(buildNewResources(items, paths));
        }
```

并在 `saveFileInfo` 方法下方（类的末尾、`}` 之前）新增静态方法：

```java
    /**
     * 把七牛列举到的 FileInfo[] 转换为待入库的 Resource 列表。
     * path 只写 key（不含 downloadUrl），避免重新写入完整 URL。
     *
     * @param items         单次迭代返回的七牛文件信息
     * @param existingPaths 数据库中已存在的 resource.path 集合，用于去重
     */
    static List<Resource> buildNewResources(FileInfo[] items, java.util.Collection<String> existingPaths) {
        List<Resource> resources = new ArrayList<>();
        for (FileInfo item : items) {
            if (item.fsize != 0L && !existingPaths.contains(item.key)) {
                Resource re = new Resource();
                re.setPath(item.key);
                re.setType(CommonConst.PATH_TYPE_ASSETS);
                re.setSize(Integer.valueOf(Long.toString(item.fsize)));
                re.setMimeType(item.mimeType);
                re.setStoreType(StoreEnum.QINIU.getCode());
                re.setUserId(CommonConst.ADMIN_USER_ID);
                resources.add(re);
            }
        }
        return resources;
    }
```

- [ ] **步骤 4：运行测试验证通过**

```bash
cd liuliupi-server
mvn test -Dtest=QiniuUtilTest -q
```

预期：PASS。

- [ ] **步骤 5：Commit**

```bash
cd liuliupi-server
git add src/main/java/com/liuliupi/utils/storage/QiniuUtil.java src/test/java/com/liuliupi/utils/storage/QiniuUtilTest.java
git commit -m "refactor(server): 抽取QiniuUtil.buildNewResources并补单测—验证只写key"
```

---

## 任务 4：SysConfigController 单元测试

**文件：**
- 创建：`liuliupi-server/src/test/java/com/liuliupi/controller/SysConfigControllerTest.java`

> 验证设计 §4：`/sysConfig/listSysConfig` 返回的 map 必须包含 `local.downloadUrl`（值取自 `@Value`）。

- [ ] **步骤 1：编写测试**

创建 `src/test/java/com/liuliupi/controller/SysConfigControllerTest.java`：

```java
package com.liuliupi.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.SysConfig;
import com.liuliupi.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigControllerTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private BaseMapper<SysConfig> baseMapper;

    @InjectMocks
    private SysConfigController controller;

    @Test
    void listSysConfigIncludesLocalDownloadUrlEvenWhenDbIsEmpty() {
        ReflectionTestUtils.setField(controller, "localDownloadUrl", "https://local.test/files");
        when(sysConfigService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.selectList(any())).thenReturn(Collections.emptyList());

        PoetryResult<Map<String, String>> result = controller.listSysConfig();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("local.downloadUrl", "https://local.test/files");
    }

    @Test
    void listSysConfigMergesPublicConfigsFromDbWithLocalDownloadUrl() {
        ReflectionTestUtils.setField(controller, "localDownloadUrl", "https://local.test/files");

        SysConfig qiniu = new SysConfig();
        qiniu.setConfigKey("qiniu.downloadUrl");
        qiniu.setConfigValue("https://cdn.example.com/");

        when(sysConfigService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.selectList(any())).thenReturn(Collections.singletonList(qiniu));

        PoetryResult<Map<String, String>> result = controller.listSysConfig();

        assertThat(result.getData())
                .containsEntry("qiniu.downloadUrl", "https://cdn.example.com/")
                .containsEntry("local.downloadUrl", "https://local.test/files");
    }
}
```

- [ ] **步骤 2：运行测试**

```bash
cd liuliupi-server
mvn test -Dtest=SysConfigControllerTest -q
```

预期：PASS。

- [ ] **步骤 3：运行全部后端测试确认无回归**

```bash
cd liuliupi-server
mvn test -q
```

预期：BUILD SUCCESS，所有测试通过。

- [ ] **步骤 4：Commit**

```bash
cd liuliupi-server
git add src/test/java/com/liuliupi/controller/SysConfigControllerTest.java
git commit -m "test(server): SysConfigController补发local.downloadUrl的单元测试"
```

---

## 任务 5：对齐 IM 端 imageSrc 兜底链

**文件：**
- 修改：`liuliupi-im-ui/src/store/index.js`
- 修改：`liuliupi-im-ui/src/main.js`
- 修改：`liuliupi-im-ui/src/utils/common.js`

> 设计 §1.2 要求 `storeType = localStorage.getItem('defaultStoreType') || store.state.webInfo.defaultStoreType || 'qiniu'`。IM 端 `imageSrc` 缺中间兜底，且 store 无 `webInfo` state。按「设计文档为准」对齐：补 `webInfo` state + `loadWebInfo` mutation + main.js 持久化整个 webInfo + `imageSrc` 补兜底。**IM 端无测试框架，本任务靠手动验证（步骤 5）。**

- [ ] **步骤 1：store 增加 webInfo state 与 loadWebInfo mutation**

修改 `liuliupi-im-ui/src/store/index.js`，将：

```js
  state: {
    currentUser: JSON.parse(localStorage.getItem("currentUser") || '{}'),
    sysConfig: JSON.parse(localStorage.getItem("sysConfig") || '{}')
  },
```

改为：

```js
  state: {
    currentUser: JSON.parse(localStorage.getItem("currentUser") || '{}'),
    sysConfig: JSON.parse(localStorage.getItem("sysConfig") || '{}'),
    webInfo: JSON.parse(localStorage.getItem("webInfo") || '{}')
  },
```

并在 `loadSysConfig` mutation 之后新增 `loadWebInfo`：

```js
    loadSysConfig(state, sysConfig) {
      state.sysConfig = sysConfig || {};
      localStorage.setItem("sysConfig", JSON.stringify(state.sysConfig));
    },
    loadWebInfo(state, webInfo) {
      state.webInfo = webInfo || {};
      localStorage.setItem("webInfo", JSON.stringify(state.webInfo));
    }
```

- [ ] **步骤 2：main.js 持久化整个 webInfo**

修改 `liuliupi-im-ui/src/main.js`，将：

```js
http.get(constant.baseURL + "/webInfo/getWebInfo")
  .then((res) => {
    if (res && res.data && res.data.defaultStoreType) {
      localStorage.setItem("defaultStoreType", res.data.defaultStoreType)
    }
  })
  .catch(() => {})
```

改为：

```js
http.get(constant.baseURL + "/webInfo/getWebInfo")
  .then((res) => {
    if (res && res.data) {
      store.commit("loadWebInfo", res.data)
      if (res.data.defaultStoreType) {
        localStorage.setItem("defaultStoreType", res.data.defaultStoreType)
      }
    }
  })
  .catch(() => {})
```

- [ ] **步骤 3：imageSrc 补 webInfo.defaultStoreType 兜底**

修改 `liuliupi-im-ui/src/utils/common.js` 第 79 行，将：

```js
    const storeType = localStorage.getItem("defaultStoreType") || "qiniu";
```

改为：

```js
    const storeType = localStorage.getItem("defaultStoreType") || store.state.webInfo.defaultStoreType || "qiniu";
```

- [ ] **步骤 4：构建 IM 前端确认无编译错误**

```bash
cd liuliupi-im-ui
pnpm lint
pnpm build
```

预期：lint 无新增错误，build 成功。

- [ ] **步骤 5：手动验证（IM 无单测）**

启动 IM 前端（`pnpm serve`），打开一个含用户上传图片的会话：
- 七牛存储下：群头像、用户头像、消息图片正常显示（URL 为 `qiniu.downloadUrl + key`）。
- 在浏览器控制台执行 `localStorage.removeItem('defaultStoreType')` 后刷新，图片仍正常（走 `store.state.webInfo.defaultStoreType` 兜底）。
- 验证 `localStorage.webInfo` 已写入且可 JSON 解析。

- [ ] **步骤 6：Commit**

```bash
cd liuliupi-im-ui
git add src/store/index.js src/main.js src/utils/common.js
git commit -m "fix(im-ui): imageSrc兜底链对齐设计—补webInfo state与loadWebInfo"
```

---

## 任务 6：修复 love.vue randomFamily 背景遗漏

**文件：**
- 修改：`liuliupi-ui/src/components/love.vue:207`

> 前端盘点发现唯一遗漏：`card === 4` 的 randomFamily 卡片背景用 CSS `url(item.bgCover)` 直接拼 key，迁移后失效。

- [ ] **步骤 1：修改背景样式走 imageSrc**

修改 `liuliupi-ui/src/components/love.vue:207`，将：

```vue
                   :style="{ background: 'url(' + item.bgCover + ') center center / cover no-repeat' }">
```

改为：

```vue
                   :style="{ background: 'url(' + $common.imageSrc(item.bgCover) + ') center center / cover no-repeat' }">
```

> 注意：第 215 行 `assets/loveLike.svg` 是 `webStaticResourcePrefix` 静态资源，**不要**改；第 209/218 行 `el-avatar :src` 已正确使用 `$common.imageSrc`，不动。

- [ ] **步骤 2：验证**

```bash
cd liuliupi-ui
pnpm test:unit
```

预期：所有现有测试通过（无回归）。手动打开「开往表白墙」页，randomFamily 卡片背景正常显示。

- [ ] **步骤 3：Commit**

```bash
cd liuliupi-ui
git add src/components/love.vue
git commit -m "fix(ui): randomFamily背景图走imageSrc拼接下载域名"
```

---

## 任务 7：补全迁移 SQL 的安全机制

**文件：**
- 修改：`docs/superpowers/migrations/2026-08-01-image-url-decouple.sql`

> 测试与迁移盘点发现：① 残留检查只查 4 个字段（漏 web_info 4 列、push_notification.cover、resource_path.cover、family 3 列、tree_hole.avatar、im_chat_group.avatar、comment/im 消息 content 等 13 列）；② 无多域名重跑流程；③ `video_url`/`resource_path.url` 无显式排除预检；④ web_info 随机 JSON 字段无可解析性校验。字段层面已 100% 覆盖，本任务只补机制。

- [ ] **步骤 1：用以下完整内容替换整个迁移脚本**

将 `docs/superpowers/migrations/2026-08-01-image-url-decouple.sql` 替换为：

```sql
-- 图片 URL 域名解耦迁移
-- =====================================================================
-- 执行前置（务必逐项完成，不可跳过）：
--   1. 在维护窗口执行；执行前停止文章/评论/头像/资源等写入（上传、编辑、删除）。
--   2. 先完整备份数据库：
--        mysqldump -u<user> -p liuliupi_blog > backup_20260801.sql
--   3. 把下面 @old 设为「实际历史 CDN 域名」。若历史上存在多个域名（含 http://、//、无尾斜杠等变体），
--      必须逐个设置 @old 后【重跑本脚本全部步骤】（从「预检」开始），每个域名独立跑一遍。
--      切勿只跑一次就认为完成。
--   4. 本脚本只迁移确认属于图片的字段；article.video_url 与 resource_path.url 保持不变。
-- =====================================================================

SET @old := 'https://file.yangshare.com/';

-- -------------------------------------------------------------------
-- 阶段 A：dry-run 预检（事务外执行）。执行者必须人工核对下列输出后再进入阶段 C。
-- -------------------------------------------------------------------

-- A-1：各图片字段命中行数（应与迁移后残留检查一一对应）
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

-- A-2：resource.path 去域名后的唯一键冲突预检。
--     输出必须为 0 行；若有冲突，必须先人工合并，禁止执行阶段 C 的 UPDATE。
SELECT REPLACE(path, @old, '') AS target_path, COUNT(*) AS duplicates
FROM resource
GROUP BY REPLACE(path, @old, '')
HAVING COUNT(*) > 1;

-- A-3：显式排除项预检——确认这些「非图片」字段不会被本脚本触碰。
--      预期：video_url / resource_path.url 即便含 @old，下方 UPDATE 也不会改动它们。
SELECT 'article.video_url(excluded,expect-unchanged)' AS field_name, COUNT(*) AS hit
FROM article WHERE video_url LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource_path.url(excluded,expect-unchanged)', COUNT(*)
FROM resource_path WHERE url LIKE CONCAT('%', @old, '%');

-- A-4：web_info 随机字段为 JSON 字符串，确认替换后仍可解析。
--      预期：JSON_VALID = 1（若某行为 0，记录 id 后单独人工处理）。
SELECT id,
       JSON_VALID(random_avatar) AS valid_random_avatar,
       JSON_VALID(random_cover)  AS valid_random_cover
FROM web_info
WHERE random_avatar LIKE CONCAT('%', @old, '%')
   OR random_cover  LIKE CONCAT('%', @old, '%');

-- -------------------------------------------------------------------
-- 阶段 B：确认 A-2 冲突为 0、A-4 全部 JSON_VALID=1 后，方可执行阶段 C。
-- -------------------------------------------------------------------

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

-- -------------------------------------------------------------------
-- 阶段 D：迁移后残留检查。所有 residual 必须为 0（覆盖全部 20 个图片字段）。
--         若任一行 > 0，说明该字段被漏迁或 REPLACE 未命中，按字段排查并补迁。
-- -------------------------------------------------------------------
SELECT 'article.article_cover' AS field_name, COUNT(*) AS residual
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

-- D-2：再次确认排除项确实未被改动。
SELECT 'article.video_url(must-be-unchanged)' AS field_name, COUNT(*) AS residual
FROM article WHERE video_url LIKE CONCAT('%', @old, '%')
UNION ALL SELECT 'resource_path.url(must-be-unchanged)', COUNT(*)
FROM resource_path WHERE url LIKE CONCAT('%', @old, '%');
```

- [ ] **步骤 2：Commit（脚本不在 CI 执行，靠人工核对）**

```bash
cd "E:\外包项目\个人博客站点\LiuLiuPiBLOG"
git add docs/superpowers/migrations/2026-08-01-image-url-decouple.sql
git commit -m "fix(migration): 迁移脚本补全残留检查/多域名流程/排除预检/JSON校验"
```

---

## 任务 8：端到端核心验收 + 历史数据迁移执行

**文件：**
- 涉及：`liuliupi-server` + `liuliupi-ui` + `liuliupi-im-ui` + 数据库（手动操作，不改代码）

> 这是设计文档「核心验收」：仅改配置即全站切图、历史数据剥离为 key。需在含真实数据的部署环境执行。

- [ ] **步骤 1：部署「兼容读路径」的代码**

把任务 1–7 的代码（含基线）部署到测试/生产环境。确认渲染代码能同时识别 key 与完整 URL（`imageSrc` 对 `http(s)://` 原样返回，故混存期安全）。

- [ ] **步骤 2：核心验收 A —— 改配置不改库切图**

在 `sys_config` 表把 `qiniu.downloadUrl` 改为一个测试域名（如 `https://cdn-test.example.com/`），**不碰其他数据、不重新上传**：
- 刷新主站：文章封面、正文配图、头像、评论图、照片墙、家庭页背景、首页推送封面，URL 全部切到新域名。
- 刷新 IM：群头像、用户头像、消息图片全部切到新域名。
- 改回原域名后图片恢复，确认是「渲染时拼接」而非改库。

- [ ] **步骤 3：核心验收 B —— 新上传存 key**

登录后台新上传一张文章封面 / 一条评论图 / 一个 IM 消息图，到数据库查对应字段：
- `article.article_cover`、`resource.path` 等存的是 key（不含域名）。
- 前端渲染拼上 `qiniu.downloadUrl` 正常显示。

- [ ] **步骤 4：核心验收 C —— 资源删除联动**

在资源管理页删除一个七牛资源 / 一个本地资源（启用本地时）：
- 对象存储中文件被删除。
- `resource` 表对应记录被删除（`SELECT * FROM resource WHERE path = '<key>'` 返回空）。

- [ ] **步骤 5：执行历史数据迁移（维护窗口）**

1. `mysqldump -u<user> -p liuliupi_blog > backup_20260801.sql`（备份）。
2. 停止写入（上传/编辑/删除）。
3. 将迁移脚本 `@old` 设为实际历史 CDN 域名。
4. 执行阶段 A 预检：核对命中行数；确认 A-2 冲突为 0；确认 A-4 全部 `JSON_VALID=1`。
5. 执行阶段 B–C 事务迁移。
6. 执行阶段 D 残留检查：所有图片字段 residual = 0；排除项保持不变。
7. 若历史上存在多个 CDN 域名，逐个改 `@old` 重跑阶段 A–D。
8. 抽样确认：`article_content` 中 `![alt](key)`、评论 `[name,key]`、`web_info` 随机 JSON 字段格式正确且可解析。

- [ ] **步骤 6：回退预案（仅在异常时）**

```bash
mysql -u<user> -p liuliupi_blog < backup_20260801.sql
```

代码改动按 commit 回滚（`git revert <commit>`）。

---

## 任务 9：全量构建测试与收尾

**文件：**
- 涉及：`liuliupi-server` + `liuliupi-ui` + `liuliupi-im-ui`

- [ ] **步骤 1：后端全量编译与测试**

```bash
cd liuliupi-server
mvn clean test -q
```

预期：BUILD SUCCESS，所有测试通过（含新增 `LocalUtilTest` / `QiniuUtilTest` / `SysConfigControllerTest`）。

- [ ] **步骤 2：主站前端全量测试与构建**

```bash
cd liuliupi-ui
pnpm test:unit
pnpm build
```

预期：所有单元测试通过；build 成功，无新增 lint 错误。

- [ ] **步骤 3：IM 前端构建**

```bash
cd liuliupi-im-ui
pnpm lint
pnpm build
```

预期：lint 无新增错误，build 成功。

- [ ] **步骤 4：回归确认（设计 §8.4）**

- 外部图（`http(s)://` 完整 URL）渲染不受影响（任务 1 测试覆盖 + 手动抽查）。
- emoji / `assets/` 静态资源（走 `webStaticResourcePrefix`）不受影响。

- [ ] **步骤 5：汇总提交（若有零散改动）**

若步骤 1–3 发现需微调的文件，单独提交；否则本任务无需额外 commit。

---

## 自检

### 1. 规格覆盖度（设计文档章节 → 任务）

| 设计需求 | 对应任务 |
|---|---|
| §1.2 `imageSrc`/`applyImagePrefix`/`pictureReg` 实现 + 单测 | 基线（实现）+ 任务 1（测试） |
| §1.2 IM 端兜底链对齐 `store.state.webInfo.defaultStoreType` | 任务 5 |
| §2 上传链路只存 key（5 处 + ImageUrlInput） | 基线（实现） |
| §2.1 `LocalUtil`/`QiniuUtil` 生命周期回归测试 | 任务 2、任务 3 |
| §3.1 `:src` 字段全量走 imageSrc | 基线 + 任务 6（love.vue 遗漏） |
| §3.2 markdown / v-html 图片前缀 | 基线（实现） |
| §4 `/sysConfig/listSysConfig` 补发 `local.downloadUrl` + IM 拉取 | 基线（实现）+ 任务 4（测试） |
| §5 历史数据迁移（字段 + 安全机制） | 基线（字段）+ 任务 7（机制）+ 任务 8（执行） |
| §7 错误处理（空值/完整URL/配置未加载） | 任务 1（imageSrc 各分支） |
| §8.1 单元测试（imageSrc/applyImagePrefix/pictureReg/LocalUtil/QiniuUtil） | 任务 1、2、3 |
| §8.2 迁移验证（dry-run/冲突/残留/JSON） | 任务 7、任务 8 |
| §8.3 端到端（改配置切图/新上传存key/删除联动） | 任务 8 |
| §8.4 回归（外部图/静态资源不受影响） | 任务 1、任务 9 |
| 验收标准全部 7 项 | 任务 1–8 分别覆盖 |

**已知取舍（已在计划中诚实说明）：**
- IM 端无测试框架，`imageSrc` 对齐改动靠手动验证（任务 5 步骤 5）。
- `QiniuUtil.deleteFile` 的七牛网络交互不做单测（强依赖外部服务），由任务 8 步骤 4 端到端覆盖；仅抽取 `buildNewResources` 测「只写 key」不变量。
- `LocalUtil`/`QiniuUtil` 的 `stripDownloadPrefix` 重复代码未抽取（非阻塞技术债），由 `LocalUtilTest` 的 `deleteFile` 用例间接覆盖。

### 2. 占位符扫描

- 无「待定 / TODO / 后续实现 / 类似任务 N」。
- 无「添加适当的错误处理 / 处理边界情况」等模糊描述。
- 每个代码步骤均含完整可编译/可运行代码块。
- 测试与实现引用的字段名、方法签名均与工作树现状核对一致（`FileVO`、`Resource`、`SysConfig`、`ResourceService.lambdaUpdate()`、`FileInfo` 等）。

### 3. 类型一致性

- `FileVO.setFile(MultipartFile)` / `setRelativePath` / `setVisitPath` / `setAbsolutePath` —— 与 `FileVO.java`（Lombok `@Data`）一致，`MockMultipartFile` 实现 `MultipartFile`。
- `ResourceService.lambdaUpdate()` 返回 `com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper<Resource>` —— 与 MyBatis-Plus `IService` 一致。
- `SysConfig.setConfigKey` / `setConfigValue` / `getConfigKey` / `getConfigValue` —— 与实体一致。
- `QiniuUtil.buildNewResources(FileInfo[], Collection<String>)` 签名在抽取（任务 3 步骤 3）与测试（任务 3 步骤 1）中完全一致。
- `$common.imageSrc` 在主站与 IM 端兜底链对齐后表达式一致（任务 5 步骤 3）。
- 迁移脚本字段名与设计 §5.1 清单逐字核对（article_cover / article_content / avatar / background_image / random_avatar / random_cover / cover / path / bg_cover / man_cover / woman_cover / comment_content / content）。

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-08-01-image-url-decouple.md`。两种执行方式：**

**1. 子代理驱动（推荐）** — 每个任务调度一个新的子代理，任务间进行审查，快速迭代。

**2. 内联执行** — 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点供审查。

**选哪种方式？**
