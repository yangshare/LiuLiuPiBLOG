# 登录图形验证码 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 `POST /user/login` 增加图形验证码校验（每次登录都必须输入），复用 hutool + `PoetryCache`，前台与后台两个登录入口同步生效。

**架构：** 新增公开接口 `GET /user/captcha`：用 `hutool-captcha` 生成线段验证码，文本存入 `PoetryCache`（5 分钟 TTL），返回 `{captchaToken, base64图片}`。改造 `POST /user/login` 增加 `captchaToken`/`code` 参数，校验时**一次性消费**（无论对错，校验后立即删缓存）。前端两个登录表单各加「验证码图片 + 输入框」，进入页面/登录失败时刷新。

**技术栈：** 后端 Spring Boot 2.7.18 + hutool 5.8.22（经 `hutool-bom` 管理）+ MyBatis-Plus + JUnit 5（surefire 2.22.2，AssertJ + Mockito）；前端 Vue 2 + element-ui + axios（封装于 `liuliupi-ui/src/utils/request.js`）。

---

## 关键工程决策（执行前必读）

执行者需理解以下三个决策，它们决定了代码形态：

### 决策 1：验证码校验逻辑提取为 `verifyCaptcha()` 包级静态方法

规格 §1.5 把校验逻辑示意性地内联在 `login()` 里。但 `UserServiceImpl.login()` 的"放行"分支会执行 `lambdaQuery()`（MyBatis-Plus 查库），**无法纯单元测试**（参考 `LoginCheckAspectTest` 只测不碰 DB 的 AOP）。

因此把校验逻辑提取为 `UserServiceImpl` 的**包级静态方法** `verifyCaptcha(captchaToken, code)`，返回 `null`（通过）或错误信息字符串。这样所有分支（失效、错误、大小写、一次性消费、放行）都可被 `src/test/java/com/liuliupi/service/impl/CaptchaTest.java`（同包，可访问包级方法）纯单元测试，且 `login()` 只需一行调用，DRY。**这不改变规格的行为意图，只是把示意伪代码工程化为可测单元。**

### 决策 2：base64 前缀用容错判断式

hutool `AbstractCaptcha` 有两个方法：`getImageBase64()`（纯 base64）与 `getImageBase64Data()`（带 `data:image/png;base64,` 前缀）。在 5.8.22 与 5.8.44 之间存在差异。为保证 `img.src` 始终可用且对版本免疫，统一调用所有版本都存在的 `getImageBase64()`，再用 `startsWith("data:")` 判断是否需要补前缀。**不要**直接用 `getImageBase64Data()`（低版本可能不存在该重载，存在 NoSuchMethodError 风险）。

### 决策 3：前端验证码参数沿用现有 `user` 对象

`liuliupi-ui/src/utils/request.js` 的 `post(url, params, json=true)`：当 `json=false` 时用 `qs.stringify` 转成 `application/x-www-form-urlencoded`。现有登录调用 `this.$http.post(url, user, false, ...)`（`user.vue` 多传一个被忽略的 `false`，`verify.vue` 传 3 个），所以 `@RequestParam` 能从 form 体接收 `account`/`password`。验证码参数只需加入同一个 `user` 对象（`captchaToken`、`code`），**保持 `json=false` 不变**，无需改请求层。

---

## 范围检查

规格是一个聚焦功能（登录防爆破），后端 + 前端是其紧耦合的两半，应在同一计划内完成，不拆分。规格末尾提到与 `2026-06-13-im-sso-auth-code-design.md` 并发实现时 `UserController`/`UserServiceImpl`/`CommonConst` 三个文件需人工合并——本计划已在"并发注意事项"中标注，不展开。

---

## 文件结构

**后端（`liuliupi-server`）：**

