# poetize → liuliupi 全局重命名 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将个人博客系统的工程名称从 "poetize" 彻底更换为 "liuliupi"，涉及 Java 包名、配置、SQL、前端代码、目录名和元数据。

**架构：** 自底向上分 5 层执行——Java 包名 → 配置/SQL/硬编码 → 前端代码 → 目录重命名 → 元数据。每层完成后 commit 一次，确保可回滚。

**技术栈：** Java 8 / SpringBoot 2.7 / MyBatis Plus / Maven / Vue 2 / Vue 3 / MySQL

**映射表：**

| 维度 | 旧值 | 新值 |
|---|---|---|
| Java 包名 | `com.ld.poetry` | `com.liuliupi` |
| MySQL 数据库名 | `poetize` | `liuliupi` |
| 构建产物名 | `poetize-server.jar` | `liuliupi-server.jar` |
| 域名 | `poetize.cn` | `yangshare.com` |
| 业务文本 | `POETIZE` | `LIULIUPI` |
| 前端字体名 | `poetize-font` | `liuliupi-font` |
| Favicon 文件名 | `poetize.jpg` | `liuliupi.jpg` |
| Docker 镜像/容器名 | `poetize-ui`, `poetize-server` | `liuliupi-ui`, `liuliupi-server` |
| 日志路径 | `/home/poetize/logs` | `/home/liuliupi/logs` |
| 顶级目录名 | `poetize-server`, `poetize-ui`, `poetize-im-ui`, `poetize_picture` | `liuliupi-server`, `liuliupi-ui`, `liuliupi-im-ui`, `liuliupi_picture` |

---

## 文件结构

### 将要修改的文件

**Java 源码（147 个 `.java` 文件）：**
- `poetize-server/poetry-web/src/main/java/com/ld/poetry/**/*.java` — 所有文件的 `package` 和 `import` 声明从 `com.ld.poetry` 改为 `com.liuliupi`
- 7 个文件含硬编码字符串需额外修改

**MyBatis Mapper XML（17 个 `.xml` 文件）：**
- `poetize-server/poetry-web/src/main/resources/mapper/**/*.xml` — `namespace` 和 `type`/`resultType` 中的包名

**Spring 配置：**
- `poetize-server/poetry-web/src/main/resources/META-INF/spring.factories` — `EnvironmentPostProcessor` 注册的类全限定名

**Maven POM（2 个）：**
- `poetize-server/pom.xml` — `<name>`, `<description>`, `<groupId>`
- `poetize-server/poetry-web/pom.xml` — `<finalName>`, `<groupId>`

**应用配置：**
- `poetize-server/poetry-web/src/main/resources/application.yml` — JDBC URL 中的数据库名
- `poetize-server/poetry-web/src/main/resources/logback-spring.xml` — `defaultValue` 中的应用名和日志路径

**Docker：**
- `poetize-server/poetry-web/Dockerfile` — JAR 文件名

**SQL：**
- `poetize-server/sql/poetry.sql` — 数据库名、表前缀、业务文本

**前端 Vue/CSS/HTML：**
- `poetize-ui/src/components/home.vue` — 字体名
- `poetize-ui/src/components/admin/admin.vue` — 字体名
- `poetize-ui/src/assets/css/color.css` — CSS 变量中的字体名
- `poetize-im-ui/src/components/index.vue` — 字体名
- `poetize-im-ui/src/assets/css/color.css` — CSS 变量中的字体名
- `poetize-im-ui/public/index.html` — title、meta、favicon、canonical URL

**LICENSE（2 个）：**
- `poetize-ui/LICENSE` — 版权持有人名
- `poetize-im-ui/LICENSE` — 版权持有人名

**元数据：**
- `README.md` — 域名、路径引用
- `.gitignore` — 目录路径
- `.idea/modules.xml` — IML 文件引用
- `.idea/poetize.iml` — 文件重命名
- `.idea/runConfigurations/poetize_ui.xml` — 重命名 + 内容替换
- `.idea/runConfigurations/poetize_server.xml` — 重命名 + 内容替换

### 将要重命名的文件/目录

**文件重命名：**
- `poetize-ui/public/poetize.jpg` → `poetize-ui/public/liuliupi.jpg`
- `poetize-im-ui/public/poetize.jpg` → `poetize-im-ui/public/liuliupi.jpg`
- `.idea/poetize.iml` → `.idea/liuliupi.iml`
- `.idea/runConfigurations/poetize_ui.xml` → `.idea/runConfigurations/liuliupi_ui.xml`
- `.idea/runConfigurations/poetize_server.xml` → `.idea/runConfigurations/liuliupi_server.xml`

**目录重命名（Layer 4，使用 `git mv`）：**
- `poetize-server/` → `liuliupi-server/`
- `poetize-ui/` → `liuliupi-ui/`
- `poetize-im-ui/` → `liuliupi-im-ui/`
- `poetize_picture/` → `liuliupi_picture/`

---

## 前置准备

- [ ] **步骤 0：确认 git 工作区干净**

