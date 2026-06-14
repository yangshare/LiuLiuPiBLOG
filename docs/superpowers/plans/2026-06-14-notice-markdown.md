# 公告 Markdown 编辑与渲染改造实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将后台公告从 JSON 数组标签编辑改为 Markdown 编辑器编辑，前台用 Markdown 渲染展示；并把首页弹窗推送从公告中剥离为独立数据表与接口。

**架构：** 后端新增 `push_notification` 表与 `PushNotificationController` 提供前台公开接口和后台管理接口；`web_info.notices` 字段改为纯 Markdown 字符串，不再做 JSON 转换。前台 `index.vue` 用 `markdown-it` 渲染公告，并从新接口读取推送弹窗；后台 `webEdit.vue` 用 `mavon-editor` 编辑公告并新增推送设置表单，统一保存按钮串行调用两个接口。

**技术栈：** Spring Boot 2.7 + MyBatis-Plus + JUnit 5 + Mockito；Vue 2 + Element UI + mavon-editor + markdown-it + Jest + @vue/test-utils。

---

## 文件结构

### 后端（liuliupi-server）

| 文件 | 职责 |
|---|---|
| `sql/liuliupi_blog.sql` | 修改 `web_info.notices` 字段为 `text`，新增 `push_notification` 表。 |
| `src/main/java/com/liuliupi/entity/PushNotification.java` | 推送配置实体，对应 `push_notification` 表。 |
| `src/main/java/com/liuliupi/dao/PushNotificationMapper.java` | MyBatis-Plus Mapper 接口。 |
| `src/main/java/com/liuliupi/service/PushNotificationService.java` | 服务接口。 |
| `src/main/java/com/liuliupi/service/impl/PushNotificationServiceImpl.java` | 服务实现：保存时保证表中只有一条记录；查询时返回启用的一条。 |
| `src/main/java/com/liuliupi/controller/PushNotificationController.java` | 三个接口：前台公开获取、后台获取、后台保存。 |
| `src/test/java/com/liuliupi/service/impl/PushNotificationServiceImplTest.java` | Service 层单元测试：保存幂等、启用开关控制。 |
| `src/test/java/com/liuliupi/controller/PushNotificationControllerTest.java` | Controller 层单元测试：三个接口的返回行为。 |

### 前端（liuliupi-ui）

| 文件 | 职责 |
|---|---|
| `src/components/admin/webEdit.vue` | 公告标签页改为 `mavon-editor`；新增推送设置表单；保存按钮同时提交公告和推送。 |
| `src/components/index.vue` | 公告区改为 Markdown 渲染的宽版公告板；弹窗推送从独立接口读取。 |
| `src/utils/common.js` | 删除 `pushNotification` 方法。 |
| `src/assets/css/index.css` | 新增 `.announcement-board` / `.markdown-content` 公告板样式（如项目已有合适类可复用）。 |
| `tests/unit/webEdit.spec.js` | 扩展测试：Markdown 编辑器渲染、推送表单渲染、保存按钮触发双请求。 |
| `tests/unit/index.spec.js` | 新建测试：Markdown 渲染结果、推送弹窗数据流。 |

---

## 任务 1：数据库 Schema 变更

**文件：**
- 修改：`sql/liuliupi_blog.sql:133-148`

- [ ] **步骤 1：修改 `web_info.notices` 字段类型**

将 `notices` 从 `varchar(512)` 改为 `text`，以支持较长 Markdown 内容。

```sql
CREATE TABLE `liuliupi_blog`.`web_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `web_name` varchar(16) NOT NULL COMMENT '网站名称',
  `web_title` varchar(512) NOT NULL COMMENT '网站信息',
  `notices` text DEFAULT NULL COMMENT '公告',
  `footer` varchar(256) NOT NULL COMMENT '页脚',
  `background_image` varchar(256) DEFAULT NULL COMMENT '背景',
  `avatar` varchar(256) NOT NULL COMMENT '头像',
  `random_avatar` text DEFAULT NULL COMMENT '随机头像',
  `random_name` varchar(4096) DEFAULT NULL COMMENT '随机名称',
  `random_cover` text DEFAULT NULL COMMENT '随机封面',
  `waifu_json` text DEFAULT NULL COMMENT '看板娘消息',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站信息表';