| 文件 | 操作 | 职责 |
|---|---|---|
| `pom.xml` | 修改 | 新增 `hutool-captcha` 依赖（版本由 `hutool-bom` 管理） |
| `src/main/java/com/liuliupi/constants/CommonConst.java` | 修改 | 新增 `CAPTCHA_KEY`、`CAPTCHA_EXPIRE` 常量 |
| `src/main/java/com/liuliupi/vo/CaptchaVO.java` | 创建 | 验证码响应 VO（`captchaToken` + `image`） |
| `src/main/java/com/liuliupi/service/UserService.java` | 修改 | 新增 `captcha()` 接口方法；`login()` 签名增加两个参数 |
| `src/main/java/com/liuliupi/service/impl/UserServiceImpl.java` | 修改 | 实现 `captcha()`；新增 `verifyCaptcha()` 包级静态方法；`login()` 接入校验 |
| `src/main/java/com/liuliupi/controller/UserController.java` | 修改 | 新增 `GET /user/captcha`；`login()` 增加 `@RequestParam` 参数 |
| `src/test/java/com/liuliupi/service/impl/CaptchaTest.java` | 创建 | 纯单元测试：`captcha()` 生成 + `verifyCaptcha()` 全分支 |

**前端（`liuliupi-ui`）：**

| 文件 | 操作 | 职责 |
|---|---|---|
| `src/components/user.vue` | 修改 | 前台登录表单加验证码图片+输入框；`login()` 传 `captchaToken`/`code`；进入页/失败刷新 |
| `src/components/admin/verify.vue` | 修改 | 后台登录表单加验证码图片+输入框；同上 |

---

## 任务 1：后端 — 图形验证码生成接口（captcha）

**文件：**
- 修改：`liuliupi-server/pom.xml`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/constants/CommonConst.java`
- 创建：`liuliupi-server/src/main/java/com/liuliupi/vo/CaptchaVO.java`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/UserService.java`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java`
- 创建测试：`liuliupi-server/src/test/java/com/liuliupi/service/impl/CaptchaTest.java`

> 工作目录为 `liuliupi-server`。所有 `mvn` 命令在该目录执行。

- [ ] **步骤 1：新增 hutool-captcha 依赖**

在 `pom.xml` 的 `<dependencies>` 块内，紧跟现有的 `hutool-extra` 依赖（约第 153-156 行）之后，插入：

```xml
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-captcha</artifactId>
        </dependency>
```

（版本由 `<dependencyManagement>` 中的 `hutool-bom`（`${hutool.version}` = 5.8.22）统一管理，**不要**加 `<version>`。）

- [ ] **步骤 2：验证依赖可解析**

运行：`mvn -q dependency:resolve -Dincludes=cn.hutool:hutool-captcha`
预期：无 `BUILD FAILURE`；输出包含下载/已存在 `hutool-captcha-5.8.22.jar`，最终 `BUILD SUCCESS`。

- [ ] **步骤 3：新增常量**

在 `CommonConst.java` 中，紧接已有的 `CODE_EXPIRE`（约第 54 行）之后插入：

```java
    /**
     * 图形验证码缓存 key 前缀
     */
    public static final String CAPTCHA_KEY = "captcha_";

    /**
     * 图形验证码过期时间：5 分钟（单位：秒）
     */
    public static final long CAPTCHA_EXPIRE = 300;
```

- [ ] **步骤 4：创建 CaptchaVO**

创建文件 `liuliupi-server/src/main/java/com/liuliupi/vo/CaptchaVO.java`：

```java
package com.liuliupi.vo;

import lombok.Data;

/**
 * 图形验证码响应
 */
@Data
public class CaptchaVO {

    /**
     * 验证码 token，登录时回传用于校验
     */
    private String captchaToken;