运行：
```bash
git status
```
预期：工作区干净，无未提交的变更。如果有未提交的变更，先 commit 或 stash。

---

### 任务 1：Java 包名重命名 — 物理目录迁移

**文件：**
- 重命名：`poetize-server/poetry-web/src/main/java/com/ld/poetry/` → `poetize-server/poetry-web/src/main/java/com/liuliupi/`

**说明：** 这是最深层的变更。Java 包名从 `com.ld.poetry` 改为 `com.liuliupi`。使用 `git mv` 保留 git 历史。此时文件内容暂不修改（在任务 2 中处理）。

- [ ] **步骤 1：创建新目录**

运行：
```bash
mkdir -p poetize-server/poetry-web/src/main/java/com/liuliupi
git add poetize-server/poetry-web/src/main/java/com/liuliupi
```

- [ ] **步骤 2：使用 git mv 移动所有源码文件到新包目录（保留子目录结构）**

运行：
```bash
find poetize-server/poetry-web/src/main/java/com/ld/poetry -type d | while read dir; do
  newdir=$(echo "$dir" | sed 's|/com/ld/poetry|/com/liuliupi|')
  mkdir -p "$newdir"
done
find poetize-server/poetry-web/src/main/java/com/ld/poetry -type f | while read f; do
  newf=$(echo "$f" | sed 's|/com/ld/poetry/|/com/liuliupi/|')
  git mv "$f" "$newf"
done
```

- [ ] **步骤 3：删除旧的空目录**

运行：
```bash
find poetize-server/poetry-web/src/main/java/com/ld -type d -empty -delete
```

- [ ] **步骤 4：验证迁移完成**

运行：
```bash
find poetize-server/poetry-web/src/main/java/com/liuliupi -name "*.java" | wc -l
```
预期：输出 `147`（全部 Java 文件已迁移）。

```bash
find poetize-server/poetry-web/src/main/java/com/ld 2>/dev/null | wc -l
```
预期：输出 `0`（旧目录已清空）。

- [ ] **步骤 5：暂存变更（不提交，在任务 2 中一起提交）**

运行：
```bash
git status
```
预期：显示大量 `renamed:` 文件（从 `com/ld/poetry/` 到 `com/liuliupi/`）。不执行 commit。

---

### 任务 2：Java 包名重命名 — 批量替换 package/import 声明