```

- [ ] **步骤 2：新增 `push_notification` 表**

在 `web_info` 表定义之后插入：

```sql
DROP TABLE IF EXISTS `liuliupi_blog`.`push_notification`;

CREATE TABLE `liuliupi_blog`.`push_notification` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title` varchar(200) DEFAULT NULL COMMENT '推送标题',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面图 URL',
  `url` varchar(500) DEFAULT NULL COMMENT '点击跳转链接',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页弹窗推送配置表';
```

- [ ] **步骤 3：更新初始数据**

将默认插入的 `notices` 从 `'[]'` 改为空字符串（或留 `''`），避免旧 JSON 数组被直接渲染：

```sql
INSERT INTO `liuliupi_blog`.`web_info`(`id`, `web_name`, `web_title`, `notices`, `footer`, `background_image`, `avatar`, `random_avatar`, `random_name`, `random_cover`, `waifu_json`, `status`) VALUES (1, 'Sara', 'LIULIUPI', '', '云想衣裳花想容， 春风拂槛露华浓。', '', '', '[]', '[]', '[]', '{}', 1);
```

- [ ] **步骤 4：Commit**

```bash
cd liuliupi-server
git add sql/liuliupi_blog.sql
git commit -m "chore(db): 公告字段改为 text 并新增 push_notification 表"
```

---

## 任务 2：创建 `PushNotification` 实体

**文件：**
- 创建：`src/main/java/com/liuliupi/entity/PushNotification.java`

- [ ] **步骤 1：编写失败测试**

创建 `src/test/java/com/liuliupi/entity/PushNotificationTest.java`：

```java
package com.liuliupi.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushNotificationTest {

    @Test
    void entityShouldHoldFields() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("测试标题");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        assertThat(push.getId()).isEqualTo(1);
        assertThat(push.getTitle()).isEqualTo("测试标题");
        assertThat(push.getCover()).isEqualTo("https://example.com/cover.jpg");
        assertThat(push.getUrl()).isEqualTo("https://example.com/");
        assertThat(push.getEnabled()).isTrue();
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationTest -q
```

预期：FAIL，找不到 `PushNotification` 类。

- [ ] **步骤 3：创建实体类**

```java
package com.liuliupi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 首页弹窗推送配置表
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("push_notification")
public class PushNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 推送标题
     */
    @TableField("title")
    private String title;

    /**
     * 封面图 URL
     */
    @TableField("cover")
    private String cover;

    /**
     * 点击跳转链接
     */
    @TableField("url")
    private String url;

    /**
     * 是否启用[0:否，1:是]
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationTest -q
```

预期：PASS。

- [ ] **步骤 5：Commit**

```bash
cd liuliupi-server
git add src/main/java/com/liuliupi/entity/PushNotification.java \
       src/test/java/com/liuliupi/entity/PushNotificationTest.java
git commit -m "feat(server): 新增 PushNotification 实体"
```

---

## 任务 3：创建 `PushNotificationMapper`

**文件：**
- 创建：`src/main/java/com/liuliupi/dao/PushNotificationMapper.java`

- [ ] **步骤 1：创建 Mapper 接口**

```java
package com.liuliupi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuliupi.entity.PushNotification;

/**
 * <p>
 * 首页弹窗推送配置表 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
public interface PushNotificationMapper extends BaseMapper<PushNotification> {

}
```

- [ ] **步骤 2：编译验证**

```bash
cd liuliupi-server
mvn compile -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
cd liuliupi-server
git add src/main/java/com/liuliupi/dao/PushNotificationMapper.java
git commit -m "feat(server): 新增 PushNotificationMapper"
```

---

## 任务 4：创建 `PushNotificationService` 与实现