    /**
     * base64 data-url（data:image/png;base64,...），前端 img.src 可直接使用
     */
    private String image;
}
```

- [ ] **步骤 5：UserService 接口新增 captcha() 方法**

在 `UserService.java` 中：

1. 顶部 import 区追加（如尚未导入）：
   ```java
   import com.liuliupi.vo.CaptchaVO;
   ```
2. 在接口体内（`login` 方法声明之后）新增方法声明：
   ```java
       /**
        * 生成图形验证码（公开接口，无需登录）
        *
        * @return 含 captchaToken 与 base64 图片的 VO
        */
       PoetryResult<CaptchaVO> captcha();
   ```

- [ ] **步骤 6：编写失败的测试（captcha 生成）**

创建文件 `liuliupi-server/src/test/java/com/liuliupi/service/impl/CaptchaTest.java`（先只放生成相关用例，校验用例在任务 2 补充）：

```java
package com.liuliupi.service.impl;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.utils.cache.PoetryCache;
import com.liuliupi.vo.CaptchaVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaTest {

    /**
     * captcha() 应生成 captchaToken、可直接用于 img.src 的 base64 图片，
     * 并把验证码文本写入缓存（key = CAPTCHA_KEY + captchaToken）。
     */
    @Test
    void captchaGeneratesTokenImageAndCachesCode() {
        UserServiceImpl service = new UserServiceImpl();

        PoetryResult<CaptchaVO> result = service.captcha();

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        CaptchaVO vo = result.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.getCaptchaToken()).isNotBlank();
        assertThat(vo.getImage()).startsWith("data:image/png;base64,");

        String cacheKey = CommonConst.CAPTCHA_KEY + vo.getCaptchaToken();
        String cachedCode = (String) PoetryCache.get(cacheKey);
        assertThat(cachedCode).isNotBlank();

        // 清理本用例写入的缓存
        PoetryCache.remove(cacheKey);
    }
}
```

- [ ] **步骤 7：运行测试验证失败**

运行：`mvn -q test -Dtest=CaptchaTest#captchaGeneratesTokenImageAndCachesCode`
预期：编译失败或测试 FAIL —— 报 `UserServiceImpl.captcha()` 未实现（接口方法在实现类缺失会导致编译错误，或实现未写时测试无法通过）。

- [ ] **步骤 8：实现 UserServiceImpl.captcha()**

在 `UserServiceImpl.java` 中：

1. 顶部 import 区追加：
   ```java
   import cn.hutool.captcha.CaptchaUtil;
   import cn.hutool.captcha.LineCaptcha;
   import com.liuliupi.vo.CaptchaVO;
   ```
   （`UUID`、`PoetryCache`、`CommonConst`、`PoetryResult` 该文件已导入，无需重复。）

2. 在 `login()` 方法之后新增实现（`@Override` 注解表明实现接口方法）：
   ```java
       @Override
       public PoetryResult<CaptchaVO> captcha() {
           // 1. 生成线段干扰验证码：宽200 高70，4 位字符，30 条干扰线
           LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 70, 4, 30);
           String code = captcha.getCode();
           // 2. 生成 token，作为缓存 key 后缀与登录回传凭据
           String captchaToken = UUID.randomUUID().toString().replaceAll("-", "");
           // 3. 写入缓存，5 分钟过期
           PoetryCache.put(CommonConst.CAPTCHA_KEY + captchaToken, code, CommonConst.CAPTCHA_EXPIRE);
           // 4. 取 base64 图片（容错：确保可直接用于 img.src，见决策 2）
           String image = captcha.getImageBase64();
           if (!image.startsWith("data:")) {
               image = "data:image/png;base64," + image;
           }
           // 5. 组装返回
           CaptchaVO vo = new CaptchaVO();
           vo.setCaptchaToken(captchaToken);
           vo.setImage(image);
           return PoetryResult.success(vo);
       }
   ```

- [ ] **步骤 9：运行测试验证通过**

运行：`mvn -q test -Dtest=CaptchaTest#captchaGeneratesTokenImageAndCachesCode`
预期：`BUILD SUCCESS`，`Tests run: 1, Failures: 0`。

- [ ] **步骤 10：UserController 新增 GET /user/captcha**

在 `UserController.java` 中：

1. 顶部 import 区追加：
   ```java
   import com.liuliupi.vo.CaptchaVO;
   ```
2. 在 `login()` 方法（第 47-51 行）之前，新增公开接口（**无 `@LoginCheck`**）：
   ```java
       /**
        * 获取图形验证码（公开接口，进入登录页即可调用，无需登录）
        */
       @GetMapping("/captcha")
       public PoetryResult<CaptchaVO> captcha() {
           return userService.captcha();
       }
   ```

- [ ] **步骤 11：编译验证整体可编译**

运行：`mvn -q test-compile`
预期：`BUILD SUCCESS`，无编译错误。

- [ ] **步骤 12：Commit**

```bash
git add liuliupi-server/pom.xml \
        liuliupi-server/src/main/java/com/liuliupi/constants/CommonConst.java \
        liuliupi-server/src/main/java/com/liuliupi/vo/CaptchaVO.java \
        liuliupi-server/src/main/java/com/liuliupi/service/UserService.java \
        liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java \
        liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java \
        liuliupi-server/src/test/java/com/liuliupi/service/impl/CaptchaTest.java
git commit -m "feat(server): 新增图形验证码生成接口 GET /user/captcha"
```

