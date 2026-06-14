# 统一用户 Token 体系实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 合并前后台双 Token 体系为单一 Token，用户登录一次即可访问前台和后台。

**架构：** 所有用户登录后只发一个 Token（`ACCESS_TOKEN + UUID`），权限判断完全依靠 `User.userType` 字段（0=站长, 1=管理员, 2=普通用户）。前端统一使用 `currentUser` 存储用户信息，通过 `userType` 控制后台入口显示。

**技术栈：** 
- 后端：Java 17, Spring Boot, MyBatis-Plus
- 前端：Vue 2 (liuliupi-ui), Vue 3 (liuliupi-im-ui)

---

## 文件结构

### 后端（liuliupi-server）

| 文件路径 | 职责 | 变更类型 |
|---------|------|---------|
| `src/main/java/com/liuliupi/constants/CommonConst.java` | 常量定义 | 修改：合并 Token 常量，旧常量标记 @Deprecated |
| `src/main/java/com/liuliupi/vo/UserVO.java` | 用户视图对象 | 修改：增加 userType 字段 |
| `src/main/java/com/liuliupi/service/UserService.java` | 用户服务接口 | 修改：login() 去掉 isAdmin 参数 |
| `src/main/java/com/liuliupi/service/impl/UserServiceImpl.java` | 用户服务实现 | 修改：login() 统一 Token 生成逻辑 |
| `src/main/java/com/liuliupi/controller/UserController.java` | 用户控制器 | 修改：login() 去掉 isAdmin 参数 |
| `src/main/java/com/liuliupi/aop/LoginCheckAspect.java` | 登录检查切面 | 修改：简化权限判断逻辑 |
| `src/main/java/com/liuliupi/controller/AdminUserController.java` | 管理员控制器 | 修改：logout() 简化 |

### 前端（liuliupi-ui）

| 文件路径 | 职责 | 变更类型 |
|---------|------|---------|
| `src/store/index.js` | Vuex Store | 修改：移除 currentAdmin，统一使用 currentUser |
| `src/utils/request.js` | HTTP 请求封装 | 修改：移除 isAdmin 参数，统一使用 userToken |
| `src/router/index.js` | 路由配置 | 修改：路由守卫改为基于 userType |
| `src/components/admin/verify.vue` | 后台登录页 | 修改：去掉 isAdmin 参数，使用统一登录 |
| `src/components/user.vue` | 前台登录页 | 无需修改 |
| `src/components/home.vue` | 首页/导航栏 | 修改：根据 userType 显示"后台管理"入口 |
| `src/components/admin/common/myHeader.vue` | 后台头部 | 修改：使用 currentUser |
| `src/components/admin/common/sidebar.vue` | 后台侧边栏 | 修改：使用 currentUser |
| `src/components/common/uploadPicture.vue` | 图片上传组件 | 修改：移除 isAdmin prop |
| `src/components/admin/postEdit.vue` | 文章编辑 | 修改：移除 isAdmin prop |
| `src/components/admin/webEdit.vue` | 网站编辑 | 修改：移除 isAdmin prop |
| `src/components/admin/resourceList.vue` | 资源列表 | 修改：移除 isAdmin prop |
| `src/components/admin/resourcePathList.vue` | 资源路径列表 | 修改：移除 isAdmin prop |
| `src/components/admin/commentList.vue` | 评论列表 | 修改：使用 currentUser |
| `src/components/admin/postList.vue` | 文章列表 | 修改：使用 currentUser |
| `src/components/admin/userList.vue` | 用户列表 | 修改：使用 currentUser |
| `src/utils/common.js` | 工具函数 | 修改：saveResource 移除 isAdmin 参数 |

### IM 前端（liuliupi-im-ui）

IM 前端的 `request.js` 已经不使用 `isAdmin` 参数，无需修改。

---

## 任务列表

### 任务 1：后端 - CommonConst 常量合并

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/constants/CommonConst.java:14-30`

- [ ] **步骤 1：添加新常量，标记旧常量为 @Deprecated**

在 `CommonConst.java` 中，将原有的 6 个 Token 相关常量合并为 3 个：

```java
// 第 14-30 行修改为：

/**
 * 根据用户ID获取Token（统一版）
 */
public static final String TOKEN = "token_";

/**
 * 根据用户ID获取Token续期间隔（统一版）
 */
public static final String TOKEN_INTERVAL_KEY = "token_interval_";

/**
 * Token 前缀（统一版）
 */
public static final String ACCESS_TOKEN = "access_token_";

// === 以下为废弃常量，保留以避免编译报错 ===

/**
 * @deprecated 使用 {@link #TOKEN} 替代
 */