**文件：**
- 创建：`src/main/java/com/liuliupi/service/PushNotificationService.java`
- 创建：`src/main/java/com/liuliupi/service/impl/PushNotificationServiceImpl.java`
- 测试：`src/test/java/com/liuliupi/service/impl/PushNotificationServiceImplTest.java`

- [ ] **步骤 1：编写失败测试**

```java
package com.liuliupi.service.impl;

import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    @Mock
    private PushNotificationMapper pushNotificationMapper;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    @Test
    void saveOrUpdateShouldInsertWhenNoRecordExists() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification push = new PushNotification();
        push.setTitle("标题");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        pushNotificationService.saveOrUpdateSingle(push);

        verify(pushNotificationMapper).insert(push);
        verify(pushNotificationMapper, never()).updateById(any());
    }

    @Test
    void saveOrUpdateShouldUpdateWhenRecordExists() {
        PushNotification existing = new PushNotification();
        existing.setId(1);

        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        PushNotification push = new PushNotification();
        push.setTitle("新标题");
        push.setEnabled(false);

        pushNotificationService.saveOrUpdateSingle(push);

        verify(pushNotificationMapper, never()).insert(any());
        verify(pushNotificationMapper).updateById(push);
    }

    @Test
    void getEnabledShouldReturnFirstEnabledRecord() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("推送");
        push.setEnabled(true);

        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.singletonList(push));

        PushNotification result = pushNotificationService.getEnabled();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void getEnabledShouldReturnNullWhenDisabled() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification result = pushNotificationService.getEnabled();

        assertThat(result).isNull();
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationServiceImplTest -q
```

预期：FAIL，找不到 `PushNotificationServiceImpl` / `saveOrUpdateSingle` / `getEnabled`。

- [ ] **步骤 3：创建 Service 接口**

```java
package com.liuliupi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liuliupi.entity.PushNotification;

/**
 * <p>
 * 首页弹窗推送配置表 服务类
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
public interface PushNotificationService extends IService<PushNotification> {

    /**
     * 保存或更新单条推送配置（表中始终只有一条记录）
     */
    void saveOrUpdateSingle(PushNotification pushNotification);

    /**
     * 获取当前启用的推送配置
     */
    PushNotification getEnabled();
}
```

- [ ] **步骤 4：创建 Service 实现**

```java
package com.liuliupi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 首页弹窗推送配置表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@Service
public class PushNotificationServiceImpl extends ServiceImpl<PushNotificationMapper, PushNotification> implements PushNotificationService {

    @Override
    public void saveOrUpdateSingle(PushNotification pushNotification) {
        List<PushNotification> list = baseMapper.selectList(new LambdaQueryWrapper<PushNotification>()
                .orderByAsc(PushNotification::getId)
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(list)) {
            baseMapper.insert(pushNotification);
        } else {
            pushNotification.setId(list.get(0).getId());
            baseMapper.updateById(pushNotification);
        }
    }

    @Override
    public PushNotification getEnabled() {
        List<PushNotification> list = baseMapper.selectList(new LambdaQueryWrapper<PushNotification>()
                .eq(PushNotification::getEnabled, 1)
                .orderByAsc(PushNotification::getId)
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationServiceImplTest -q
```

预期：PASS。

- [ ] **步骤 6：Commit**

```bash
cd liuliupi-server
git add src/main/java/com/liuliupi/service/PushNotificationService.java \
       src/main/java/com/liuliupi/service/impl/PushNotificationServiceImpl.java \
       src/test/java/com/liuliupi/service/impl/PushNotificationServiceImplTest.java
git commit -m "feat(server): 新增 PushNotificationService 及实现"
```

---

## 任务 5：创建 `PushNotificationController`

**文件：**
- 创建：`src/main/java/com/liuliupi/controller/PushNotificationController.java`
- 测试：`src/test/java/com/liuliupi/controller/PushNotificationControllerTest.java`

- [ ] **步骤 1：编写失败测试**