---

## 任务 2：后端 — 登录接口接入验证码校验（一次性消费）

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/UserService.java`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java`
- 修改：`liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java`
- 修改测试：`liuliupi-server/src/test/java/com/liuliupi/service/impl/CaptchaTest.java`

- [ ] **步骤 1：UserService 接口 login() 签名变更**

在 `UserService.java` 中，把现有 `login` 声明（约第 30 行）：

```java
    PoetryResult<UserVO> login(String account, String password);
```

替换为：

```java
    /**
     * 用户名、邮箱、手机号/密码登录
     * 统一 Token 体系，不再区分前后台登录
     *
     * @param account      账号（用户名/邮箱/手机号）
     * @param password     密码（AES 加密后）
     * @param captchaToken 图形验证码 token（来自 GET /user/captcha）
     * @param code         用户输入的图形验证码
     * @return 用户信息（含 Token）
     */
    PoetryResult<UserVO> login(String account, String password, String captchaToken, String code);
```

- [ ] **步骤 2：编写失败的测试（verifyCaptcha 全分支）**

在 `CaptchaTest.java` 中追加（保留任务 1 已有的生成用例）以下内容。注意 `verifyCaptcha` 是 `UserServiceImpl` 的**包级静态方法**，本测试类与实现类同包（`com.liuliupi.service.impl`），可直接调用：

```java
    private static final String FIXED_TOKEN = "fixed-captcha-token";

    @org.junit.jupiter.api.AfterEach
    void cleanFixedToken() {
        PoetryCache.remove(com.liuliupi.constants.CommonConst.CAPTCHA_KEY + FIXED_TOKEN);
    }

    /** captchaToken 不存在/过期 → "验证码已失效，请刷新！" */
    @Test
    void verifyReturnsExpiredWhenTokenNotCached() {
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "ABCD");
        assertThat(error).isEqualTo("验证码已失效，请刷新！");
    }

    /** 验证码比对不一致 → "验证码错误！" */
    @Test
    void verifyReturnsErrorWhenCodeMismatch() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "WRONG");
        assertThat(error).isEqualTo("验证码错误！");
    }

    /** 大小写不一致但字母相同 → 通过（返回 null） */
    @Test
    void verifyPassesWhenCodeMatchesIgnoringCase() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "abcd");
        assertThat(error).isNull();
    }

    /** 一次性消费：无论对错，校验后立即删除，第二次用同一 token 必返回"已失效" */
    @Test
    void verifyConsumesTokenOnceSoSecondAttemptFails() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        // 第一次（错误码）消费掉 token
        UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "WRONG");
        // 第二次即使传正确码，token 已被删 → 已失效
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "ABCD");
        assertThat(error).isEqualTo("验证码已失效，请刷新！");
    }
```

> 该测试文件顶部需确保已导入 `CommonConst` 与 `PoetryCache`（任务 1 已导入）。`@AfterEach` 用全限定名引入避免顶部 import 杂乱；如已在顶部 import `org.junit.jupiter.api.AfterEach` 则用短名。

- [ ] **步骤 3：运行测试验证失败**

运行：`mvn -q test -Dtest=CaptchaTest`
预期：编译失败 —— `UserServiceImpl.verifyCaptcha(String,String)` 方法不存在（且 `login()` 签名变更后实现类尚未同步，也会报编译错误）。

- [ ] **步骤 4：新增 verifyCaptcha() 包级静态方法**

在 `UserServiceImpl.java` 中，紧接 `captcha()` 方法之后新增（见决策 1）：

```java
       /**
        * 校验图形验证码（一次性消费：无论校验对错，校验后立即从缓存删除，防同一张图被反复枚举）。
        * <p>
        * 提取为独立可测单元，避免 login() 的 DB 查询干扰单元测试。
        *
        * @param captchaToken 验证码 token
        * @param code         用户输入的验证码
        * @return null 表示校验通过；非 null 表示错误提示信息
        */
       static String verifyCaptcha(String captchaToken, String code) {
           // 1. 取出缓存的验证码文本
           String cachedCode = (String) PoetryCache.get(CommonConst.CAPTCHA_KEY + captchaToken);
           // 2. 一次性消费：无论对错立即删除
           PoetryCache.remove(CommonConst.CAPTCHA_KEY + captchaToken);
           // 3. 校验
           if (!StringUtils.hasText(captchaToken) || cachedCode == null) {
               return "验证码已失效，请刷新！";
           }
           if (!cachedCode.equalsIgnoreCase(code)) {
               return "验证码错误！";
           }
           return null;
       }
```