@Deprecated
public static final String USER_TOKEN = "user_token_";

/**
 * @deprecated 使用 {@link #TOKEN} 替代
 */
@Deprecated
public static final String ADMIN_TOKEN = "admin_token_";

/**
 * @deprecated 使用 {@link #TOKEN_INTERVAL_KEY} 替代
 */
@Deprecated
public static final String USER_TOKEN_INTERVAL = "user_token_interval_";

/**
 * @deprecated 使用 {@link #TOKEN_INTERVAL_KEY} 替代
 */
@Deprecated
public static final String ADMIN_TOKEN_INTERVAL = "admin_token_interval_";

/**
 * @deprecated 使用 {@link #ACCESS_TOKEN} 替代
 */
@Deprecated
public static final String USER_ACCESS_TOKEN = "user_access_token_";

/**
 * @deprecated 使用 {@link #ACCESS_TOKEN} 替代
 */
@Deprecated
public static final String ADMIN_ACCESS_TOKEN = "admin_access_token_";
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS（旧常量被其他地方引用，但标记 @Deprecated 后不会报错）

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/constants/CommonConst.java
git commit -m "refactor(server): 合并 Token 常量，旧常量标记 @Deprecated"
```

---

### 任务 2：后端 - UserVO 增加 userType 字段

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/vo/UserVO.java:43-45`

- [ ] **步骤 1：添加 userType 字段**

在 `UserVO.java` 的 `isBoss` 字段后添加：

```java
// 第 43 行后添加
private Boolean isBoss = false;

private Integer userType;

private String accessToken;
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/vo/UserVO.java
git commit -m "feat(server): UserVO 增加 userType 字段"
```

---

### 任务 3：后端 - UserService 接口签名变更

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/UserService.java:29`

- [ ] **步骤 1：修改 login 方法签名**

将 `UserService.java` 第 29 行：

```java
PoetryResult<UserVO> login(String account, String password, Boolean isAdmin);
```

修改为：

```java
PoetryResult<UserVO> login(String account, String password);
```

- [ ] **步骤 2：更新 Javadoc 注释**

将第 22-28 行的注释：

```java
/**
 * 用户名、邮箱、手机号/密码登录
 *
 * @param account
 * @param password
 * @return
 */
```

修改为：

```java
/**
 * 用户名、邮箱、手机号/密码登录
 * 统一 Token 体系，不再区分前台/后台登录
 *
 * @param account 账号（用户名/邮箱/手机号）
 * @param password 密码（加密后）
 * @return 用户信息（含 Token）
 */
```

- [ ] **步骤 3：验证编译（预期失败）**

运行：`cd liuliupi-server && mvn compile -q`
预期：编译失败，因为 `UserServiceImpl.java` 和 `UserController.java` 还未修改

- [ ] **步骤 4：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/service/UserService.java
git commit -m "refactor(server): UserService.login() 去掉 isAdmin 参数"
```

---

### 任务 4：后端 - UserServiceImpl.login() 统一 Token 逻辑

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java:76-139`

- [ ] **步骤 1：重写 login() 方法**

将 `UserServiceImpl.java` 第 76-139 行的 `login()` 方法替换为：

```java
@Override
public PoetryResult<UserVO> login(String account, String password) {
    password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

    User one = lambdaQuery().and(wrapper -> wrapper
                    .eq(User::getUsername, account)
                    .or()
                    .eq(User::getEmail, account)
                    .or()
                    .eq(User::getPhoneNumber, account))
            .eq(User::getPassword, DigestUtils.md5DigestAsHex(password.getBytes()))
            .one();

    if (one == null) {
        return PoetryResult.fail("账号/密码错误，请重新输入！");
    }

    if (!one.getUserStatus()) {
        return PoetryResult.fail("账号被冻结！");
    }

    // 统一 Token 体系：检查是否已有 Token
    String accessToken = "";
    if (PoetryCache.get(CommonConst.TOKEN + one.getId()) != null) {
        accessToken = (String) PoetryCache.get(CommonConst.TOKEN + one.getId());
    }

    // 如果没有 Token，生成新的
    if (!StringUtils.hasText(accessToken)) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        accessToken = CommonConst.ACCESS_TOKEN + uuid;
        PoetryCache.put(accessToken, one, CommonConst.TOKEN_EXPIRE);
        PoetryCache.put(CommonConst.TOKEN + one.getId(), accessToken, CommonConst.TOKEN_EXPIRE);
    }

    // 构建返回的 UserVO
    UserVO userVO = new UserVO();
    BeanUtils.copyProperties(one, userVO);
    userVO.setPassword(null);
    userVO.setUserType(one.getUserType());
    
    // userType == 0 时为站长（Boss）
    if (one.getUserType() == PoetryEnum.USER_TYPE_ADMIN.getCode()) {
        userVO.setIsBoss(true);
    }
    
    userVO.setAccessToken(accessToken);
    return PoetryResult.success(userVO);
}
```

- [ ] **步骤 2：验证编译（预期失败）**

运行：`cd liuliupi-server && mvn compile -q`
预期：编译失败，因为 `UserController.java` 还未修改

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java
git commit -m "refactor(server): UserServiceImpl.login() 统一 Token 生成逻辑"
```