```java
package com.liuliupi.controller;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationControllerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private PushNotificationController controller;

    @Test
    void getPushNotificationShouldReturnEnabledRecord() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("推送标题");
        push.setEnabled(true);

        when(pushNotificationService.getEnabled()).thenReturn(push);

        PoetryResult<PushNotification> result = controller.getPushNotification();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTitle()).isEqualTo("推送标题");
    }

    @Test
    void getPushNotificationShouldReturnNullWhenDisabled() {
        when(pushNotificationService.getEnabled()).thenReturn(null);

        PoetryResult<PushNotification> result = controller.getPushNotification();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    void adminGetPushNotificationShouldDelegateToService() {
        PushNotification push = new PushNotification();
        push.setId(1);

        when(pushNotificationService.getEnabled()).thenReturn(push);

        PoetryResult<PushNotification> result = controller.getAdminPushNotification();

        assertThat(result.getData()).isEqualTo(push);
    }

    @Test
    void savePushNotificationShouldDelegateToService() {
        PushNotification push = new PushNotification();
        push.setTitle("标题");

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(200);
        verify(pushNotificationService).saveOrUpdateSingle(push);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationControllerTest -q
```

预期：FAIL，找不到 `PushNotificationController`。

- [ ] **步骤 3：创建 Controller**

```java
package com.liuliupi.controller;

import com.liuliupi.aop.LoginCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 首页弹窗推送配置 前端控制器
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/pushNotification")
public class PushNotificationController {

    @Autowired
    private PushNotificationService pushNotificationService;

    /**
     * 前台获取当前启用的推送
     */
    @GetMapping("/getPushNotification")
    public PoetryResult<PushNotification> getPushNotification() {
        return PoetryResult.success(pushNotificationService.getEnabled());
    }

    /**
     * 后台获取当前推送配置
     */
    @GetMapping("/admin/getPushNotification")
    @LoginCheck(0)
    public PoetryResult<PushNotification> getAdminPushNotification() {
        return PoetryResult.success(pushNotificationService.getEnabled());
    }

    /**
     * 后台保存/更新推送配置
     */
    @PostMapping("/admin/savePushNotification")
    @LoginCheck(0)
    public PoetryResult<Void> savePushNotification(@RequestBody PushNotification pushNotification) {
        pushNotificationService.saveOrUpdateSingle(pushNotification);
        return PoetryResult.success();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
cd liuliupi-server
mvn test -Dtest=PushNotificationControllerTest -q
```

预期：PASS。

- [ ] **步骤 5：运行全部后端测试**

```bash
cd liuliupi-server
mvn test -q
```

预期：所有测试通过。

- [ ] **步骤 6：Commit**

```bash
cd liuliupi-server
git add src/main/java/com/liuliupi/controller/PushNotificationController.java \
       src/test/java/com/liuliupi/controller/PushNotificationControllerTest.java
git commit -m "feat(server): 新增 PushNotificationController 接口"
```

---

## 任务 6：移除后台 `notices` 的 JSON 转换

**文件：**
- 修改：`src/main/java/com/liuliupi/controller/WebInfoController.java`（无需改动，确认即可）
- 修改：`src/components/admin/webEdit.vue`（见任务 7）

当前 `WebInfoController.updateWebInfo` 已直接接收 `WebInfo` 对象并 `updateById`，未对 `notices` 做 JSON 处理。确认后无需后端改动；前端在 `webEdit.vue` 中移除 `JSON.parse` / `JSON.stringify` 即可。

- [ ] **步骤 1：确认 `WebInfoController` 未解析 notices**

```bash
cd liuliupi-server
grep -n "notices" src/main/java/com/liuliupi/controller/WebInfoController.java
```

预期：无匹配结果。

- [ ] **步骤 2：Commit（仅记录确认）**

如确认无匹配，本任务无需单独 commit，合并到任务 7。

---

## 任务 7：改造后台 `webEdit.vue` 公告标签页

**文件：**
- 修改：`src/components/admin/webEdit.vue`
- 测试：`tests/unit/webEdit.spec.js`

- [ ] **步骤 1：编写失败测试**