> `StringUtils` 指 `org.springframework.util.StringUtils`，该文件已导入（`login()` 已用 `StringUtils.hasText`）。

- [ ] **步骤 5：改造 login() 签名并接入校验**

在 `UserServiceImpl.java` 中，把现有 `login()`（第 75-123 行）的方法签名与方法体开头改造：

**签名：** 由
```java
    public PoetryResult<UserVO> login(String account, String password) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));
```
改为：
```java
    public PoetryResult<UserVO> login(String account, String password, String captchaToken, String code) {
        // 1. 图形验证码校验（一次性消费）。校验不通过直接返回，不走后续解密/查库逻辑。
        String captchaError = verifyCaptcha(captchaToken, code);
        if (captchaError != null) {
            return PoetryResult.fail(captchaError);
        }
        // 2. AES 解密
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));
```

> 方法体其余部分（`lambdaQuery` 查询、Token 生成、UserVO 构建等）**保持不变**。校验逻辑插入在 AES 解密之前，与规格 §1.5 一致。

- [ ] **步骤 6：运行 verifyCaptcha 测试验证通过**

运行：`mvn -q test -Dtest=CaptchaTest`
预期：`BUILD SUCCESS`，`Tests run: 5, Failures: 0`（1 个生成 + 4 个校验分支）。

- [ ] **步骤 7：UserController login() 签名变更**

在 `UserController.java` 中，把现有 `login()`（第 47-51 行）：

```java
    @PostMapping("/login")
    public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                      @RequestParam("password") String password) {
        return userService.login(account, password);
    }
```

替换为：

```java
    @PostMapping("/login")
    public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                      @RequestParam("password") String password,
                                      @RequestParam("captchaToken") String captchaToken,
                                      @RequestParam("code") String code) {
        return userService.login(account, password, captchaToken, code);
    }
```

- [ ] **步骤 8：编译验证整体可编译**

运行：`mvn -q test-compile`
预期：`BUILD SUCCESS`。

- [ ] **步骤 9：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/service/UserService.java \
        liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java \
        liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java \
        liuliupi-server/src/test/java/com/liuliupi/service/impl/CaptchaTest.java
git commit -m "feat(server): 登录接口接入图形验证码校验（一次性消费）"
```

---

## 任务 3：前端 — 前台登录表单接入图形验证码（user.vue）

**文件：**
- 修改：`liuliupi-ui/src/components/user.vue`

> 前端无测试框架，本任务以「手动验证清单」收尾。

- [ ] **步骤 1：data() 新增验证码相关状态**

在 `user.vue` 的 `data() { return { ... } }` 中，找到现有的 `account: "",` / `password: "",`（约第 218-219 行），在其后追加：

```js
        account: "",
        password: "",
        // 图形验证码（登录专用，与注册/找回密码的 code 区分）
        captchaToken: "",
        captchaImage: "",
        captchaCode: "",
```

- [ ] **步骤 2：模板新增验证码图片 + 输入框**

在 `<template>` 中，找到登录表单块 `sign-in-container`（约第 27-35 行）：

```html
        <div class="form-container sign-in-container">
          <div class="myCenter">
            <h1>登录</h1>
            <input v-model="account" type="text" placeholder="用户名/邮箱/手机号">
            <input v-model="password" type="password" placeholder="密码">
            <a href="#" @click="changeDialog('找回密码')">忘记密码？</a>
            <button @click="login()">登录</button>
          </div>
        </div>
```

在密码 `<input>` 之后、「忘记密码」`<a>` 之前插入验证码行：

```html
            <input v-model="password" type="password" placeholder="密码">
            <div class="login-captcha">
              <input v-model="captchaCode" type="text" placeholder="验证码">
              <img :src="captchaImage" @click="getCaptcha()"
                   class="login-captcha-img" alt="验证码" title="点击刷新">
            </div>
            <a href="#" @click="changeDialog('找回密码')">忘记密码？</a>