---

### 任务 5：后端 - UserController.login() 去掉 isAdmin 参数

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java:46-51`

- [ ] **步骤 1：修改 login 方法**

将 `UserController.java` 第 46-51 行：

```java
@PostMapping("/login")
public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                  @RequestParam("password") String password,
                                  @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {
    return userService.login(account, password, isAdmin);
}
```

修改为：

```java
/**
 * 用户名、邮箱、手机号/密码登录
 * 统一 Token 体系，不再区分前台/后台
 */
@PostMapping("/login")
public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                  @RequestParam("password") String password) {
    return userService.login(account, password);
}
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/controller/UserController.java
git commit -m "refactor(server): UserController.login() 去掉 isAdmin 参数"
```

---

### 任务 6：后端 - LoginCheckAspect 简化权限判断

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/aop/LoginCheckAspect.java:27-88`

- [ ] **步骤 1：重写 around() 方法**

将 `LoginCheckAspect.java` 第 27-88 行的 `around()` 方法替换为：

```java
@Around("@annotation(loginCheck)")
public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
    String token = PoetryUtil.getToken();
    if (!StringUtils.hasText(token)) {
        throw new PoetryLoginException(CodeMsg.NOT_LOGIN.getMsg());
    }

    User user = (User) PoetryCache.get(token);

    if (user == null) {
        throw new PoetryLoginException(CodeMsg.LOGIN_EXPIRED.getMsg());
    }

    // 统一权限判断：直接使用 userType
    // loginCheck.value() 表示所需的最低权限级别（0=站长, 1=管理员, 2=普通用户）
    // userType 值越小权限越高：0 < 1 < 2
    if (loginCheck.value() < user.getUserType()) {
        throw new PoetryRuntimeException("权限不足！");
    }

    // 重置过期时间（统一使用一套 key）
    String userId = user.getId().toString();
    boolean needRefresh = PoetryCache.get(CommonConst.TOKEN_INTERVAL_KEY + userId) == null;

    if (needRefresh) {
        synchronized (userId.intern()) {
            // 双重检查
            if (PoetryCache.get(CommonConst.TOKEN_INTERVAL_KEY + userId) == null) {
                PoetryCache.put(token, user, CommonConst.TOKEN_EXPIRE);
                PoetryCache.put(CommonConst.TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                PoetryCache.put(CommonConst.TOKEN_INTERVAL_KEY + userId, token, CommonConst.TOKEN_INTERVAL);
            }
        }
    }
    
    return joinPoint.proceed();
}
```

- [ ] **步骤 2：移除未使用的 import**

检查文件顶部的 import，移除不再使用的：
- `com.liuliupi.enums.PoetryEnum`（如果不再直接引用）

- [ ] **步骤 3：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 4：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/aop/LoginCheckAspect.java
git commit -m "refactor(server): LoginCheckAspect 简化为基于 userType 判断"
```

---

### 任务 7：后端 - AdminUserController.logout() 简化

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/controller/AdminUserController.java:101-118`

- [ ] **步骤 1：简化 logout() 方法**

将 `AdminUserController.java` 第 101-118 行的 `logout()` 方法替换为：

```java
/**
 * 登出用户（清除 Token + 断开 WebSocket）
 * 统一 Token 体系后，只需清除一套 Token
 */
private void logout(Integer userId) {
    // 清除统一 Token
    if (PoetryCache.get(CommonConst.TOKEN + userId) != null) {
        String token = (String) PoetryCache.get(CommonConst.TOKEN + userId);
        PoetryCache.remove(CommonConst.TOKEN + userId);
        PoetryCache.remove(token);
    }
    
    // 断开 WebSocket 连接
    TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
    if (tioWebsocketStarter != null) {
        Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
    }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/controller/AdminUserController.java
git commit -m "refactor(server): AdminUserController.logout() 简化为统一 Token"
```

---

