# 统一用户体系设计：合并前后台 Token

> 日期：2026-06-12
> 状态：已批准

## 背景

当前系统对前端用户和后台管理员使用两套独立的 Token（`USER_ACCESS_TOKEN` 和 `ADMIN_ACCESS_TOKEN`）。同一个管理员用户如果需要同时访问前台和后台，必须登录两次，分别获取不同的 Token。这个设计不够合理。

## 目标

- 统一 Token 体系：一个用户只发一个 Token，登录一次即可
- 管理员登录后可直接访问后台，无需二次登录
- 前台和后台保持在同一个 SPA 中，通过路由切换
- 管理员在前台和后台之间可随时自由切换

## 方案

去掉 `USER_ACCESS_TOKEN` / `ADMIN_ACCESS_TOKEN` 的双轨制，所有用户登录后只发一个 Token。权限判断完全依靠 `User.userType` 字段（0=站长, 1=管理员, 2=普通用户）。

## 改造详情

### 1. 后端 Token 体系改造

#### `UserServiceImpl.login()` 简化

- **去掉** `isAdmin` 参数，只接收 `account` 和 `password`
- 所有用户登录统一生成一种 Token：`ACCESS_TOKEN + UUID`
- 缓存 key 统一为 `TOKEN + userId`
- `isBoss` 字段保留（`userType == 0` 时为 true）

#### `UserController.login()` 接口变更

```
改造前: POST /user/login?account=xxx&password=xxx&isAdmin=false
改造后: POST /user/login?account=xxx&password=xxx
```

#### `LoginCheckAspect` 简化

- 不再通过 Token 前缀（`USER_ACCESS_TOKEN` / `ADMIN_ACCESS_TOKEN`）区分身份
- 直接从缓存取出 User 对象，读取 `user.getUserType()` 判断权限
- 核心权限判断保留：`loginCheck.value() < user.getUserType()` → 权限不足
- Token 续期逻辑简化为只操作一套 key

#### `CommonConst` 常量合并

| 旧常量 | 新常量 | 说明 |
|---|---|---|
| `USER_ACCESS_TOKEN` + `ADMIN_ACCESS_TOKEN` | `ACCESS_TOKEN` | Token 前缀 |
| `USER_TOKEN` + `ADMIN_TOKEN` | `TOKEN` | 用户ID→Token 缓存 key |
| `USER_TOKEN_INTERVAL` + `ADMIN_TOKEN_INTERVAL` | `TOKEN_INTERVAL` | 续期间隔 key |

- 旧常量标记 `@Deprecated` 暂不删除，避免引用处编译报错

#### `AdminUserController.logout()` 简化

- 只清一套 Token（`TOKEN + userId`）+ 断 WebSocket

#### `UserVO` 增加字段

- 增加 `userType`（Integer）字段，登录时从 `User` 拷贝

### 2. 前端改造

#### 登录页统一

- 登录请求去掉 `isAdmin` 参数
- 登录成功后将完整 `UserVO`（含 `userType`、`isBoss`、`accessToken`）存入 Vuex Store

#### 路由守卫

- `/admin/*` 路由守卫逻辑：
  1. 检查 `store.state.currentUser` 是否存在（已登录）
  2. 检查 `store.state.currentUser.userType <= 1`（管理员或站长）
  3. 不满足 → 重定向到首页
- 前台路由（用户中心等）：只检查是否已登录

#### 导航栏

- 管理员/站长（`userType <= 1`）登录后，导航栏显示"后台管理"入口
- 普通用户看不到此入口

#### 后台管理面板

- `admin.vue` 布局不变（Header + Sidebar + RouterView）
- 去掉后台独立的登录页（如果有）

#### IM 模块（liuliupi-im-ui）

- 同步去掉登录接口中的 `isAdmin` 参数

### 3. 不改动的部分

- 注册逻辑
- 忘记密码 / 验证码
- IM 聊天模块（使用 `userId` 关联，不受 Token 改造影响）
- 后台管理接口的 `@LoginCheck` 注解（仅 AOP 内部逻辑简化）

### 4. 边界情况

| 场景 | 处理方式 |
|---|---|
| 管理员在线时被降级为普通用户 | `changeUserType()` 清除 Token + 断 WebSocket，下次请求被拦截 |
| 管理员在线时被冻结 | `changeUserStatus()` 清除 Token + 断 WebSocket |
| 同一用户多设备登录 | 保持现有逻辑：每个用户只存一个 Token，新登录覆盖旧的 |
| Token 过期 | 缓存取到 null 时抛 `LOGIN_EXPIRED`，前端跳转登录页 |
| 普通用户手动访问 `/admin` | 前端路由守卫拦截；后端 `@LoginCheck(0)` 也因 `userType=2` 拒绝 |
| 前台输入管理员账号登录 | 正常登录，导航栏显示后台入口 |

### 5. 涉及文件清单

**后端（liuliupi-server）：**
- `entity/User.java` — 无变更
- `vo/UserVO.java` — 增加 `userType` 字段
- `controller/UserController.java` — `login()` 去掉 `isAdmin` 参数
- `controller/AdminUserController.java` — `logout()` 简化
- `service/impl/UserServiceImpl.java` — `login()` 去掉 `isAdmin`，Token 合并
- `aop/LoginCheckAspect.java` — 去掉 Token 前缀判断，改为 `userType` 判断
- `constants/CommonConst.java` — 合并常量，旧常量 `@Deprecated`

**前端（liuliupi-ui）：**
- 登录相关组件 — 去掉 `isAdmin` 参数
- Vuex Store — 存储 `userType`
- 路由守卫 — 基于 `userType` 判断后台权限
- 导航栏组件 — 根据 `userType` 显示/隐藏"后台管理"入口

**IM 前端（liuliupi-im-ui）：**
- 登录相关逻辑 — 同步去掉 `isAdmin` 参数