```

- [ ] **步骤 3：created() 进入页面即获取验证码**

把空的 `created()`（约第 232 行）改为：

```js
    created() {
      this.getCaptcha();
    },
```

- [ ] **步骤 4：新增 getCaptcha() 方法**

在 `methods: { ... }` 中（建议放在 `login()` 之前）新增：

```js
      getCaptcha() {
        this.$http.get(this.$constant.baseURL + "/user/captcha")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.captchaToken = res.data.captchaToken;
              this.captchaImage = res.data.image;
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
```

- [ ] **步骤 5：改造 login() —— 校验非空、传参、失败刷新**

把现有 `login()`（约第 246-276 行）整体替换为：

```js
      login() {
        if (this.$common.isEmpty(this.account) || this.$common.isEmpty(this.password)) {
          this.$message({
            message: "请输入账号或密码！",
            type: "error"
          });
          return;
        }

        if (this.$common.isEmpty(this.captchaCode)) {
          this.$message({
            message: "请输入验证码！",
            type: "error"
          });
          return;
        }

        let user = {
          account: this.account.trim(),
          password: this.$common.encrypt(this.password.trim()),
          captchaToken: this.captchaToken,
          code: this.captchaCode.trim()
        };

        this.$http.post(this.$constant.baseURL + "/user/login", user, false, false)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.$store.commit("loadCurrentUser", res.data);
              localStorage.setItem("userToken", res.data.accessToken);
              this.account = "";
              this.password = "";
              this.captchaCode = "";
              this.$router.push({path: '/'});
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
            // 登录失败：验证码已被一次性消费，刷新图片并清空输入
            this.captchaCode = "";
            this.getCaptcha();
          });
      },
```

> `post(..., false, false)` 第三参 `false` = `json=false`（form-urlencoded），保持原样以匹配后端 `@RequestParam`；验证码参数加入同一 `user` 对象（见决策 3）。

- [ ] **步骤 6：追加验证码图片样式**

在 `user.vue` 末尾的 `<style scoped>`（约第 625 行起）内追加：

```css
  .login-captcha {
    display: flex;
    align-items: center;
    width: 100%;
  }

  .login-captcha input {
    flex: 1;
  }

  .login-captcha-img {
    width: 110px;
    height: 40px;
    margin-left: 10px;
    cursor: pointer;
    border-radius: 4px;
  }
```

- [ ] **步骤 7：手动验证清单**

启动前端（`liuliupi-ui` 目录 `npm run serve`）与后端，逐项确认：

- [ ] 打开前台登录页，验证码图片自动出现（非空白/非裂图）。
- [ ] 点击验证码图片，图片刷新（token 变化）。
- [ ] 不填验证码点登录 → 提示「请输入验证码！」。
- [ ] 填错验证码点登录 → 提示「验证码错误！」，且图片自动刷新、输入框清空。
- [ ] 故意等 >5 分钟（或手动清缓存）再提交 → 提示「验证码已失效，请刷新！」。
- [ ] 填正确验证码 + 正确账密 → 登录成功，跳转首页。
- [ ] 填正确验证码 + 错误账密 → 提示「账号/密码错误」类提示，图片刷新。
- [ ] 同一验证码重复提交两次 → 第二次必提示「已失效」（一次性消费）。

- [ ] **步骤 8：Commit**

```bash
git add liuliupi-ui/src/components/user.vue
git commit -m "feat(ui): 前台登录表单接入图形验证码"
```

---

## 任务 4：前端 — 后台登录表单接入图形验证码（verify.vue）

**文件：**
- 修改：`liuliupi-ui/src/components/admin/verify.vue`

- [ ] **步骤 1：data() 新增验证码相关状态**

在 `verify.vue` 的 `data() { return { ... } }`（约第 35-41 行）中，把：

```js
      return {
        redirect: this.$route.query.redirect,
        account: "",
        password: ""
      }
```

替换为：

```js
      return {
        redirect: this.$route.query.redirect,
        account: "",
        password: "",
        captchaToken: "",
        captchaImage: "",
        captchaCode: ""
      }