### 任务 8：后端 - UserServiceImpl.exit() 简化（补充）

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java:141-156`

- [ ] **步骤 1：简化 exit() 方法**

将 `UserServiceImpl.java` 第 141-156 行的 `exit()` 方法替换为：

```java
@Override
public PoetryResult exit() {
    String token = PoetryUtil.getToken();
    Integer userId = PoetryUtil.getUserId();
    
    // 统一 Token 体系：只需清除一套 Token
    PoetryCache.remove(CommonConst.TOKEN + userId);
    PoetryCache.remove(token);
    
    // 断开 WebSocket 连接
    TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
    if (tioWebsocketStarter != null) {
        Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
    }
    
    return PoetryResult.success();
}
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java
git commit -m "refactor(server): UserServiceImpl.exit() 简化为统一 Token"
```

---

### 任务 9：后端 - UserServiceImpl.regist() Token 生成统一（补充）

**文件：**
- 修改：`liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java:216-220`

- [ ] **步骤 1：修改 regist() 中的 Token 生成逻辑**

将 `UserServiceImpl.java` 第 216-220 行：

```java
User one = lambdaQuery().eq(User::getId, u.getId()).one();

String userToken = CommonConst.USER_ACCESS_TOKEN + UUID.randomUUID().toString().replaceAll("-", "");
PoetryCache.put(userToken, one, CommonConst.TOKEN_EXPIRE);
PoetryCache.put(CommonConst.USER_TOKEN + one.getId(), userToken, CommonConst.TOKEN_EXPIRE);
```

修改为：

```java
User one = lambdaQuery().eq(User::getId, u.getId()).one();