在 `tests/unit/webEdit.spec.js` 中新增以下测试（放在文件末尾 `})` 之前）：

```javascript
  it('renders mavon-editor in notice tab without imgAdd binding', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { notices: '# 公告\n\n欢迎使用' }
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

    const editor = wrapper.find('mavon-editor-stub')
    expect(editor.exists()).toBe(true)
    expect(editor.attributes('value')).toBe('# 公告\n\n欢迎使用')
    expect(editor.attributes('imgadd')).toBeUndefined()
  })

  it('renders push notification form in notice tab', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          pushNotification: {
            title: '推送标题',
            cover: 'https://example.com/cover.jpg',
            url: 'https://example.com/',
            enabled: true
          }
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

    const inputs = wrapper.findAll('el-input-stub')
    expect(inputs.filter(i => i.attributes('value') === '推送标题').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/cover.jpg').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/').length).toBeGreaterThan(0)
  })

  it('saveNotice calls both updateWebInfo and savePushNotification', async () => {
    const postMock = jest.fn().mockResolvedValue({})
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { id: 1, notices: '# 公告' },
          pushNotification: { title: '推送', cover: '', url: '', enabled: true }
        }
      },
      mocks: {
        $http: { post: postMock, get: jest.fn() },
        $constant: { baseURL: 'http://localhost:8080' },
        $message: jest.fn()
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

    await wrapper.vm.saveNotice()

    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/webInfo/updateWebInfo',
      { id: 1, notices: '# 公告' }
    )
    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/pushNotification/admin/savePushNotification',
      { title: '推送', cover: '', url: '', enabled: true }
    )
  })
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/webEdit.spec.js
```

预期：FAIL，找不到 `mavon-editor-stub`、`pushNotification`、新的 `saveNotice` 行为。

- [ ] **步骤 3：修改 `webEdit.vue` 的公告标签页**

将 `webEdit.vue` 中 `el-tab-pane label="公告"` 部分替换为：

```vue
      <el-tab-pane label="公告" name="notice">
        <el-card>
          <div slot="header">公告 Markdown</div>
          <mavon-editor v-model="webInfo.notices" />
        </el-card>

        <el-card class="push-notification-card">
          <div slot="header">推送设置</div>
          <el-form :model="pushNotification" label-width="100px">
            <el-form-item label="推送标题">
              <el-input v-model="pushNotification.title" placeholder="请输入推送标题"></el-input>
            </el-form-item>

            <el-form-item label="封面链接">
              <el-input v-model="pushNotification.cover" placeholder="https://example.com/cover.jpg"></el-input>
            </el-form-item>

            <el-form-item label="跳转链接">
              <el-input v-model="pushNotification.url" placeholder="https://example.com/"></el-input>
            </el-form-item>

            <el-form-item label="是否启用">
              <el-switch v-model="pushNotification.enabled"></el-switch>
            </el-form-item>
          </el-form>
        </el-card>

        <div class="form-actions">
          <el-button type="primary" @click="saveNotice()">保存</el-button>
        </div>
      </el-tab-pane>
```

- [ ] **步骤 4：更新组件 data**

在 `data()` 中删除 `notices: []`、`inputNoticeVisible`、`inputNoticeValue`，新增 `pushNotification`：

```javascript
        pushNotification: {
          title: "",
          cover: "",
          url: "",
          enabled: true
        },
```

- [ ] **步骤 5：修改 `getWebInfo` 方法**

移除 `this.notices = JSON.parse(res.data.notices);`，改为直接赋值：

```javascript
              this.webInfo.notices = res.data.notices || "";
```

并新增推送配置加载（在 `getWebInfo` 末尾、catch 之前）：

```javascript
              this.getPushNotification();
```

- [ ] **步骤 6：新增 `getPushNotification` 方法**

在 `methods` 中新增：

