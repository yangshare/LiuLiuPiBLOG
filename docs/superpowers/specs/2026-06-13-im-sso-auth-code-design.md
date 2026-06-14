# IM 站点 SSO 一次性授权码设计（问题 7）

> 日期：2026-06-13
> 状态：待审查

## 背景

博客主站（liuliupi-ui）与 IM 子站（liuliupi-im-ui）是两个独立的 Vue 应用。IM 站没有独立登录，依赖主站通过 `/user/token` 接口建立登录态。当前 SSO 传递机制存在多重缺陷：

1. **token 进 URL**：主站把 accessToken 用 AES 加密后拼进 URL `?userToken=xxx`，通过 `window.open` 跳转 IM 站（`home.vue:376`、`user.vue:328`）。URL 会进入浏览器历史、服务器访问日志、Referer 头，**token 泄露面很大**。
2. **AES 密钥硬编码**：SSO 安全性绑死在公开的 `CRYPOTJS_KEY = "sarasarasarasara"`（`CommonConst.java:124`）上，前后端共享、前端可反编译，密钥泄露即可伪造任意用户 token。
3. **同步 XHR**：IM 站 `main.js:71` 用 `xhr.open(..., false)` 同步请求换取用户信息，阻塞主线程，且同步 XHR 已被现代浏览器逐步废弃。

> 说明：`/user/token` 不是冗余接口，而是 **IM 站唯一的登录入口**（主站自身不调用它），不能删除，需要改造的是凭证传递方式。

## 目标

- 消除"token 进 URL"这个最大泄露面。
- 顺带摆脱 SSO 对 AES 公开密钥的依赖。
- 将 IM 站的同步 XHR 改为异步请求。
- 不要求主站与 IM 站同根域名（保持现有部署灵活性）。

## 方案

采用 **OAuth 式一次性授权码**：主站跳转前先向后端换取一个**短时（30s）、一次性**的 `ssoCode`，只把 code 放进 URL；IM 站用 code 向 `/user/token` 换取真正的 accessToken。token 永不进 URL，code 泄露窗口极小且用一次即焚。

## 改造详情

### 1. 后端（liuliupi-server）

#### 1.1 新增常量（`constants/CommonConst.java`）

```java
/** SSO 一次性授权码缓存 key 前缀 */
public static final String SSO_CODE = "sso_code_";
/** SSO 授权码过期时间：30 秒 */
public static final long SSO_CODE_EXPIRE = 30;
```

#### 1.2 新增接口 `POST /user/ssoCode`（`controller/UserController.java`）

- **需 `@LoginCheck`**（主站已登录用户才能换码）。
- 逻辑：
  1. `Integer userId = PoetryUtil.getUserId();`
  2. `String ssoCode = UUID.randomUUID().toString().replaceAll("-", "");`
  3. `PoetryCache.put(CommonConst.SSO_CODE + ssoCode, userId, CommonConst.SSO_CODE_EXPIRE);`
  4. 返回 `PoetryResult.success(ssoCode);`（`data` 即 code 字符串）

#### 1.3 改造 `POST /user/token`

**接口签名变更**（`controller/UserController.java:57`）：

```
改造前: POST /user/token?userToken=<AES密文>
改造后: POST /user/token?ssoCode=<授权码>
```

**`UserServiceImpl.token()`（`service/impl/UserServiceImpl.java:521`）改造**：