**文件：**
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/**/*.java`（共 147 个文件，约 598 处引用）

- [ ] **步骤 1：批量替换所有 Java 文件中的包名**

运行（在项目根目录执行）：
```bash
find poetize-server/poetry-web/src/main/java/com/liuliupi -name "*.java" -exec sed -i 's/com\.ld\.poetry/com.liuliupi/g' {} +
```

- [ ] **步骤 2：验证替换完成**

运行：
```bash
grep -r "com\.ld\.poetry" poetize-server/poetry-web/src/main/java/com/liuliupi/ --include="*.java" | wc -l
```
预期：输出 `0`，表示没有遗留的旧包名。

- [ ] **步骤 3：验证新包名存在**

运行：
```bash
grep -r "package com.liuliupi" poetize-server/poetry-web/src/main/java/com/liuliupi/ --include="*.java" | wc -l
```
预期：输出 `147`（每个 Java 文件一个 `package` 声明）。

- [ ] **步骤 4：Commit（包含目录迁移 + 内容替换）**

运行：
```bash
git add poetize-server/poetry-web/src/main/java/com/
git commit -m "refactor: rename Java package com.ld.poetry to com.liuliupi"
```

---

### 任务 3：更新 MyBatis Mapper XML 中的包名

**文件：**
- 修改：`poetize-server/poetry-web/src/main/resources/mapper/*.xml`（12 个文件）
- 修改：`poetize-server/poetry-web/src/main/resources/mapper/im/*.xml`（5 个文件）

**变更内容：** 所有 `namespace="com.ld.poetry..."` 和 `type="com.ld.poetry..."` 和 `resultType="com.ld.poetry..."` 中的 `com.ld.poetry` → `com.liuliupi`

- [ ] **步骤 1：批量替换所有 Mapper XML 中的包名**

运行：
```bash
find poetize-server/poetry-web/src/main/resources/mapper -name "*.xml" -exec sed -i 's/com\.ld\.poetry/com.liuliupi/g' {} +
```

- [ ] **步骤 2：验证替换完成**

运行：
```bash
grep -r "com\.ld\.poetry" poetize-server/poetry-web/src/main/resources/mapper/ --include="*.xml" | wc -l
```
预期：输出 `0`。

- [ ] **步骤 3：验证新包名**

运行：
```bash
grep -r "com\.liuliupi" poetize-server/poetry-web/src/main/resources/mapper/ --include="*.xml" | wc -l
```
预期：输出大于 `0`（约 34 处引用）。

- [ ] **步骤 4：Commit**

运行：
```bash
git add poetize-server/poetry-web/src/main/resources/mapper/
git commit -m "refactor: update MyBatis mapper XML package names to com.liuliupi"
```

---

### 任务 4：更新 spring.factories 中的包名

**文件：**
- 修改：`poetize-server/poetry-web/src/main/resources/META-INF/spring.factories:1`

**当前内容（第 1 行）：**
```
org.springframework.boot.env.EnvironmentPostProcessor=com.ld.poetry.config.CustomEnvironmentPostProcessor
```

**替换为：**
```
org.springframework.boot.env.EnvironmentPostProcessor=com.liuliupi.config.CustomEnvironmentPostProcessor
```

- [ ] **步骤 1：替换 spring.factories 中的类全限定名**

运行：
```bash
sed -i 's/com\.ld\.poetry/com.liuliupi/g' poetize-server/poetry-web/src/main/resources/META-INF/spring.factories
```

- [ ] **步骤 2：验证替换**

运行：
```bash
cat poetize-server/poetry-web/src/main/resources/META-INF/spring.factories
```
预期输出：
```
org.springframework.boot.env.EnvironmentPostProcessor=com.liuliupi.config.CustomEnvironmentPostProcessor
```

- [ ] **步骤 3：Commit**

运行：
```bash
git add poetize-server/poetry-web/src/main/resources/META-INF/spring.factories
git commit -m "refactor: update spring.factories class reference to com.liuliupi"
```

---

### 任务 5：Maven POM 更新

**文件：**
- 修改：`poetize-server/pom.xml:6,9,10`
- 修改：`poetize-server/poetry-web/pom.xml:109`

- [ ] **步骤 1：修改父 POM — groupId、name、description**

在 `poetize-server/pom.xml` 中：

第 6 行 `<groupId>com.ld</groupId>` → `<groupId>com.liuliupi</groupId>`

第 9 行 `<name>POETIZE - 最美博客</name>` → `<name>LIULIUPI</name>`

第 10 行 `<description>POETIZE - 最美博客</description>` → `<description>LIULIUPI</description>`

运行：
```bash
sed -i 's|<groupId>com.ld</groupId>|<groupId>com.liuliupi</groupId>|g' poetize-server/pom.xml
sed -i 's|<name>POETIZE - 最美博客</name>|<name>LIULIUPI</name>|g' poetize-server/pom.xml
sed -i 's|<description>POETIZE - 最美博客</description>|<description>LIULIUPI</description>|g' poetize-server/pom.xml
```

- [ ] **步骤 2：修改子 POM — groupId、finalName**

在 `poetize-server/poetry-web/pom.xml` 中：

第 8 行 `<groupId>com.ld</groupId>` → `<groupId>com.liuliupi</groupId>`

第 109 行 `<finalName>poetize-server</finalName>` → `<finalName>liuliupi-server</finalName>`

运行：
```bash
sed -i 's|<groupId>com.ld</groupId>|<groupId>com.liuliupi</groupId>|g' poetize-server/poetry-web/pom.xml
sed -i 's|<finalName>poetize-server</finalName>|<finalName>liuliupi-server</finalName>|g' poetize-server/poetry-web/pom.xml
```

- [ ] **步骤 3：更新父 POM 中子模块依赖的 groupId**

在 `poetize-server/pom.xml` 第 83 行：

`<groupId>com.ld</groupId>` → `<groupId>com.liuliupi</groupId>`

**说明：** 这在步骤 1 的批量替换中已处理（因为 `com.ld` 被替换为 `com.liuliupi`），运行验证确认即可。

- [ ] **步骤 4：验证 POM 中无遗留旧值**

运行：
```bash
grep -n "com.ld\|POETIZE\|poetize" poetize-server/pom.xml poetize-server/poetry-web/pom.xml
```
预期：无输出（所有引用已替换）。

- [ ] **步骤 5：Commit**

运行：
```bash
git add poetize-server/pom.xml poetize-server/poetry-web/pom.xml
git commit -m "refactor: update Maven POM groupId, name, description and finalName"
```

---

### 任务 6：application.yml — 数据库名更新

**文件：**
- 修改：`poetize-server/poetry-web/src/main/resources/application.yml:43`

**当前第 43 行：**
```yaml
    url: jdbc:mysql://192.168.6.3:3306/poetize?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

**替换为：**
```yaml
    url: jdbc:mysql://192.168.6.3:3306/liuliupi?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

- [ ] **步骤 1：替换 JDBC URL 中的数据库名**

运行：
```bash
sed -i 's|3306/poetize?|3306/liuliupi?|g' poetize-server/poetry-web/src/main/resources/application.yml
```

- [ ] **步骤 2：验证替换**

运行：
```bash
grep "jdbc" poetize-server/poetry-web/src/main/resources/application.yml
```
预期输出：
```
    url: jdbc:mysql://192.168.6.3:3306/liuliupi?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

- [ ] **步骤 3：Commit**

运行：
```bash
git add poetize-server/poetry-web/src/main/resources/application.yml
git commit -m "refactor: update database name in application.yml to liuliupi"
```

---

### 任务 7：logback-spring.xml — 应用名和日志路径更新

**文件：**
- 修改：`poetize-server/poetry-web/src/main/resources/logback-spring.xml:8,11`

**当前第 8 行：**
```xml
    <springProperty scope="context" name="springAppName" source="spring.application.name" defaultValue="poetize"/>
```

**替换为：**
```xml
    <springProperty scope="context" name="springAppName" source="spring.application.name" defaultValue="liuliupi"/>
```

**当前第 11 行：**
```xml
    <springProperty scope="context" name="logback.rootDir" source="logback.rootDir" defaultValue="/home/poetize/logs"/>
```

**替换为：**
```xml
    <springProperty scope="context" name="logback.rootDir" source="logback.rootDir" defaultValue="/home/liuliupi/logs"/>
```

- [ ] **步骤 1：替换 logback 中的应用名和日志路径**

运行：
```bash
sed -i 's|defaultValue="poetize"|defaultValue="liuliupi"|g' poetize-server/poetry-web/src/main/resources/logback-spring.xml
sed -i 's|/home/poetize/logs|/home/liuliupi/logs|g' poetize-server/poetry-web/src/main/resources/logback-spring.xml
```

- [ ] **步骤 2：验证替换**

运行：
```bash
grep -n "poetize" poetize-server/poetry-web/src/main/resources/logback-spring.xml
```
预期：无输出。

- [ ] **步骤 3：Commit**

运行：
```bash
git add poetize-server/poetry-web/src/main/resources/logback-spring.xml
git commit -m "refactor: update logback app name and log path to liuliupi"
```

---

### 任务 8：Dockerfile — JAR 文件名更新

**文件：**
- 修改：`poetize-server/poetry-web/Dockerfile:8,12`

**当前第 8 行：**
```dockerfile
ADD target/*.jar poetize-server.jar
```

**替换为：**
```dockerfile
ADD target/*.jar liuliupi-server.jar
```

**当前第 12 行：**
```dockerfile
ENTRYPOINT ["java", "-jar", "poetize-server.jar"]
```

**替换为：**
```dockerfile
ENTRYPOINT ["java", "-jar", "liuliupi-server.jar"]
```

- [ ] **步骤 1：替换 Dockerfile 中的 JAR 文件名**

运行：
```bash
sed -i 's|poetize-server\.jar|liuliupi-server.jar|g' poetize-server/poetry-web/Dockerfile
```

- [ ] **步骤 2：验证替换**

运行：
```bash
cat poetize-server/poetry-web/Dockerfile
```
预期：第 8 行显示 `ADD target/*.jar liuliupi-server.jar`，第 12 行显示 `ENTRYPOINT ["java", "-jar", "liuliupi-server.jar"]`。

- [ ] **步骤 3：Commit**

运行：
```bash
git add poetize-server/poetry-web/Dockerfile
git commit -m "refactor: update Dockerfile JAR name to liuliupi-server.jar"
```

---

### 任务 9：Java 代码中的硬编码字符串更新

**文件：**
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/config/CustomEnvironmentPostProcessor.java:28`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/utils/CodeGenerator.java:83`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/im/websocket/ImConfigConst.java:20`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/utils/mail/MailUtil.java:89`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/utils/mail/MailSendUtil.java:72,89,137,150`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/service/impl/ArticleServiceImpl.java:110,121`
- 修改：`poetize-server/poetry-web/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java:315,343,433,597`

- [ ] **步骤 1：CustomEnvironmentPostProcessor — 数据库名**

在 `CustomEnvironmentPostProcessor.java` 第 28 行：

`private static final String DATABASE = "poetize";` → `private static final String DATABASE = "liuliupi";`

运行：
```bash
sed -i 's|DATABASE = "poetize"|DATABASE = "liuliupi"|g' poetize-server/poetry-web/src/main/java/com/liuliupi/config/CustomEnvironmentPostProcessor.java
```

- [ ] **步骤 2：CodeGenerator — JDBC URL**

在 `CodeGenerator.java` 第 83 行：

`"jdbc:mysql://ip:port/poetize?...` → `"jdbc:mysql://ip:port/liuliupi?...`

运行：
```bash
sed -i 's|/poetize?|/liuliupi?|g' poetize-server/poetry-web/src/main/java/com/liuliupi/utils/CodeGenerator.java
```

- [ ] **步骤 3：ImConfigConst — 协议名**

在 `ImConfigConst.java` 第 20 行：

`"protocol_poetize"` → `"protocol_liuliupi"`

运行：
```bash
sed -i 's|protocol_poetize|protocol_liuliupi|g' poetize-server/poetry-web/src/main/java/com/liuliupi/im/websocket/ImConfigConst.java
```

- [ ] **步骤 4：MailUtil — 域名**

在 `MailUtil.java` 第 89 行：

`href="https://poetize.cn"` → `href="https://yangshare.com"`

运行：
```bash
sed -i 's|https://poetize\.cn|https://yangshare.com|g' poetize-server/poetry-web/src/main/java/com/liuliupi/utils/mail/MailUtil.java
```

- [ ] **步骤 5：MailSendUtil — 品牌文本（4 处）**

在 `MailSendUtil.java` 第 72、89、137、150 行：

`"POETIZE"` → `"LIULIUPI"`

运行：
```bash
sed -i 's|"POETIZE"|"LIULIUPI"|g' poetize-server/poetry-web/src/main/java/com/liuliupi/utils/mail/MailSendUtil.java
```

- [ ] **步骤 6：ArticleServiceImpl — 品牌文本（2 处）**

在 `ArticleServiceImpl.java` 第 110、121 行：

`"POETIZE"` → `"LIULIUPI"`

运行：
```bash
sed -i 's|"POETIZE"|"LIULIUPI"|g' poetize-server/poetry-web/src/main/java/com/liuliupi/service/impl/ArticleServiceImpl.java
```

- [ ] **步骤 7：UserServiceImpl — 品牌文本（4 处）**

在 `UserServiceImpl.java` 第 315、343、433、597 行：

`"POETIZE"` → `"LIULIUPI"`

运行：
```bash
sed -i 's|"POETIZE"|"LIULIUPI"|g' poetize-server/poetry-web/src/main/java/com/liuliupi/service/impl/UserServiceImpl.java
```

- [ ] **步骤 8：验证所有 Java 文件中无遗留**

运行：
```bash
grep -rn "poetize\|POETIZE" poetize-server/poetry-web/src/main/java/ --include="*.java"
```
预期：无输出（所有 `poetize`/`POETIZE` 已替换）。

- [ ] **步骤 9：Commit**

运行：
```bash
git add poetize-server/poetry-web/src/main/java/com/liuliupi/
git commit -m "refactor: update hardcoded strings in Java source code"
```

---

### 任务 10：SQL 脚本更新

**文件：**
- 修改：`poetize-server/sql/poetry.sql`（全文件约 40+ 处替换）

**变更内容：**
1. 第 1 行：`CREATE DATABASE IF NOT EXISTS poetize` → `CREATE DATABASE IF NOT EXISTS liuliupi`
2. 所有 `` `poetize`.`表名` `` → `` `liuliupi`.`表名` ``（约 30 处）
3. 第 327 行：`【POETIZE】` → `【LIULIUPI】`
4. 第 328 行：`【POETIZE】` → `【LIULIUPI】`
5. 第 314 行：`'POETIZE'` → `'LIULIUPI'`（web_info 表的 web_title）
6. 第 333 行：`https://poetize.cn` → `https://yangshare.com`
7. 第 337 行：`https://file.poetize.cn` → `https://file.yangshare.com`
8. 第 341 行：`https://poetize.cn` → `https://yangshare.com`

- [ ] **步骤 1：替换 SQL 中的数据库名（反引号包裹的）**

运行：
```bash
sed -i 's/`poetize`\./`liuliupi`./g' poetize-server/sql/poetry.sql
```

- [ ] **步骤 2：替换 CREATE DATABASE 语句中的数据库名**

运行：
```bash
sed -i 's/DATABASE IF NOT EXISTS poetize/DATABASE IF NOT EXISTS liuliupi/g' poetize-server/sql/poetry.sql
```

- [ ] **步骤 3：替换业务文本 POETIZE → LIULIUPI**

运行：
```bash
sed -i 's/【POETIZE】/【LIULIUPI】/g' poetize-server/sql/poetry.sql
sed -i "s/'POETIZE'/'LIULIUPI'/g" poetize-server/sql/poetry.sql
```

- [ ] **步骤 4：替换域名 poetize.cn → yangshare.com**

运行：
```bash
sed -i 's|poetize\.cn|yangshare.com|g' poetize-server/sql/poetry.sql
```

- [ ] **步骤 5：验证 SQL 文件中无遗留**

运行：
```bash
grep -in "poetize" poetize-server/sql/poetry.sql
```
预期：无输出。

- [ ] **步骤 6：Commit**

运行：
```bash
git add poetize-server/sql/poetry.sql
git commit -m "refactor: update SQL script database name, brand text and domain"
```

---

### 任务 11：后端编译验证

- [ ] **步骤 1：运行 Maven 编译**

运行：
```bash
cd poetize-server
mvn compile -q
```
预期：`BUILD SUCCESS`，无编译错误。

- [ ] **步骤 2：如果编译失败，根据错误信息修复**

常见可能问题：
- 遗漏的 import 语句 → 手动修复对应文件
- mapper XML 中遗漏的 namespace → 手动修复

修复后重新运行 `mvn compile -q` 直到通过。

- [ ] **步骤 3：Commit 修复（如有）**

运行：
```bash
git add -A
git commit -m "fix: resolve compilation errors after package rename"
```

（如果没有编译错误，跳过此步骤。）

---

### 任务 12：前端字体名更新

**文件：**
- 修改：`poetize-ui/src/components/admin/admin.vue:37`
- 修改：`poetize-ui/src/components/home.vue:441`
- 修改：`poetize-ui/src/assets/css/color.css:74`
- 修改：`poetize-im-ui/src/components/index.vue:667`
- 修改：`poetize-im-ui/src/assets/css/color.css:72`

- [ ] **步骤 1：替换 Vue 文件中的字体名**

运行：
```bash
sed -i 's|"poetize-font"|"liuliupi-font"|g' poetize-ui/src/components/admin/admin.vue
sed -i 's|"poetize-font"|"liuliupi-font"|g' poetize-ui/src/components/home.vue
sed -i 's|"poetize-font"|"liuliupi-font"|g' poetize-im-ui/src/components/index.vue
```

- [ ] **步骤 2：替换 CSS 文件中的字体名**

运行：
```bash
sed -i 's|--globalFont: poetize-font|--globalFont: liuliupi-font|g' poetize-ui/src/assets/css/color.css
sed -i 's|--globalFont: poetize-font|--globalFont: liuliupi-font|g' poetize-im-ui/src/assets/css/color.css
```

- [ ] **步骤 3：验证前端文件中无遗留**

运行：
```bash
grep -rn "poetize-font" poetize-ui/src/ poetize-im-ui/src/
```
预期：无输出。

- [ ] **步骤 4：Commit**

运行：
```bash
git add poetize-ui/src/components/admin/admin.vue poetize-ui/src/components/home.vue poetize-ui/src/assets/css/color.css poetize-im-ui/src/components/index.vue poetize-im-ui/src/assets/css/color.css
git commit -m "refactor: rename font from poetize-font to liuliupi-font in frontend"
```

---

### 任务 13：前端 Favicon 文件重命名和引用更新

**文件：**
- 重命名：`poetize-ui/public/poetize.jpg` → `poetize-ui/public/liuliupi.jpg`
- 重命名：`poetize-im-ui/public/poetize.jpg` → `poetize-im-ui/public/liuliupi.jpg`
- 修改：`poetize-im-ui/public/index.html:5`

- [ ] **步骤 1：重命名 Favicon 文件**

运行：
```bash
git mv poetize-ui/public/poetize.jpg poetize-ui/public/liuliupi.jpg
git mv poetize-im-ui/public/poetize.jpg poetize-im-ui/public/liuliupi.jpg
```

- [ ] **步骤 2：更新 IM 前端 index.html 中的 favicon 引用**

在 `poetize-im-ui/public/index.html` 第 5 行：

`<link rel="icon" href="poetize.jpg" sizes="16x16">` → `<link rel="icon" href="liuliupi.jpg" sizes="16x16">`

运行：
```bash
sed -i 's|href="poetize.jpg"|href="liuliupi.jpg"|g' poetize-im-ui/public/index.html
```

- [ ] **步骤 3：验证**

运行：
```bash
grep -n "poetize" poetize-im-ui/public/index.html
```
预期：仍有输出（title、meta 中的 POETIZE 和 poetize.cn 尚未处理，那些在任务 14 处理）。

- [ ] **步骤 4：Commit**

运行：
```bash
git add poetize-ui/public/ poetize-im-ui/public/
git commit -m "refactor: rename favicon from poetize.jpg to liuliupi.jpg"
```

---

### 任务 14：IM 前端 index.html — 标题、Meta、域名更新

**文件：**
- 修改：`poetize-im-ui/public/index.html:7,8,9,11`

**当前第 7 行：**
```html
    <title>POETIZE</title>
```
**替换为：**
```html
    <title>LIULIUPI</title>
```

**当前第 8 行：**
```html
    <meta name="keywords" content="POETIZE,Sara,生活倒影,最美博客,个人博客,个人网站,生活笔记,记录生活,Java,SpringBoot,Vue,Vue2,Vue3,IM,聊天室">
```
**替换为：**
```html
    <meta name="keywords" content="LIULIUPI,Sara,生活倒影,最美博客,个人博客,个人网站,生活笔记,记录生活,Java,SpringBoot,Vue,Vue2,Vue3,IM,聊天室">
```

**当前第 9 行：**
```html
    <meta name="description" content="POETIZE：作诗，有诗意地描写。这是我的个人网站，我的生活倒影，有诗意地记录自己的生活。它是一个 SpringBoot + Vue2 + Vue3 的产物。">
```
**替换为：**
```html
    <meta name="description" content="LIULIUPI：作诗，有诗意地描写。这是我的个人网站，我的生活倒影，有诗意地记录自己的生活。它是一个 SpringBoot + Vue2 + Vue3 的产物。">
```

**当前第 11 行：**
```html
    <link rel="canonical" href="https://poetize.cn">
```
**替换为：**
```html
    <link rel="canonical" href="https://yangshare.com">
```

- [ ] **步骤 1：替换 title 和 meta 中的 POETIZE**

运行：
```bash
sed -i 's|<title>POETIZE</title>|<title>LIULIUPI</title>|g' poetize-im-ui/public/index.html
sed -i 's|content="POETIZE,|content="LIULIUPI,|g' poetize-im-ui/public/index.html
sed -i 's|content="POETIZE：|content="LIULIUPI：|g' poetize-im-ui/public/index.html
```

- [ ] **步骤 2：替换 canonical URL 中的域名**

运行：
```bash
sed -i 's|https://poetize\.cn|https://yangshare.com|g' poetize-im-ui/public/index.html
```

- [ ] **步骤 3：验证**

运行：
```bash
grep -in "poetize" poetize-im-ui/public/index.html
```
预期：无输出。

- [ ] **步骤 4：Commit**

运行：
```bash
git add poetize-im-ui/public/index.html
git commit -m "refactor: update IM frontend title, meta tags and canonical URL"
```

---

### 任务 15：前端 LICENSE 文件更新

**文件：**
- 修改：`poetize-ui/LICENSE:3`
- 修改：`poetize-im-ui/LICENSE:3`

**当前两个文件第 3 行均为：**
```
Copyright (c) 2022 POETIZE
```
**替换为：**
```
Copyright (c) 2022 LIULIUPI
```

- [ ] **步骤 1：替换两个 LICENSE 中的版权名**

运行：
```bash
sed -i 's|Copyright (c) 2022 POETIZE|Copyright (c) 2022 LIULIUPI|g' poetize-ui/LICENSE
sed -i 's|Copyright (c) 2022 POETIZE|Copyright (c) 2022 LIULIUPI|g' poetize-im-ui/LICENSE
```

- [ ] **步骤 2：验证**

运行：
```bash
head -3 poetize-ui/LICENSE poetize-im-ui/LICENSE
```
预期：两个文件第 3 行均为 `Copyright (c) 2022 LIULIUPI`。

- [ ] **步骤 3：Commit**

运行：
```bash
git add poetize-ui/LICENSE poetize-im-ui/LICENSE
git commit -m "refactor: update LICENSE copyright holder to LIULIUPI"
```

---

### 任务 16：顶级目录重命名

**文件：**
- 重命名：`poetize-server/` → `liuliupi-server/`
- 重命名：`poetize-ui/` → `liuliupi-ui/`
- 重命名：`poetize-im-ui/` → `liuliupi-im-ui/`
- 重命名：`poetize_picture/` → `liuliupi_picture/`

**说明：** 使用 `git mv` 保留 git 历史。必须在 Layer 1–3 全部完成后执行。

- [ ] **步骤 1：重命名后端目录**

运行：
```bash
git mv poetize-server liuliupi-server
```

- [ ] **步骤 2：重命名主前端目录**

运行：
```bash
git mv poetize-ui liuliupi-ui
```

- [ ] **步骤 3：重命名 IM 前端目录**

运行：
```bash
git mv poetize-im-ui liuliupi-im-ui
```

- [ ] **步骤 4：重命名图片目录**

运行：
```bash
git mv poetize_picture liuliupi_picture
```

- [ ] **步骤 5：验证目录结构**

运行：
```bash
ls -d liuliupi-* liuliupi_picture
```
预期：列出 `liuliupi-im-ui  liuliupi-server  liuliupi-ui  liuliupi_picture`。

- [ ] **步骤 6：Commit**

运行：
```bash
git add -A
git commit -m "refactor: rename top-level directories from poetize-* to liuliupi-*"
```

---

### 任务 17：.gitignore 更新

**文件：**
- 修改：`.gitignore:34,35`

**当前第 34–35 行：**
```
/poetize-ui/pnpm-lock.yaml
/poetize-ui/yarn.lock
```

**替换为：**
```
/liuliupi-ui/pnpm-lock.yaml
/liuliupi-ui/yarn.lock
```

- [ ] **步骤 1：替换 .gitignore 中的目录路径**

运行：
```bash
sed -i 's|/poetize-ui/|/liuliupi-ui/|g' .gitignore
```

- [ ] **步骤 2：验证**

运行：
```bash
grep -n "poetize" .gitignore
```
预期：无输出。

- [ ] **步骤 3：Commit**

运行：
```bash
git add .gitignore
git commit -m "refactor: update .gitignore paths for renamed directories"
```

---

### 任务 18：README.md 更新

**文件：**
- 修改：`README.md:2,12,16,18,20`

- [ ] **步骤 1：替换域名引用**

在 `README.md` 第 2 行：

`[poetize.cn](https://poetize.cn)` → `[yangshare.com](https://yangshare.com)`

运行：
```bash
sed -i 's|poetize\.cn|yangshare.com|g' README.md
```

- [ ] **步骤 2：替换图片路径**

在 `README.md` 第 12、16 行：

`poetize_picture/` → `liuliupi_picture/`

运行：
```bash
sed -i 's|poetize_picture/|liuliupi_picture/|g' README.md
```

- [ ] **步骤 3：替换 Dockerfile 路径**

在 `README.md` 第 18 行：

`poetize-ui/Dockerfile` → `liuliupi-ui/Dockerfile`

在 `README.md` 第 20 行：

`poetize-server/poetry-web/Dockerfile` → `liuliupi-server/poetry-web/Dockerfile`

运行：
```bash
sed -i 's|poetize-ui/|liuliupi-ui/|g' README.md
sed -i 's|poetize-server/|liuliupi-server/|g' README.md
```

- [ ] **步骤 4：验证**

运行：
```bash
grep -in "poetize" README.md
```
预期：Gitee URL 中 `yangshare/poetize` 仍然保留（这是远程仓库名，不在本规格范围内），其余所有 `poetize` 引用已替换。如果只有 Gitee URL 行有输出，则视为通过。

- [ ] **步骤 5：Commit**

运行：
```bash
git add README.md
git commit -m "docs: update README with new domain, paths and directory names"
```

---

### 任务 19：IDE 配置更新

**文件：**
- 重命名：`.idea/poetize.iml` → `.idea/liuliupi.iml`
- 修改：`.idea/modules.xml:5`
- 重命名：`.idea/runConfigurations/poetize_ui.xml` → `.idea/runConfigurations/liuliupi_ui.xml`
- 修改：重命名后的 `.idea/runConfigurations/liuliupi_ui.xml` 内容
- 重命名：`.idea/runConfigurations/poetize_server.xml` → `.idea/runConfigurations/liuliupi_server.xml`
- 修改：重命名后的 `.idea/runConfigurations/liuliupi_server.xml` 内容

**重要说明：** `.idea/` 在 `.gitignore` 中，不被 git 追踪。这些修改仅影响本地 IDE 配置。使用普通 `mv` 而非 `git mv`。无需 commit。

- [ ] **步骤 1：重命名 IML 文件并更新 modules.xml**

运行：
```bash
mv .idea/poetize.iml .idea/liuliupi.iml
sed -i 's|poetize\.iml|liuliupi.iml|g' .idea/modules.xml
```

- [ ] **步骤 2：重命名运行配置并替换内容**

运行：
```bash
mv .idea/runConfigurations/poetize_ui.xml .idea/runConfigurations/liuliupi_ui.xml
mv .idea/runConfigurations/poetize_server.xml .idea/runConfigurations/liuliupi_server.xml
```

- [ ] **步骤 3：替换运行配置 XML 内容中的所有 poetize 引用**

运行：
```bash
sed -i 's|poetize|liuliupi|g' .idea/runConfigurations/liuliupi_ui.xml
sed -i 's|poetize|liuliupi|g' .idea/runConfigurations/liuliupi_server.xml
```

- [ ] **步骤 4：验证**

运行：
```bash
grep -rin "poetize" .idea/
```
预期：无输出。

**注意：** `.idea/` 不被 git 追踪，无需 commit。

---

### 任务 20：最终全局验证

- [ ] **步骤 1：全局搜索确认无遗漏**

运行：
```bash
grep -ri "poetize" --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git --exclude-dir=dist --exclude-dir=docs .
```
预期：仅有 `README.md` 中的 Gitee 仓库 URL（`gitee.com/yangshare/poetize`）可能匹配，这是远程仓库名，不在本规格范围内。其他文件不应有匹配。

- [ ] **步骤 2：如果有遗漏，逐个修复**

对于步骤 1 输出的每个匹配项（排除 docs/ 目录），使用 `sed` 或手动编辑修复。

- [ ] **步骤 3：全局搜索 POETIZE（大写）**

运行：
```bash
grep -ri "POETIZE" --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git --exclude-dir=dist .
```
预期：无输出（排除 docs/ 目录）。

- [ ] **步骤 4：全局搜索 com.ld.poetry**

运行：
```bash
grep -ri "com\.ld\.poetry" --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git --exclude-dir=dist .
```
预期：无输出（排除 docs/ 目录）。

- [ ] **步骤 5：再次运行 Maven 编译确认**

运行：
```bash
cd liuliupi-server
mvn compile -q
```
预期：`BUILD SUCCESS`。

- [ ] **步骤 6：最终 Commit**

运行：
```bash
git add -A
git status
```

如果有任何未提交的修复：
```bash
git commit -m "fix: final cleanup of remaining poetize references"
```

如果没有未提交的变更，则跳过 commit。

---

## 自检清单

完成后核对：

| 维度 | 检查方式 | 预期 |
|---|---|---|
| Java 包名 | `grep -r "com.ld.poetry" liuliupi-server/ --include="*.java"` | 0 匹配 |
| 数据库名 | `grep -r "\"poetize\"" liuliupi-server/ --include="*.java"` | 0 匹配 |
| 配置文件中 `poetize` | `grep -r "poetize" liuliupi-server/ --include="*.yml" --include="*.xml" --include="*.properties"` | 0 匹配 |
| 前端 `poetize-font` | `grep -r "poetize-font" liuliupi-ui/ liuliupi-im-ui/` | 0 匹配 |
| 目录名 | `ls -d poetize-* 2>/dev/null` | 无输出 |
| 品牌文本 | `grep -r "POETIZE" --exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist --exclude-dir=.git --exclude-dir=docs .` | 0 匹配 |
| 编译 | `cd liuliupi-server && mvn compile -q` | BUILD SUCCESS |
| README 例外 | `grep -n "poetize" README.md` | 仅 `gitee.com/yangshare/poetize` URL（远程仓库名） |