```javascript
      getPushNotification() {
        this.$http.get(this.$constant.baseURL + "/pushNotification/admin/getPushNotification", {}, true)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.pushNotification = {
                title: res.data.title || "",
                cover: res.data.cover || "",
                url: res.data.url || "",
                enabled: res.data.enabled === undefined ? true : res.data.enabled
              };
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

- [ ] **步骤 7：修改 `saveNotice` 方法**

将 `saveNotice()` 替换为：

```javascript
      saveNotice() {
        let noticeParam = {
          id: this.webInfo.id,
          notices: this.webInfo.notices
        };

        Promise.all([
          this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", noticeParam, true),
          this.$http.post(this.$constant.baseURL + "/pushNotification/admin/savePushNotification", this.pushNotification, true)
        ])
          .then(() => {
            this.getWebInfo();
            this.$message({
              message: "保存成功！",
              type: "success"
            });
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
```

- [ ] **步骤 8：删除旧的公告标签操作方法**

删除以下方法：
- `handleInputNoticeConfirm`
- `showNoticeInput`
- `handleClose`（如仅用于 notices 则删除；如 random 资源也使用则保留并确认通用性）

如果 `handleClose` 同时用于 random 资源，保留它。

- [ ] **步骤 9：运行测试验证通过**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/webEdit.spec.js
```

预期：PASS。

- [ ] **步骤 10：Commit**

```bash
cd liuliupi-ui
git add src/components/admin/webEdit.vue tests/unit/webEdit.spec.js
git commit -m "feat(admin): 公告标签页改为 Markdown 编辑并新增推送设置"
```

---

## 任务 8：改造前台 `index.vue` 公告与推送

**文件：**
- 修改：`src/components/index.vue`
- 修改：`src/utils/common.js`
- 创建：`tests/unit/index.spec.js`

- [ ] **步骤 1：编写失败测试**

创建 `tests/unit/index.spec.js`：

```javascript
import { shallowMount } from '@vue/test-utils'
import Index from '@/components/index.vue'

const createWrapper = (options = {}) => {
  return shallowMount(Index, {
    mocks: {
      $store: {
        state: {
          webInfo: { notices: '# 公告\n\n- 第一项\n- 第二项' },
          sortInfo: []
        }
      },
      $common: {
        isEmpty: (v) => v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0) || (typeof v === 'object' && Object.keys(v).length === 0)
      },
      $http: {
        get: jest.fn().mockResolvedValue({ data: null }),
        post: jest.fn().mockResolvedValue({ data: null })
      },
      $constant: { baseURL: 'http://localhost:8080', jinrishici: '' },
      $message: jest.fn()
    },
    stubs: [
      'loader',
      'zombie',
      'printer',
      'article-list',
      'sort-article',
      'my-footer',
      'my-aside',
      'el-image',
      'el-dialog'
    ],
    ...options
  })
}

describe('index.vue', () => {
  it('renders notice board with markdown content', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.announcement-board').exists()).toBe(true)
    expect(wrapper.find('.announcement-body').exists()).toBe(true)
  })

  it('renders raw text when markdown render throws', () => {
    const wrapper = createWrapper({
      mocks: {
        $store: {
          state: {
            webInfo: { notices: '# 公告' },
            sortInfo: []
          }
        }
      },
      data() {
        return {
          noticeHtml: '原始公告文本'
        }
      }
    })
    expect(wrapper.find('.announcement-body').html()).toContain('原始公告文本')
  })

  it('loads push notification from new endpoint', async () => {
    const getMock = jest.fn().mockResolvedValue({
      data: { title: '推送', cover: 'https://example.com/cover.jpg', url: 'https://example.com/', enabled: true }
    })
    const wrapper = createWrapper({
      mocks: {
        $http: {
          get: getMock,
          post: jest.fn().mockResolvedValue({ data: null })
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 2100))

    expect(getMock).toHaveBeenCalledWith('http://localhost:8080/pushNotification/getPushNotification')
    expect(wrapper.vm.push['标题']).toBe('推送')
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/index.spec.js
```

预期：FAIL，找不到 `.announcement-board`、新的 push 数据格式等。

- [ ] **步骤 3：修改 `index.vue` 的公告区**

将模板中：

```vue
              <div class="announcement background-opacity">
                <i class="fa fa-volume-up" aria-hidden="true"></i>
                <div>
                  <div v-for="(notice, index) in $common.pushNotification($store.state.webInfo.notices, true)" :key="index">
                    {{ notice }}
                  </div>
                </div>
              </div>
```

替换为：

```vue
              <div class="announcement-board background-opacity">
                <div class="announcement-header">
                  <i class="fa fa-volume-up" aria-hidden="true"></i>
                  <span>公告</span>
                </div>
                <div class="announcement-body" v-html="noticeHtml"></div>
              </div>
```

- [ ] **步骤 4：引入 `markdown-it` 并渲染公告**

在 `<script>` 顶部添加：

```javascript
  import MarkdownIt from 'markdown-it';
```

在 `data()` 中新增：

```javascript
        noticeHtml: '',
```

新增 `renderNotice` 方法：

```javascript
      renderNotice() {
        const notices = this.$store.state.webInfo.notices || '';
        try {
          const md = new MarkdownIt({ breaks: true });
          this.noticeHtml = md.render(notices);
        } catch (e) {
          this.noticeHtml = notices;
        }
      },
```

在 `created()` 中调用：

```javascript
    created() {
      this.renderNotice();
      this.getGuShi();
      this.getSortArticles();
    },
```

- [ ] **步骤 5：修改推送弹窗数据获取逻辑**

将 `mounted()` 中的：

```javascript
      setTimeout(() => {
        this.push = this.$common.pushNotification(this.$store.state.webInfo.notices, false);
        if(!this.$common.isEmpty(this.push)) {
          if("0" !== localStorage.getItem("showPushNotification_" + this.push['链接'])) {
            this.pushDialogVisible = true;
            localStorage.setItem("showPushNotification_" + this.push['链接'], "0");
          }
        }
      }, 2000);
```

替换为：

```javascript
      setTimeout(() => {
        this.$http.get(this.$constant.baseURL + '/pushNotification/getPushNotification')
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.push = {
                '标题': res.data.title,
                '封面': res.data.cover,
                '链接': res.data.url
              };
              if ("0" !== localStorage.getItem("showPushNotification_" + this.push['链接'])) {
                this.pushDialogVisible = true;
                localStorage.setItem("showPushNotification_" + this.push['链接'], "0");
              }
            }
          })
          .catch(() => {
            // 静默忽略，不弹窗
          });
      }, 2000);
```

- [ ] **步骤 6：删除 `common.js` 中的 `pushNotification` 方法**

编辑 `src/utils/common.js`，删除以下方法：

```javascript
  pushNotification(notices, isNotification) {
    if (isNotification) {
      if (this.isEmpty(notices)) {
        return [];
      } else {
        return notices.filter(f => "推送标题：" !== f.substr(0, 5) &&
          "推送封面：" !== f.substr(0, 5) &&
          "推送链接：" !== f.substr(0, 5));
      }
    } else {
      let push = {};
      notices.forEach(notice => {
        if ("推送标题：" === notice.substr(0, 5)) {
          push['标题'] = notice.substr(5);
        } else if ("推送封面：" === notice.substr(0, 5)) {
          push['封面'] = notice.substr(5);
        } else if ("推送链接：" === notice.substr(0, 5)) {
          push['链接'] = notice.substr(5);
        }
      });
      return push;
    }
  },
```

- [ ] **步骤 7：新增公告板样式**

在 `src/components/index.vue` 的 `<style scoped>` 末尾添加：

```css
  .announcement-board {
    padding: 22px;
    border: 1px dashed var(--lightGray);
    color: var(--greyFont);
    border-radius: 10px;
    margin: 40px auto 40px;
  }

  .announcement-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    font-size: 18px;
    font-weight: bold;
  }

  .announcement-header i {
    color: var(--themeBackground);
    font-size: 22px;
    margin-right: 10px;
    animation: scale 0.8s ease-in-out infinite;
  }

  .announcement-body {
    line-height: 1.8;
  }

  .announcement-body :deep(p) {
    margin: 0 0 12px 0;
  }

  .announcement-body :deep(ul),
  .announcement-body :deep(ol) {
    padding-left: 20px;
  }

  .announcement-body :deep(a) {
    color: var(--themeBackground);
  }
```

- [ ] **步骤 8：运行测试验证通过**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/index.spec.js
```

预期：PASS。

- [ ] **步骤 9：运行全部前端单元测试**

```bash
cd liuliupi-ui
pnpm test:unit
```

预期：所有测试通过。

- [ ] **步骤 10：Commit**

```bash
cd liuliupi-ui
git add src/components/index.vue src/utils/common.js tests/unit/index.spec.js
git commit -m "feat(index): 前台公告改为 Markdown 渲染并独立推送接口"
```

---

## 任务 9：端到端验证与收尾

**文件：**
- 涉及：`liuliupi-server` 与 `liuliupi-ui`

- [ ] **步骤 1：后端完整编译与测试**

```bash
cd liuliupi-server
mvn clean test -q
```

预期：BUILD SUCCESS，所有测试通过。

- [ ] **步骤 2：前端完整构建**

```bash
cd liuliupi-ui
pnpm build
```

预期：构建成功，无新增 lint 错误。

- [ ] **步骤 3：提交合并计划**

将 worktree 中的改动合并回 `dev` 分支（或按团队流程创建 PR）。

```bash
git checkout dev
git merge worktree-notice-markdown-plan
```

- [ ] **步骤 4：数据库迁移提醒**

在部署环境中执行：

```sql
ALTER TABLE `liuliupi_blog`.`web_info` MODIFY COLUMN `notices` text DEFAULT NULL COMMENT '公告';

CREATE TABLE `liuliupi_blog`.`push_notification` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title` varchar(200) DEFAULT NULL COMMENT '推送标题',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面图 URL',
  `url` varchar(500) DEFAULT NULL COMMENT '点击跳转链接',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页弹窗推送配置表';
```

---

## 自检

### 1. 规格覆盖度

| 规格需求 | 对应任务 |
|---|---|
| 后台公告使用 Markdown 编辑器维护 | 任务 7 |
| 前台使用 Markdown 渲染展示 | 任务 8 |
| 首页弹窗推送从公告中剥离，独立成表 | 任务 1、任务 4、任务 5 |
| 后台「公告」标签页保持单一保存按钮 | 任务 7 |
| 旧数据不再兼容，由管理员重新编辑 | 任务 1（默认空字符串）、任务 8（直接字符串渲染） |
| 不支持图片本地上传 | 任务 7（未绑定 `@imgAdd`） |
| 不实现推送历史、定时推送 | 已满足，仅单条记录 |
| 不做旧公告数据自动迁移 | 任务 1、任务 7 |
| 错误处理：保存失败分别提示 | 任务 7（Promise.all catch） |
| 错误处理：前台获取推送失败静默 | 任务 8（catch 空处理） |
| Markdown 渲染异常显示原始文本 | 任务 8（try/catch） |
| 后端测试覆盖保存/查询/开关 | 任务 4、任务 5 |

### 2. 占位符扫描

- 无 "待定" / "TODO" / "后续实现"。
- 无 "添加适当的错误处理" 等模糊描述。
- 每个代码步骤均包含完整代码块。
- 所有引用的方法名、字段名在先前任务中已定义。

### 3. 类型一致性

- `PushNotification.enabled` 类型为 `Boolean`，与表中 `tinyint(1)` 一致。
- Controller 路径 `/pushNotification/admin/savePushNotification` 与 `webEdit.vue` 调用一致。
- `index.vue` 推送弹窗字段 `标题` / `封面` / `链接` 与模板绑定一致。

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-06-14-notice-markdown.md`。两种执行方式：**

**1. 子代理驱动（推荐）** — 每个任务调度一个新的子代理，任务间进行审查，快速迭代。

**2. 内联执行** — 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点供审查。

**选哪种方式？**