```

- [ ] **步骤 2：模板新增验证码图片 + 输入框**

在 `<template>` 中，密码 `el-input` 块（约第 12-16 行）之后、`proButton` 块之前插入：

```html
      <div>
        <el-input v-model="password" type="password">
          <template slot="prepend">密码</template>
        </el-input>
      </div>
      <div class="captcha-row">
        <el-input v-model="captchaCode" placeholder="验证码" class="captcha-input"></el-input>
        <img :src="captchaImage" @click="getCaptcha()"
             class="captcha-img" alt="验证码" title="点击刷新">
      </div>
      <div>
        <proButton :info="'提交'"
```

- [ ] **步骤 3：created() 增加获取验证码**

在现有 `created()`（约第 43-50 行）末尾追加一行 `this.getCaptcha();`，最终形如：

```js
    created() {
      let sysConfig = this.$store.state.sysConfig;
      if (!this.$common.isEmpty(sysConfig) && !this.$common.isEmpty(sysConfig['webStaticResourcePrefix'])) {
        let root = document.querySelector(":root");
        let webStaticResourcePrefix = sysConfig['webStaticResourcePrefix'];
        root.style.setProperty("--backgroundPicture", "url(" + webStaticResourcePrefix + "assets/backgroundPicture.jpg)");
      }
      this.getCaptcha();
    },
```

- [ ] **步骤 4：新增 getCaptcha() 方法**

在 `methods: { ... }` 中（`login()` 之前）新增：

```js
      getCaptcha() {
        this.$http.get(this.$constant.baseURL + "/user/captcha")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.captchaToken = res.data.captchaToken;
              this.captchaImage = res.data.image;
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
```

- [ ] **步骤 5：改造 login() —— 校验非空、传参、失败刷新**

把现有 `login()`（约第 52-83 行）整体替换为：

```js
      login() {
        if (this.$common.isEmpty(this.account) || this.$common.isEmpty(this.password)) {
          this.$message({
            message: "请输入账号或密码！",
            type: "error"
          });
          return;
        }

        if (this.$common.isEmpty(this.captchaCode)) {
          this.$message({
            message: "请输入验证码！",
            type: "error"
          });
          return;
        }

        let user = {
          account: this.account.trim(),
          password: this.$common.encrypt(this.password.trim()),
          captchaToken: this.captchaToken,
          code: this.captchaCode.trim()
        };

        this.$http.post(this.$constant.baseURL + "/user/login", user, false)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              // 统一 Token 体系：使用 currentUser 和 userToken
              localStorage.setItem("userToken", res.data.accessToken);
              this.$store.commit("loadCurrentUser", res.data);
              this.account = "";
              this.password = "";
              this.captchaCode = "";
              this.$router.push({path: this.redirect});
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
            // 验证码已被一次性消费，刷新图片并清空输入
            this.captchaCode = "";
            this.getCaptcha();
          });
      }
```

> 注意：`verify.vue` 原 `post(..., false)` 是 3 参（`json=false`），保持 3 参即可，无需像 `user.vue` 那样多传一个 `false`。

- [ ] **步骤 6：追加验证码样式**

在 `verify.vue` 末尾的 `<style scoped>`（约第 88 行起）内、现有 `.verify-content > div:last-child > div` 规则之后追加：

```css
  .captcha-row {
    display: flex;
    align-items: center;
  }

  .captcha-input {
    flex: 1;
  }

  .captcha-img {
    width: 120px;
    height: 40px;
    margin-left: 10px;
    cursor: pointer;
    border-radius: 4px;
  }