- **删除** AES 解密行：`userToken = new String(SecureUtil.aes(...).decrypt(userToken));`（不再依赖 `CRYPOTJS_KEY`）
- 方法签名 `token(String userToken)` → `token(String ssoCode)`
- 新逻辑：
  ```java
  // 1. 用 ssoCode 换 userId
  Integer userId = (Integer) PoetryCache.get(CommonConst.SSO_CODE + ssoCode);
  if (userId == null) {
      throw new PoetryRuntimeException("授权码无效或已过期！");
  }
  // 2. 一次性消费
  PoetryCache.remove(CommonConst.SSO_CODE + ssoCode);
  // 3. 通过统一 Token 体系的既有映射，反查当前有效 accessToken
  String accessToken = (String) PoetryCache.get(CommonConst.TOKEN + userId);
  if (!StringUtils.hasText(accessToken)) {
      throw new PoetryRuntimeException("登录已过期，请重新登录！");
  }
  // 4. 取用户信息并返回
  User user = (User) PoetryCache.get(accessToken);
  UserVO userVO = new UserVO();
  BeanUtils.copyProperties(user, userVO);
  userVO.setPassword(null);
  userVO.setAccessToken(accessToken);
  return PoetryResult.success(userVO);
  ```

> 旧 AES 入口**直接废弃**，不保留兼容（主站 + IM 站 + 后端三方同步发布，无第三方调用方）。

### 2. 主站前端（liuliupi-ui）

#### 2.1 `components/home.vue:376` 跳转改造

```js
// 改造前
let userToken = this.$common.encrypt(localStorage.getItem("userToken"));
window.open(this.$constant.imBaseURL + "?userToken=" + userToken + "&defaultStoreType=" + ...);

// 改造后
this.$http.post(this.$constant.baseURL + "/user/ssoCode", {}).then((res) => {
  window.open(this.$constant.imBaseURL + "?ssoCode=" + res.data + "&defaultStoreType=" + ...);
});
```

#### 2.2 `components/user.vue:328` 跳转改造

同 2.1，把 `encrypt(userToken)` 拼 URL 改为「先 `POST /user/ssoCode` 拿 code，再拼 `?ssoCode=`」。

两处均**移除** `$common.encrypt(...)` 调用。

### 3. IM 站前端（liuliupi-im-ui）

#### 3.1 `main.js:68` 路由守卫改造

```js
// 改造前：同步 XHR + userToken
if (typeof to.query.userToken !== "undefined") {
  let userToken = to.query.userToken;
  const xhr = new XMLHttpRequest();
  xhr.open('post', constant.baseURL + "/user/token", false);   // 同步，阻塞
  xhr.send("userToken=" + userToken);
  ...
}

// 改造后：异步 + ssoCode
if (typeof to.query.ssoCode !== "undefined") {
  let ssoCode = to.query.ssoCode;
  // 复用 IM 站现有 http 封装（http 已在 main.js 顶部 import，即同文件 $http = http 处，路由守卫可直接用）
  // http.post(url, params, json=false) → 内部 qs.stringify 发 form-urlencoded，匹配后端 @RequestParam
  http.post(constant.baseURL + "/user/token", { ssoCode }, false).then((res) => {
    // res 即后端 PoetryResult 对象 { code, message, data }（request.js 内部已 resolve(res.data) 剥去一层）
    if (res.code === 200) {
      store.commit("loadCurrentUser", res.data);
      localStorage.setItem("userToken", res.data.accessToken);
      window.location.href = constant.imURL;
    } else {
      window.location.href = constant.webBaseURL;
    }
  }).catch(() => {
    window.location.href = constant.webBaseURL;
  });
  return; // 异步处理中，先不调 next()
}
```

> Vue Router 4 路由守卫异步处理：在异步回调内完成 `window.location.href` 跳转，守卫本身 `return` 不立即 `next()`。

### 4. 数据流

```
主站已登录用户点击「进入 IM」
  → POST /user/ssoCode（header 自带主站 accessToken，@LoginCheck 校验）
  → 后端生成 ssoCode（30s、一次性），存缓存，返回 code
  → 主站 window.open(imBaseURL?ssoCode=xxx)
  → IM 站路由守卫取 query.ssoCode
  → 异步 POST /user/token?ssoCode=xxx
  → 后端：消费 code → 查 TOKEN+userId 得 accessToken → 返回 UserVO+accessToken
  → IM 站存 localStorage（后续请求 header Authorization 带该 token，认证机制不变）
```

## 错误处理

