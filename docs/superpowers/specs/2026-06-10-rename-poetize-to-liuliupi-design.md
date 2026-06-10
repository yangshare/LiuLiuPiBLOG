# 工程名称更换设计：poetize → liuliupi

## 概述

将个人博客系统的工程名称从 "poetize" 彻底更换为 "liuliupi"。涉及目录名、Java 包名、数据库名、构建产物、业务文本、域名引用等所有层面。

## 映射表

| 维度 | 旧值 | 新值 |
|---|---|---|
| 顶级目录名 | `poetize-server`, `poetize-ui`, `poetize-im-ui`, `poetize_picture` | `liuliupi-server`, `liuliupi-ui`, `liuliupi-im-ui`, `liuliupi_picture` |
| Java 包名 | `com.ld.poetry` | `com.liuliupi` |
| MySQL 数据库名 | `poetize` | `liuliupi` |
| 构建产物名 | `poetize-server.jar` | `liuliupi-server.jar` |
| 域名 | `poetize.cn` | `yangshare.com` |
| 业务文本 | `POETIZE` | `LIULIUPI` |
| 前端字体名 | `poetize-font` | `liuliupi-font` |
| Favicon 文件名 | `poetize.jpg` | `liuliupi.jpg` |
| Docker 镜像/容器名 | `poetize-ui`, `poetize-server` | `liuliupi-ui`, `liuliupi-server` |
| 日志路径 | `/home/poetize/logs` | `/home/liuliupi/logs` |
| pom.xml 项目名 | `POETIZE - 最美博客` | `LIULIUPI` |
| LICENSE | `Copyright (c) 2022 POETIZE` | `Copyright (c) 2022 LIULIUPI` |
| IDE 运行配置 | `poetize_ui.xml`, `poetize_server.xml` | `liuliupi_ui.xml`, `liuliupi_server.xml` |

## 执行方案：自底向上分层执行

按依赖关系从最底层开始，逐层向上修改。每层完成后 commit 一次。

### Layer 1：Java 包名（最深层）

**目标：** 将 `com.ld.poetry` 改为 `com.liuliupi`。

**操作：**

1. 物理目录重命名：`com/ld/poetry/` → `com/liuliupi/`
2. 批量替换所有 `.java` 文件（约 80+ 个）中的：
   - `package com.ld.poetry` → `package com.liuliupi`
   - `import com.ld.poetry` → `import com.liuliupi`
3. 更新 `pom.xml` 中的 `<groupId>` 和 `<scanBasePackages>` 等配置
4. 检查并更新 `spring.factories`（EnvironmentPostProcessor 注册）
5. 检查 `mybatis-plus` 的 mapper scan 配置中的包名
6. 检查 `application.yml` 中可能的包名引用

**验证：** `mvn compile` 编译通过。

### Layer 2：配置与 SQL

**目标：** 更新后端所有配置文件、SQL 脚本和 Java 代码中的硬编码字符串。

**操作：**

#### 2.1 `application.yml`
- 数据库 URL：`jdbc:mysql://...:3306/poetize?...` → `jdbc:mysql://...:3306/liuliupi?...`

#### 2.2 `logback-spring.xml`
- `defaultValue="poetize"` → `defaultValue="liuliupi"`
- `defaultValue="/home/poetize/logs"` → `defaultValue="/home/liuliupi/logs"`

#### 2.3 Maven `pom.xml`
- 父 `pom.xml`：`<name>POETIZE - 最美博客</name>` → `<name>LIULIUPI</name>`
- 父 `pom.xml`：`<description>POETIZE - 最美博客</description>` → `<description>LIULIUPI</description>`
- 子 `pom.xml`：`<finalName>poetize-server</finalName>` → `<finalName>liuliupi-server</finalName>`

#### 2.4 后端 Dockerfile
- `ADD target/*.jar poetize-server.jar` → `ADD target/*.jar liuliupi-server.jar`
- `ENTRYPOINT ["java", "-jar", "poetize-server.jar"]` → `ENTRYPOINT ["java", "-jar", "liuliupi-server.jar"]`