```

- [ ] **步骤 7：手动验证清单**

启动后台登录页（访问后台路由），逐项确认（与任务 3 清单等价，此处针对后台）：

- [ ] 后台登录页验证码图片自动出现。
- [ ] 点击图片刷新。
- [ ] 不填验证码 → 提示「请输入验证码！」。
- [ ] 错验证码 → 提示「验证码错误！」+ 图片刷新 + 输入清空。
- [ ] 过期提交 → 提示「已失效」。
- [ ] 正确验证码 + 正确站长账密 → 登录成功跳转后台（`redirect`）。
- [ ] 一次性消费：同验证码第二次提交必失败。

- [ ] **步骤 8：Commit**

```bash
git add liuliupi-ui/src/components/admin/verify.vue
git commit -m "feat(ui): 后台登录表单接入图形验证码"
```

---

## 任务 5：端到端验证与收尾

- [ ] **步骤 1：后端全量测试**

运行：`mvn -q test`
预期：`BUILD SUCCESS`，全部测试通过（含 `CaptchaTest` 5 个、`LoginCheckAspectTest` 原有用例，无回归）。

- [ ] **步骤 2：端到端冒烟（前后台）**

启动后端与前端，分别用前台（`user.vue`）与后台（`verify.vue`）登录入口，完成一次「正确验证码 + 正确账密 → 登录成功」全流程，确认两处入口行为一致。

- [ ] **步骤 3：检查工作区干净**

运行：`git status`
预期：`nothing to commit, working tree clean`（本计划所有变更已在任务 1-4 提交）。

---

## 自检结果

**1. 规格覆盖度**

| 规格章节 | 对应任务 |
|---|---|
| §1.1 新增 hutool-captcha 依赖 | 任务 1 步骤 1-2 |
| §1.2 新增 CAPTCHA_KEY / CAPTCHA_EXPIRE 常量 | 任务 1 步骤 3 |
| §1.3 新增 GET /user/captcha（无 @LoginCheck） | 任务 1 步骤 8（service）、步骤 10（controller） |
| §1.4 新增 CaptchaVO | 任务 1 步骤 4 |
| §1.5 改造 POST /user/login（签名+校验+一次性消费） | 任务 2 全部 |
| §2.1 前台 user.vue | 任务 3 全部 |
| §2.2 后台 verify.vue | 任务 4 全部 |
| §3 数据流 | 任务 1-4 整体实现，任务 5 端到端验证 |
| §错误处理（失效/错误/一次性消费） | 任务 2 步骤 2 测试全分支覆盖 |
| §测试策略（captcha 生成、login 校验分支、大小写、一次性消费） | 任务 1 步骤 6（生成）、任务 2 步骤 2（4 个校验分支） |
| §边界情况（5 分钟过期、重复提交、两入口） | 测试覆盖一次性消费；过期由 TTL 与"失效"分支保证；两入口由任务 3/4 保证 |
| §范围外（注册防爆破等） | 明确不做 ✓ |

无遗漏。

**2. 占位符扫描** — 已逐项核对，所有代码步骤均含完整代码块，命令均含预期输出，无 "TODO/待定/类似任务 N" 等红旗。

**3. 类型一致性**

- `verifyCaptcha(String captchaToken, String code) → String`（null 表通过）—— 定义于任务 2 步骤 4，调用于任务 2 步骤 5，测试于任务 2 步骤 2。命名一致 ✓
- `captcha() → PoetryResult<CaptchaVO>` —— 接口（任务 1 步骤 5）、实现（任务 1 步骤 8）、controller（任务 1 步骤 10）一致 ✓
- `login(String, String, String, String) → PoetryResult<UserVO>` —— 接口（任务 2 步骤 1）、实现（任务 2 步骤 5）、controller（任务 2 步骤 7）签名一致 ✓
- `CaptchaVO` 字段：`captchaToken`、`image` —— 定义（任务 1 步骤 4）、赋值（步骤 8）、断言（步骤 6）一致 ✓
- 常量：`CommonConst.CAPTCHA_KEY`、`CommonConst.CAPTCHA_EXPIRE` —— 定义（任务 1 步骤 3）后所有引用一致 ✓
- 前端变量：`captchaToken`/`captchaImage`/`captchaCode` —— 前端表单绑定 `captchaCode`，请求体 `code: this.captchaCode`（映射后端 `@RequestParam("code")`），`captchaToken: this.captchaToken`（映射后端 `@RequestParam("captchaToken")`）。前后端字段一一对应 ✓

---

## 并发注意事项

本计划与 `2026-06-13-im-sso-auth-code-design.md` 共享以下后端文件，若两者并发实现，需人工合并（方法级不重叠，合并应为追加式）：

- `controller/UserController.java`：本计划改 `login` + 新增 `captcha`；SSO 计划改 `token` + 新增 `ssoCode`。
- `service/impl/UserServiceImpl.java`：本计划改 `login` + 新增 `captcha`/`verifyCaptcha`；SSO 计划改 `token`。
- `constants/CommonConst.java`：两边均追加常量（本计划加 `CAPTCHA_KEY`/`CAPTCHA_EXPIRE`），各自独立，追加即可。

建议后端改动由同一序列/worktree 完成，或并发后由人工合并上述三个文件。
