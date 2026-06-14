# 登录图形验证码设计（问题 4：登录防爆破）

> 日期：2026-06-13
> 状态：待审查

## 背景

当前登录接口 `POST /user/login` 无任何防爆破机制——没有失败计数、没有验证码门槛、没有 IP 限流。共有两个登录入口都调用该接口：

- 前台用户登录：`liuliupi-ui/src/components/user.vue:260`
- 后台管理员登录：`liuliupi-ui/src/components/admin/verify.vue:66`

攻击者可对 `/user/login` 进行无限制暴力撞库。后端目前没有任何图形验证码基础设施（仅有短信/邮件验证码）。

## 目标

- 为 `/user/login` 增加图形验证码校验，**始终要求**（每次登录都必须输入）。
- 复用项目既有技术栈（hutool）与缓存（`PoetryCache`），最小化新依赖。
- 前台与后台两个登录入口同步生效。

## 方案

引入 `hutool-captcha` 生成图形验证码（项目已通过 `hutool-bom` 管理 hutool 版本，已引入 `hutool-crypto`、`hutool-extra`，验证码模块为同栈轻量增量）。验证码文本存入 `PoetryCache`（短 TTL），登录时校验。验证码**一次性消费**（无论校验对错，校验后立即从缓存删除，防止同一张图被反复枚举）。

## 改造详情

### 1. 后端（liuliupi-server）

#### 1.1 新增依赖（`pom.xml`）

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-captcha</artifactId>
</dependency>
```

（版本由 `hutool-bom` 统一管理，无需指定 `<version>`。）

#### 1.2 新增常量（`constants/CommonConst.java`）

```java
/** 图形验证码缓存 key 前缀 */
public static final String CAPTCHA_KEY = "captcha_";
/** 图形验证码过期时间：5 分钟 */
public static final long CAPTCHA_EXPIRE = 300;
```

#### 1.3 新增接口 `GET /user/captcha`（`controller/UserController.java`）

- **无 `@LoginCheck`**（进入登录页即可调用，公开接口）。
- 逻辑：
  1. `LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 70, 4, 30);`
  2. `String code = captcha.getCode();`
  3. `String captchaToken = UUID.randomUUID().toString().replaceAll("-", "");`
  4. `PoetryCache.put(CommonConst.CAPTCHA_KEY + captchaToken, code, CommonConst.CAPTCHA_EXPIRE);`
  5. 取 base64 图片：`String image = captcha.getImageBase64();`
     - hutool 5.8 的 `getImageBase64()` 返回**带** `data:image/png;base64,` 前缀的字符串，前端 `img.src` 可直接使用。
     - 实现时以 hutool 5.8.22 实际 API 为准；若该方法返回纯 base64，则改用 `getImageBase64Data()` 并手动拼 `data:image/png;base64,` 前缀。
- 返回 `PoetryResult<CaptchaVO>`。

#### 1.4 新增 VO（`vo/CaptchaVO.java`）

```java
@Data
public class CaptchaVO {
    private String captchaToken;  // 校验时回传
    private String image;         // base64 data-url，前端直接渲染
}
```

#### 1.5 改造 `POST /user/login`

**接口签名变更**（`controller/UserController.java:47`）：

```
改造前: POST /user/login?account=xxx&password=xxx
改造后: POST /user/login?account=xxx&password=xxx&captchaToken=xxx&code=xxx
```

**`UserServiceImpl.login()`（`service/impl/UserServiceImpl.java:76`）签名变更**：

```java
PoetryResult<UserVO> login(String account, String password, String captchaToken, String code);
```

**校验逻辑（插入到现有 AES 解密 / 账号密码校验之前）**：

```java
// 1. 取出缓存的验证码文本
String cachedCode = (String) PoetryCache.get(CommonConst.CAPTCHA_KEY + captchaToken);
// 2. 一次性消费：无论对错立即删除，防同一张图反复枚举
PoetryCache.remove(CommonConst.CAPTCHA_KEY + captchaToken);
// 3. 校验
if (!StringUtils.hasText(captchaToken) || cachedCode == null) {
    return PoetryResult.fail("验证码已失效，请刷新！");
}
if (!cachedCode.equalsIgnoreCase(code)) {
    return PoetryResult.fail("验证码错误！");
}
// 4. 通过 → 继续原有 AES 解密 + 账号密码逻辑
```

### 2. 前端（liuliupi-ui）

#### 2.1 前台登录（`components/user.vue`）

- 进入登录页 `mounted` / 登录失败时，调用 `GET /user/captcha` 获取 `{captchaToken, image}`。
- 表单新增「验证码图片（`<img :src="image" @click="refreshCaptcha">`）+ 验证码输入框」。
- 登录请求 body 增加 `captchaToken`、`code`：
  ```js
  let user = {
    account: this.account.trim(),
    password: this.$common.encrypt(this.password.trim()),
    captchaToken: this.captchaToken,
    code: this.code.trim()
  };
  ```
- 登录失败（`catch`）若提示含"验证码"，自动刷新验证码图片。

#### 2.2 后台登录（`components/admin/verify.vue`）

同 2.1 的改动，应用到后台登录表单（`verify.vue:61` 附近的登录请求）。

### 3. 数据流

```
进入登录页
  → GET /user/captcha
  → {captchaToken, image} 渲染图片
  → 用户填 账号/密码/验证码
  → POST /user/login?account&password&captchaToken&code
  → 后端：验证码校验（一次性消费）→ AES 解密密码 → 账号密码比对
  → 成功返回 UserVO / 失败返回相应提示