#### 2.5 Java 代码中的硬编码
- `CustomEnvironmentPostProcessor.java`：`DATABASE = "poetize"` → `DATABASE = "liuliupi"`
- `CodeGenerator.java`：JDBC URL 中 `poetize` → `liuliupi`
- `ImConfigConst.java`：`PROTOCOL_NAME = "protocol_poetize"` → `PROTOCOL_NAME = "protocol_liuliupi"`
- `MailUtil.java`：`https://poetize.cn` → `https://yangshare.com`
- `MailSendUtil.java`（4 处）：`"POETIZE"` → `"LIULIUPI"`
- `ArticleServiceImpl.java`（2 处）：`"POETIZE"` → `"LIULIUPI"`
- `UserServiceImpl.java`（4 处）：`"POETIZE"` → `"LIULIUPI"`

#### 2.6 SQL 脚本 `poetry.sql`
- `CREATE DATABASE IF NOT EXISTS poetize` → `CREATE DATABASE IF NOT EXISTS liuliupi`
- 所有 `` `poetize`.`表名` `` → `` `liuliupi`.`表名` ``
- 邮件模板 `【POETIZE】` → `【LIULIUPI】`
- 域名提示 `poetize.cn` → `yangshare.com`
- `web_info` 表 `'POETIZE'` → `'LIULIUPI'`

#### 2.7 LICENSE
- `Copyright (c) 2022 POETIZE` → `Copyright (c) 2022 LIULIUPI`

### Layer 3：前端代码

**目标：** 修改两个前端子项目中的所有代码和资源。

**操作：**

#### 3.1 字体名（3 个 Vue 文件 + 2 个 CSS 文件）
- Vue 文件中：`"poetize-font"` → `"liuliupi-font"`
- CSS 文件中：`--globalFont: poetize-font` → `--globalFont: liuliupi-font`

涉及文件：
- `poetize-ui/src/components/admin/admin.vue`
- `poetize-ui/src/components/home.vue`
- `poetize-im-ui/src/components/index.vue`
- `poetize-ui/src/assets/css/color.css`
- `poetize-im-ui/src/assets/css/color.css`

#### 3.2 Favicon 文件重命名
- `poetize-ui/public/poetize.jpg` → `poetize-ui/public/liuliupi.jpg`
- `poetize-im-ui/public/poetize.jpg` → `poetize-im-ui/public/liuliupi.jpg`
- `poetize-im-ui/public/index.html` 中：`href="poetize.jpg"` → `href="liuliupi.jpg"`

#### 3.3 IM 前端 `index.html`
- `<title>POETIZE</title>` → `<title>LIULIUPI</title>`
- `<meta name="keywords">` 中 `POETIZE` → `LIULIUPI`
- `<meta name="description">` 重写为 LIULIUPI 版本
- `<link rel="canonical" href="https://poetize.cn">` → `href="https://yangshare.com"`

#### 3.4 LICENSE 文件
- `poetize-ui/LICENSE`：`POETIZE` → `LIULIUPI`
- `poetize-im-ui/LICENSE`：`POETIZE` → `LIULIUPI`

### Layer 4：目录重命名

**前提：** Layer 1–3 全部完成。

**操作：**

#### 4.1 顶级项目目录（使用 `git mv` 保留 git 历史）
- `poetize-server/` → `liuliupi-server/`
- `poetize-ui/` → `liuliupi-ui/`
- `poetize-im-ui/` → `liuliupi-im-ui/`
- `poetize_picture/` → `liuliupi_picture/`

#### 4.2 `.gitignore` 更新
- `/poetize-ui/pnpm-lock.yaml` → `/liuliupi-ui/pnpm-lock.yaml`
- `/poetize-ui/yarn.lock` → `/liuliupi-ui/yarn.lock`

### Layer 5：元数据

**操作：**

#### 5.1 README.md
- 域名引用：`poetize.cn` → `yangshare.com`
- Dockerfile 路径：`poetize-ui/Dockerfile` → `liuliupi-ui/Dockerfile`，`poetize-server/poetry-web/Dockerfile` → `liuliupi-server/poetry-web/Dockerfile`
- 图片路径：`poetize_picture/` → `liuliupi_picture/`

#### 5.2 IDE 配置 `.idea/`
- `.idea/poetize.iml` → `.idea/liuliupi.iml`
- `.idea/runConfigurations/poetize_ui.xml` → `.idea/runConfigurations/liuliupi_ui.xml`
  - 内容中所有 `poetize` 替换为 `liuliupi`
- `.idea/runConfigurations/poetize_server.xml` → `.idea/runConfigurations/liuliupi_server.xml`
  - 内容中所有 `poetize` 替换为 `liuliupi`

## 验证

所有 5 层完成后，运行：

1. `grep -ri "poetize" --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git` — 确认无遗漏
2. `mvn compile` — 确认后端编译通过