| 场景 | 响应 |
|---|---|
| ssoCode 不存在/过期/已被使用 | `PoetryRuntimeException("授权码无效或已过期！")` → code 500 |
| 主站 accessToken 已过期（`TOKEN+userId` 缓存为空） | `PoetryRuntimeException("登录已过期，请重新登录！")` |
| IM 站收到非 200 | 跳回主站 `webBaseURL` |

## 安全收益

- ✅ token 永不进 URL / 访问日志 / Referer。
- ✅ ssoCode 30s + 一次性消费，泄露窗口极小。
- ✅ SSO 不再依赖公开的 AES 密钥（`/user/token` 移除 AES 解密）。
- ✅ 同步 XHR → 异步，消除主线程阻塞。

## 测试策略（TDD）

参照 `src/test/java/com/liuliupi/aop/LoginCheckAspectTest.java`：

- `UserController.ssoCode`：需登录（未登录被 `@LoginCheck` 拦截）、生成成功、缓存写入、TTL=30s。
- `UserServiceImpl.token`（ssoCode 换 token）：
  - 正确 ssoCode → 返回 UserVO + accessToken
  - ssoCode 已被使用（一次性） → "授权码无效或已过期"
  - ssoCode 过期 → 同上
  - 主站 token 已过期（`TOKEN+userId` 为空） → "登录已过期，请重新登录"
- 前端手动验证清单：主站→IM 跳转成功建立登录态、code 复用失败、主站掉线后跳转失败回主站。

## 边界情况

| 场景 | 处理 |
|---|---|
| 用户复制 IM 站 URL（含 ssoCode）稍后访问 | code 已过期/已用 → 跳回主站重新发起 |
| 主站用户点「进入 IM」但自身 token 已过期 | `/user/ssoCode` 被 `@LoginCheck` 拦截（code 300）→ 前端跳登录 |
| ssoCode 被中间人从 URL 截获 | 30s 内且一次性，最多被使用一次；token 本身未暴露 |
| 多次点击「进入 IM」 | 每次生成独立 ssoCode，互不影响 |

## 范围外（本次不做）

- `CRYPOTJS_KEY` 在密码传输等其他场景的继续使用（仅移除 SSO 链路对它的依赖）。
- 共享 Cookie / postMessage 等其他 SSO 方案。
- Redis 迁移、MD5→BCrypt 等其他架构问题。

## 发布要求

主站 + IM 站 + 后端**同步发布**。`/user/token` 语义从 `userToken`（AES 密文）变为 `ssoCode`（授权码），旧入口直接废弃，无灰度兼容。

## 涉及文件清单

**后端（liuliupi-server）：**
- `constants/CommonConst.java` — 新增 `SSO_CODE`、`SSO_CODE_EXPIRE`
- `controller/UserController.java` — 新增 `POST /user/ssoCode`；`token()` 参数变更
- `service/UserService.java` — `token()` 接口签名变更
- `service/impl/UserServiceImpl.java` — `token()` 改为 ssoCode 换 token，删除 AES 解密
- `src/test/...` — 新增 ssoCode / token 单测

**主站前端（liuliupi-ui）：**
- `components/home.vue` — 跳转改用 ssoCode
- `components/user.vue` — 跳转改用 ssoCode

**IM 站前端（liuliupi-im-ui）：**
- `main.js` — 路由守卫改异步 + ssoCode

## 并发实现注意事项

本规格与 `2026-06-13-login-captcha-design.md` 是**两个独立规格**，可并发实现。但二者共享以下后端文件，并发修改时需注意合并冲突：

- `controller/UserController.java`（本规格改 `token` + 新增 `ssoCode`；验证码规格改 `login` + 新增 `captcha`，方法级不重叠）
- `service/impl/UserServiceImpl.java`（本规格改 `token`；验证码规格改 `login`，方法级不重叠）
- `constants/CommonConst.java`（两边都新增常量，追加即可）

建议：后端改动由同一序列/worktree 完成，或并发后由人工合并这三个文件。主站/IM 站前端改动与验证码规格完全无交集，可安全并发。