```

## 错误处理

| 场景 | 响应 |
|---|---|
| captchaToken 为空 / 缓存无记录（过期或未获取） | `fail("验证码已失效，请刷新！")` |
| 验证码比对不一致（忽略大小写） | `fail("验证码错误")` |
| 上述两种情况均**一次性消费**该 token | 第二次用同一 token 必然返回"已失效" |

## 测试策略（TDD）

参照项目已有 `src/test/java/com/liuliupi/aop/LoginCheckAspectTest.java` 的单测模式：

- `UserController.captcha`：生成成功、返回含 captchaToken 与 image、缓存已写入。
- `UserServiceImpl.login` 验证码校验分支：
  - 正确验证码 → 放行进入账号密码逻辑
  - 错误验证码 → `fail("验证码错误")`
  - captchaToken 不存在/过期 → `fail("验证码已失效，请刷新")`
  - 大小写不一致但字母相同 → 通过
  - **一次性消费**：同一 captchaToken 第二次校验必失败
- 前端无测试框架，提供手动验证清单（验证码展示、点击刷新、登录失败自动刷新）。

## 边界情况

| 场景 | 处理 |
|---|---|
| 用户打开登录页后停留超过 5 分钟才提交 | 验证码过期 → 提示刷新 |
| 用户重复提交同一验证码 | 一次性消费，第二次失败 |
| `/user/captcha` 被恶意高频调用刷接口 | 本次不做限流（YAGNI），后续可加 `@SaveCheck` |
| 后台与前台登录入口 | 两处都加验证码，逻辑一致 |

## 范围外（本次不做）

- 注册接口 `/user/regist` 的防爆破（问题 4 严格指登录）。
- Redis 迁移、MD5→BCrypt、AES 密钥外置等其他架构问题。

## 涉及文件清单

**后端（liuliupi-server）：**
- `pom.xml` — 新增 `hutool-captcha` 依赖
- `constants/CommonConst.java` — 新增 `CAPTCHA_KEY`、`CAPTCHA_EXPIRE`
- `vo/CaptchaVO.java` — 新增 VO
- `controller/UserController.java` — 新增 `GET /user/captcha`；`login()` 增加 captchaToken/code 参数
- `service/UserService.java` — `login()` 接口签名变更
- `service/impl/UserServiceImpl.java` — `login()` 增加验证码校验逻辑
- `src/test/...` — 新增 captcha 与 login 验证码单测

**前端（liuliupi-ui）：**
- `components/user.vue` — 登录表单加验证码
- `components/admin/verify.vue` — 后台登录表单加验证码

## 并发实现注意事项

本规格与 `2026-06-13-im-sso-auth-code-design.md` 是**两个独立规格**，可并发实现。但二者共享以下后端文件，并发修改时需注意合并冲突：

- `controller/UserController.java`（本规格改 `login` + 新增 `captcha`；SSO 规格改 `token` + 新增 `ssoCode`，方法级不重叠）
- `service/impl/UserServiceImpl.java`（本规格改 `login`；SSO 规格改 `token`，方法级不重叠）
- `constants/CommonConst.java`（两边都新增常量，追加即可）

建议：后端改动由同一序列/worktree 完成，或并发后由人工合并这三个文件。