// 统一 Token 体系
String userToken = CommonConst.ACCESS_TOKEN + UUID.randomUUID().toString().replaceAll("-", "");
PoetryCache.put(userToken, one, CommonConst.TOKEN_EXPIRE);
PoetryCache.put(CommonConst.TOKEN + one.getId(), userToken, CommonConst.TOKEN_EXPIRE);
```

- [ ] **步骤 2：验证编译**

运行：`cd liuliupi-server && mvn compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-server/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java
git commit -m "refactor(server): UserServiceImpl.regist() 使用统一 Token"
```

---

### 任务 10：前端 - Store 统一 currentUser

**文件：**
- 修改：`liuliupi-ui/src/store/index.js:10-11, 60-63`

- [ ] **步骤 1：移除 currentAdmin state**

将 `store/index.js` 第 10-11 行：

```javascript
currentUser: JSON.parse(localStorage.getItem("currentUser") || '{}'),
currentAdmin: JSON.parse(localStorage.getItem("currentAdmin") || '{}'),
```

修改为：

```javascript
currentUser: JSON.parse(localStorage.getItem("currentUser") || '{}'),
```

- [ ] **步骤 2：移除 loadCurrentAdmin mutation**

将第 60-63 行：

```javascript
loadCurrentAdmin(state, user) {
  state.currentAdmin = user;
  localStorage.setItem("currentAdmin", JSON.stringify(user));
},
```

删除这 4 行。

- [ ] **步骤 3：验证前端构建**

运行：`cd liuliupi-ui && npm run build`
预期：可能有警告（引用 currentAdmin 的地方会报错），先提交，后续任务修复

- [ ] **步骤 4：Commit**

```bash
git add liuliupi-ui/src/store/index.js
git commit -m "refactor(ui): Store 移除 currentAdmin，统一使用 currentUser"
```

---

### 任务 11：前端 - request.js 简化 Token 处理

**文件：**
- 修改：`liuliupi-ui/src/utils/request.js:26-31, 49-59, 73-79, 93-105`

- [ ] **步骤 1：修改响应拦截器中的登出逻辑**

将第 26-31 行：

```javascript
store.commit("loadCurrentUser", {});
localStorage.removeItem("userToken");
store.commit("loadCurrentAdmin", {});
localStorage.removeItem("adminToken");
window.location.href = constant.webURL + "/user";
```

修改为：

```javascript
store.commit("loadCurrentUser", {});
localStorage.removeItem("userToken");
window.location.href = constant.webURL + "/user";
```

- [ ] **步骤 2：简化 post 方法**

将第 49-59 行：

```javascript
post(url, params = {}, isAdmin = false, json = true) {
  let config;
  if (isAdmin) {
    config = {
      headers: {"Authorization": localStorage.getItem("adminToken")}
    };
  } else {
    config = {
      headers: {"Authorization": localStorage.getItem("userToken")}
    };
  }
```

修改为：

```javascript
post(url, params = {}, json = true) {
  let config = {
    headers: {"Authorization": localStorage.getItem("userToken")}
  };
```

- [ ] **步骤 3：简化 get 方法**

将第 73-79 行：

```javascript
get(url, params = {}, isAdmin = false) {
  let headers;
  if (isAdmin) {
    headers = {"Authorization": localStorage.getItem("adminToken")};
  } else {
    headers = {"Authorization": localStorage.getItem("userToken")};
  }
```

修改为：

```javascript
get(url, params = {}) {
  let headers = {"Authorization": localStorage.getItem("userToken")};
```

- [ ] **步骤 4：简化 upload 方法**

将第 93-105 行：

```javascript
upload(url, param, isAdmin = false, option) {
  let config;
  if (isAdmin) {
    config = {
      headers: {"Authorization": localStorage.getItem("adminToken"), "Content-Type": "multipart/form-data"},
      timeout: 60000
    };
  } else {
    config = {
      headers: {"Authorization": localStorage.getItem("userToken"), "Content-Type": "multipart/form-data"},
      timeout: 60000
    };
  }
```

修改为：

```javascript
upload(url, param, option) {
  let config = {
    headers: {"Authorization": localStorage.getItem("userToken"), "Content-Type": "multipart/form-data"},
    timeout: 60000
  };
```

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/utils/request.js
git commit -m "refactor(ui): request.js 移除 isAdmin 参数，统一使用 userToken"
```

---

### 任务 12：前端 - router 路由守卫改为 userType

**文件：**
- 修改：`liuliupi-ui/src/router/index.js:135-148`

- [ ] **步骤 1：修改路由守卫逻辑**

将第 135-148 行：

```javascript
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!Boolean(localStorage.getItem("adminToken"))) {
      next({
        path: '/verify',
        query: {redirect: to.fullPath}
      });
    } else {
      next();
    }
  } else {
    next();
  }
})
```

修改为：

```javascript
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 统一 Token 体系：检查 currentUser 是否存在且 userType <= 1（站长或管理员）
    const currentUser = JSON.parse(localStorage.getItem("currentUser") || '{}');
    const hasPermission = currentUser && currentUser.userType !== undefined && currentUser.userType <= 1;
    
    if (!hasPermission) {
      next({
        path: '/verify',
        query: {redirect: to.fullPath}
      });
    } else {
      next();
    }
  } else {
    next();
  }
})
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/router/index.js
git commit -m "refactor(ui): 路由守卫改为基于 userType 判断后台权限"
```

---

### 任务 13：前端 - verify.vue 后台登录页改造

**文件：**
- 修改：`liuliupi-ui/src/components/admin/verify.vue:61-74`

- [ ] **步骤 1：修改登录请求**

将第 61-74 行：

```javascript
let user = {
  account: this.account.trim(),
  password: this.$common.encrypt(this.password.trim()),
  isAdmin: true
};

this.$http.post(this.$constant.baseURL + "/user/login", user, true, false)
  .then((res) => {
    if (!this.$common.isEmpty(res.data)) {
      localStorage.setItem("adminToken", res.data.accessToken);
      this.$store.commit("loadCurrentAdmin", res.data);
      this.account = "";
      this.password = "";
      this.$router.push({path: this.redirect});
    }
  })
```

修改为：

```javascript
let user = {
  account: this.account.trim(),
  password: this.$common.encrypt(this.password.trim())
};

this.$http.post(this.$constant.baseURL + "/user/login", user, false)
  .then((res) => {
    if (!this.$common.isEmpty(res.data)) {
      // 统一 Token 体系：使用 currentUser 和 userToken
      localStorage.setItem("userToken", res.data.accessToken);
      this.$store.commit("loadCurrentUser", res.data);
      this.account = "";
      this.password = "";
      this.$router.push({path: this.redirect});
    }
  })
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/verify.vue
git commit -m "refactor(ui): verify.vue 使用统一登录，移除 isAdmin 参数"
```

---

### 任务 14：前端 - home.vue 导航栏显示后台入口

**文件：**
- 修改：`liuliupi-ui/src/components/home.vue:82-88`

- [ ] **步骤 1：根据 userType 显示/隐藏后台入口**

找到 home.vue 中的后台入口（约第 82-88 行），当前代码大致为：

```html
<!-- 后台 -->
<li>
  <div class="my-menu" @click="goAdmin()">
    💻️ <span>后台</span>
  </div>
</li>
```

修改为（添加 `v-if` 条件）：

```html
<!-- 后台：仅站长和管理员可见 -->
<li v-if="!$common.isEmpty($store.state.currentUser) && $store.state.currentUser.userType <= 1">
  <div class="my-menu" @click="goAdmin()">
    💻️ <span>后台</span>
  </div>
</li>
```

- [ ] **步骤 2：同样修改移动端菜单中的后台入口**

找到移动端菜单中的后台入口（约第 233-238 行），当前代码大致为：

```html
<!-- 后台 -->
<li @click="goAdmin()">
  <div>
    💻️ <span>后台</span>
  </div>
</li>
```

修改为：

```html
<!-- 后台：仅站长和管理员可见 -->
<li @click="goAdmin()" v-if="!$common.isEmpty($store.state.currentUser) && $store.state.currentUser.userType <= 1">
  <div>
    💻️ <span>后台</span>
  </div>
</li>
```

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-ui/src/components/home.vue
git commit -m "feat(ui): home.vue 导航栏根据 userType 显示后台入口"
```

---

### 任务 15：前端 - admin/myHeader.vue 使用 currentUser

**文件：**
- 修改：`liuliupi-ui/src/components/admin/common/myHeader.vue:30, 60-72`

- [ ] **步骤 1：修改头像引用**

将第 30 行：

```vue
:src="$store.state.currentAdmin.avatar">
```

修改为：

```vue
:src="$store.state.currentUser.avatar">
```

- [ ] **步骤 2：修改 logout 方法**

将第 60-72 行：

```javascript
logout() {
  this.$http.get(this.$constant.baseURL + "/user/logout", {}, true)
    .then((res) => {
    })
    .catch((error) => {
      this.$message({
        message: error.message,
        type: "error"
      });
    });
  this.$store.commit("loadCurrentAdmin", {});
  localStorage.removeItem("adminToken");
  this.$router.push({path: '/'});
}
```

修改为：

```javascript
logout() {
  this.$http.get(this.$constant.baseURL + "/user/logout")
    .then((res) => {
    })
    .catch((error) => {
      this.$message({
        message: error.message,
        type: "error"
      });
    });
  this.$store.commit("loadCurrentUser", {});
  localStorage.removeItem("userToken");
  this.$router.push({path: '/'});
}
```

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-ui/src/components/admin/common/myHeader.vue
git commit -m "refactor(ui): myHeader.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 16：前端 - admin/sidebar.vue 使用 currentUser

**文件：**
- 修改：`liuliupi-ui/src/components/admin/common/sidebar.vue:54`

- [ ] **步骤 1：修改 isBoss 判断**

将第 54 行：

```javascript
isBoss: this.$store.state.currentAdmin.isBoss,
```

修改为：

```javascript
isBoss: this.$store.state.currentUser.isBoss,
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/common/sidebar.vue
git commit -m "refactor(ui): sidebar.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 17：前端 - admin/commentList.vue 使用 currentUser

**文件：**
- 修改：`liuliupi-ui/src/components/admin/commentList.vue:47`

- [ ] **步骤 1：修改 isBoss 判断**

将第 47 行：

```javascript
isBoss: this.$store.state.currentAdmin.isBoss,
```

修改为：

```javascript
isBoss: this.$store.state.currentUser.isBoss,
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/commentList.vue
git commit -m "refactor(ui): commentList.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 18：前端 - admin/postList.vue 使用 currentUser

**文件：**
- 修改：`liuliupi-ui/src/components/admin/postList.vue:97`

- [ ] **步骤 1：修改 isBoss 判断**

将第 97 行：

```javascript
isBoss: this.$store.state.currentAdmin.isBoss,
```

修改为：

```javascript
isBoss: this.$store.state.currentUser.isBoss,
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/postList.vue
git commit -m "refactor(ui): postList.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 19：前端 - admin/userList.vue 使用 currentUser

**文件：**
- 修改：`liuliupi-ui/src/components/admin/userList.vue:35`

- [ ] **步骤 1：修改 admin ID 判断**

将第 35 行：

```vue
<el-switch v-if="scope.row.id !== $store.state.currentAdmin.id" @click.native="changeUserStatus(scope.row)" v-model="scope.row.userStatus"></el-switch>
```

修改为：

```vue
<el-switch v-if="scope.row.id !== $store.state.currentUser.id" @click.native="changeUserStatus(scope.row)" v-model="scope.row.userStatus"></el-switch>
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/userList.vue
git commit -m "refactor(ui): userList.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 20：前端 - uploadPicture.vue 移除 isAdmin prop

**文件：**
- 修改：`liuliupi-ui/src/components/common/uploadPicture.vue:56, 112, 123, 127-128, 160`

- [ ] **步骤 1：移除 isAdmin prop**

找到 props 定义（约第 56 行）：

```javascript
isAdmin: {
  type: Boolean,
  default: false
},
```

删除这 4 行。

- [ ] **步骤 2：修改 upload 方法中的 key 生成**

将第 112 行：

```javascript
let key = this.prefix + "/" + (!this.$common.isEmpty(this.$store.state.currentUser.username) ? (this.$store.state.currentUser.username.replace(/[^a-zA-Z]/g, '') + this.$store.state.currentUser.id) : (this.$store.state.currentAdmin.username.replace(/[^a-zA-Z]/g, '') + this.$store.state.currentAdmin.id)) + new Date().getTime() + Math.floor(Math.random() * 1000) + suffix;
```

修改为：

```javascript
let key = this.prefix + "/" + this.$store.state.currentUser.username.replace(/[^a-zA-Z]/g, '') + this.$store.state.currentUser.id + new Date().getTime() + Math.floor(Math.random() * 1000) + suffix;
```

- [ ] **步骤 3：修改 upload 方法调用**

将第 123 行：

```javascript
return this.$http.upload(this.$constant.baseURL + "/resource/upload", fd, this.isAdmin, options);
```

修改为：

```javascript
return this.$http.upload(this.$constant.baseURL + "/resource/upload", fd, options);
```

- [ ] **步骤 4：修改 XHR 上传逻辑**

将第 127-128 行：

```javascript
if (this.isAdmin) {
  xhr.setRequestHeader("Authorization", localStorage.getItem("adminToken"));
```

修改为：

```javascript
// 统一使用 userToken
xhr.setRequestHeader("Authorization", localStorage.getItem("userToken"));
```

同时删除原来的 `else` 分支（如果有）。

- [ ] **步骤 5：修改 saveResource 调用**

将第 160 行：

```javascript
this.$common.saveResource(this, this.prefix, url, file.size, file.raw.type, file.name, "qiniu", this.isAdmin);
```

修改为：

```javascript
this.$common.saveResource(this, this.prefix, url, file.size, file.raw.type, file.name, "qiniu");
```

- [ ] **步骤 6：Commit**

```bash
git add liuliupi-ui/src/components/common/uploadPicture.vue
git commit -m "refactor(ui): uploadPicture.vue 移除 isAdmin prop，统一使用 userToken"
```

---

### 任务 21：前端 - 后台管理组件移除 isAdmin prop 传递

**文件：**
- 修改：`liuliupi-ui/src/components/admin/postEdit.vue:74`
- 修改：`liuliupi-ui/src/components/admin/webEdit.vue:42, 57, 187, 236`
- 修改：`liuliupi-ui/src/components/admin/resourceList.vue:103`
- 修改：`liuliupi-ui/src/components/admin/resourcePathList.vue:74, 87`

- [ ] **步骤 1：postEdit.vue 移除 :isAdmin="true"**

找到第 74 行（或附近），将：

```vue
<uploadPicture :isAdmin="true" :prefix="'articleCover'" ...>
```

修改为：

```vue
<uploadPicture prefix="articleCover" ...>
```

- [ ] **步骤 2：webEdit.vue 移除所有 :isAdmin="true"**

找到第 42、57、187、236 行（或附近），将所有 `:isAdmin="true"` 删除。

- [ ] **步骤 3：resourceList.vue 移除 :isAdmin="true"**

找到第 103 行（或附近），将 `:isAdmin="true"` 删除。

- [ ] **步骤 4：resourcePathList.vue 移除 :isAdmin="true"**

找到第 74、87 行（或附近），将所有 `:isAdmin="true"` 删除。

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/postEdit.vue
git add liuliupi-ui/src/components/admin/webEdit.vue
git add liuliupi-ui/src/components/admin/resourceList.vue
git add liuliupi-ui/src/components/admin/resourcePathList.vue
git commit -m "refactor(ui): 后台组件移除 uploadPicture 的 isAdmin prop"
```

---

### 任务 22：前端 - common.js saveResource 移除 isAdmin 参数

**文件：**
- 修改：`liuliupi-ui/src/utils/common.js:209, 219`

- [ ] **步骤 1：修改 saveResource 方法签名**

将第 209 行：

```javascript
saveResource(that, type, path, size, mimeType, originalName, storeType, isAdmin = false) {
```

修改为：

```javascript
saveResource(that, type, path, size, mimeType, originalName, storeType) {
```

- [ ] **步骤 2：修改 post 调用**

将第 219 行：

```javascript
that.$http.post(that.$constant.baseURL + "/resource/saveResource", resource, isAdmin)
```

修改为：

```javascript
that.$http.post(that.$constant.baseURL + "/resource/saveResource", resource)
```

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-ui/src/utils/common.js
git commit -m "refactor(ui): common.js saveResource 移除 isAdmin 参数"
```

---

### 任务 23：前端 - admin/postEdit.vue 修复 currentAdmin 引用

**文件：**
- 修改：`liuliupi-ui/src/components/admin/postEdit.vue:188`

- [ ] **步骤 1：修改 currentAdmin 引用**

将第 188 行：

```javascript
let key = "articlePicture" + "/" + this.$store.state.currentAdmin.username.replace(/[^a-zA-Z]/g, '') + this.$store.state.currentAdmin.id + ...
```

修改为：

```javascript
let key = "articlePicture" + "/" + this.$store.state.currentUser.username.replace(/[^a-zA-Z]/g, '') + this.$store.state.currentUser.id + ...
```

- [ ] **步骤 2：Commit**

```bash
git add liuliupi-ui/src/components/admin/postEdit.vue
git commit -m "refactor(ui): postEdit.vue 使用 currentUser 替换 currentAdmin"
```

---

### 任务 24：验证后端编译和测试

- [ ] **步骤 1：后端完整编译**

运行：`cd liuliupi-server && mvn clean compile`
预期：BUILD SUCCESS

- [ ] **步骤 2：后端运行测试（如果有）**

运行：`cd liuliupi-server && mvn test`
预期：Tests run: X, Failures: 0, Errors: 0（或无测试）

- [ ] **步骤 3：Commit（如有修复）**

---

### 任务 25：验证前端构建

- [ ] **步骤 1：前端完整构建**

运行：`cd liuliupi-ui && npm run build`
预期：构建成功，无错误

- [ ] **步骤 2：检查是否还有 currentAdmin 引用**

运行：`grep -r "currentAdmin" liuliupi-ui/src/`
预期：无输出（或仅注释中）

- [ ] **步骤 3：检查是否还有 adminToken 引用**

运行：`grep -r "adminToken" liuliupi-ui/src/`
预期：无输出（或仅注释中）

- [ ] **步骤 4：Commit（如有修复）**

---

### 任务 26：IM 前端检查

- [ ] **步骤 1：检查 IM 前端是否有 isAdmin 参数**

运行：`grep -r "isAdmin" liuliupi-im-ui/src/`
预期：无输出（IM 前端已经是统一 Token）

- [ ] **步骤 2：检查 IM 前端是否使用 adminToken**

运行：`grep -r "adminToken" liuliupi-im-ui/src/`
预期：无输出

---

### 任务 27：最终验证和清理

- [ ] **步骤 1：全局检查旧常量引用**

运行：`grep -rn "USER_ACCESS_TOKEN\|ADMIN_ACCESS_TOKEN\|USER_TOKEN\|ADMIN_TOKEN\|USER_TOKEN_INTERVAL\|ADMIN_TOKEN_INTERVAL" liuliupi-server/src/main/java/ --include="*.java"`
预期：仅 `CommonConst.java` 中有引用（标记为 @Deprecated 的常量定义）

- [ ] **步骤 2：检查前端 localStorage 清理**

确认以下 localStorage key 不再被使用：
- `adminToken`
- `currentAdmin`

运行：`grep -r "adminToken\|currentAdmin" liuliupi-ui/src/`
预期：无输出

- [ ] **步骤 3：最终 Commit**

```bash
git add .
git commit -m "chore: 统一用户 Token 体系改造完成"
```

---

## 自检清单

完成所有任务后，对照规格文档检查：

| 需求 | 对应任务 | 状态 |
|-----|---------|-----|
| 后端 Token 统一（ACCESS_TOKEN + UUID） | 任务 4, 9 | [ ] |
| 后端缓存 key 统一（TOKEN + userId） | 任务 4, 9 | [ ] |
| login() 去掉 isAdmin 参数 | 任务 3, 4, 5 | [ ] |
| LoginCheckAspect 改为 userType 判断 | 任务 6 | [ ] |
| UserVO 增加 userType 字段 | 任务 2 | [ ] |
| AdminUserController.logout() 简化 | 任务 7 | [ ] |
| UserServiceImpl.exit() 简化 | 任务 8 | [ ] |
| 前端 Store 统一 currentUser | 任务 10 | [ ] |
| 前端 request.js 简化 | 任务 11 | [ ] |
| 前端路由守卫改为 userType | 任务 12 | [ ] |
| 前端 verify.vue 统一登录 | 任务 13 | [ ] |
| 前端导航栏显示后台入口 | 任务 14 | [ ] |
| 前端 admin 组件使用 currentUser | 任务 15-19, 23 | [ ] |
| 前端 uploadPicture 简化 | 任务 20, 21 | [ ] |
| IM 前端检查 | 任务 26 | [ ] |

---

## 执行建议

1. **后端先行**：先完成后端所有任务（1-9），确保 API 可用
2. **前端跟进**：后端完成后，再进行前端改造（10-23）
3. **逐步验证**：每完成一个任务就验证编译/构建
4. **频繁 Commit**：每个任务独立 commit，便于回滚
5. **IM 前端**：最后检查 IM 前端，确认无需改动

**预计耗时：** 4-6 小时（包含验证和调试）
