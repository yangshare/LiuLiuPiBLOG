# Admin 统计信息页布局优化设计

日期：2026-06-14  
模块：liuliupi-ui / `src/components/admin/main.vue`  
状态：待实现

---

## 1. 背景与目标

当前统计信息页采用纵向平铺结构：「总览 → 今日访问 → 昨日访问」依次堆叠，存在以下问题：

- 视觉层级弱：三个 section 使用同等大小的绿色标题按钮，难以区分主次。
- 空间利用率低：PC 端大量水平空间闲置，关键数据没有形成视觉焦点。
- 信息密度不均：历史总览（18883）与今日（6）、昨日（4）占用几乎相同的垂直空间。
- 表格局促：省份 / IP 列宽 140px，长 IP 容易换行，数字列留白过多。
- 昨日板块太空：只有用户列表，左右大片空白。

**目标**：将页面改造为卡片式布局，PC 横向一屏展示，手机自动竖排，提升信息层级与可读性。

---

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| 布局骨架 | 左侧总览大卡片 + 右侧今日 / 昨日双卡 | 总访问量作为核心指标需要最大展示面积；今日 / 昨日作为时间切片对称放置 |
| 响应式策略 | PC 横向双栏，手机竖排 | 满足「PC 横向就可以展示完，手机才是竖向布局」的要求 |
| 省份数据展示 | echarts 中国地图 | 用户明确要求可视化地图 |
| IP 数据展示 | 保留 el-table TOP10 | 精确数据仍然需要表格 |
| 今日 / 昨日内容 | 仅 KPI + 访问用户列表 | 用户确认去掉省份统计，保持右侧轻盈 |
| 卡片容器 | Element UI `el-card` | 与项目现有 Element UI 风格一致 |

---

## 3. 详细设计

### 3.1 整体布局

#### PC 端（≥992px）

```
┌─────────────────────────────────────────────────────────────┐
│ ⚙️ 统计信息                                                   │
├───────────────────────────────────────┬─────────────────────┤
│                                       │   今日访问           │
│              总 览                    │   ┌─────────────┐   │
│   ┌───────────────────────────────┐   │   │   KPI: 6    │   │
│   │  总访问量 KPI                 │   │   │  用户列表    │   │
│   │  ┌──────────┐ ┌───────────┐   │   │   └─────────────┘   │
│   │  │ 中国地图  │ │ IP TOP10  │   │   ├─────────────────────┤
│   │  │          │ │  表格     │   │   │   昨日访问           │
│   │  └──────────┘ └───────────┘   │   │   ┌─────────────┐   │
│   └───────────────────────────────┘   │   │   KPI: 4    │   │
│                                       │   │  用户列表    │   │
└───────────────────────────────────────┴───┴─────────────┴───┘
```

- 左侧列宽占比 2/3，右侧列宽占比 1/3。
- 左侧「总览」卡片跨两行，与右侧两张卡片整体等高。
- 左侧内部：顶部居中 KPI，下方地图与 IP 表格左右并排。

#### 移动端（＜768px）

三个卡片垂直堆叠：

1. 总览（KPI + 地图 + IP 表格）
2. 今日访问（KPI + 用户列表）
3. 昨日访问（KPI + 用户列表）

卡片间距从 PC 的 `16px` 缩小为 `12px`，KPI 字号适度缩小，表格允许横向滚动。

### 3.2 组件拆分

```
AdminMain (components/admin/main.vue)
├── 页面标题栏
├── el-row / el-col 响应式外壳
├── OverviewCard
│   ├── StatKpi（复用）
│   ├── ProvinceMap（echarts 中国地图，新增子组件）
│   └── IpTop10Table（el-table）
├── TodayCard
│   ├── StatKpi（复用）
│   └── VisitorList（el-table + el-avatar）
└── YesterdayCard
    ├── StatKpi（复用）
    └── VisitorList（el-table + el-avatar）
```

**组件说明**：

- `StatKpi`：复用的 KPI 数字组件， props：`label`、`value`、`suffix`。
- `ProvinceMap`：封装 echarts 实例，接收 `provinceData` 数组，渲染中国地图，加载失败时触发 `error` 事件。
- `VisitorList`：复用的访问用户列表，props：`users`。

### 3.3 数据接口

复用现有接口：

```http
GET /webInfo/getHistoryInfo
```

返回字段使用：

| 字段 | 类型 | 用途 |
|---|---|---|
| `ip_history_count` | number | 左侧总访问量 KPI |
| `ip_history_province` | `{province: string, num: number}[]` | 省份地图数据 |
| `ip_history_ip` | `{ip: string, num: number}[]` | IP TOP10 表格 |
| `ip_count_today` | number | 今日访问量 KPI |
| `username_today` | `{username: string, avatar: string}[]` | 今日访问用户 |
| `ip_count_yest` | number | 昨日访问量 KPI |
| `username_yest` | `{username: string, avatar: string}[]` | 昨日访问用户 |

`province_today` 不再使用，可以保留但不做展示。

### 3.4 视觉风格

- 页面标题栏保留现有黄色背景 `var(--lightYellow)` 和齿轮图标。
- 卡片使用 `el-card`，标题使用 `header` 插槽，字体加粗。
- KPI 数字颜色继续使用 `var(--maxLightRed)`，保持品牌一致性。
- 地图颜色从浅到深按访问量映射，Tooltip 显示省份名称和数量。
- 表格列宽优化：IP 列加宽到 160px，数量列居中 80px。

### 3.5 响应式栅格

```vue
<el-row :gutter="16">
  <el-col :xs="24" :sm="24" :md="16" :lg="16">
    <OverviewCard />
  </el-col>
  <el-col :xs="24" :sm="24" :md="8" :lg="8">
    <TodayCard />
    <YesterdayCard />
  </el-col>
</el-row>
```

---

## 4. 边界情况与错误处理

| 场景 | 处理方案 |
|---|---|
| 数据加载中 | 三个卡片外层使用 `v-loading`，统一显示加载动画 |
| 省份数据为空 | 地图区域显示「暂无省份数据」占位 |
| 地图 GeoJSON 加载失败 | 降级为「省份 TOP10 紧凑表格」，保证核心信息可读 |
| 今日 / 昨日无用户 | 用户列表位置显示 `el-empty` |
| IP 数据为空 | 表格显示 `el-table` 默认空数据 |
| API 请求失败 | 保持现有 `$message.error(error.message)` |

---

## 5. 依赖与风险

### 新增依赖

- `echarts@^5.x`：用于中国地图可视化。
- 中国地图 GeoJSON：推荐将 `node_modules/echarts/map/json/china.json` 复制到项目 `public/geo/china.json`，通过 `fetch('/geo/china.json')` 加载；避免 echarts 内置地图路径在打包后失效。

### 组件文件位置

- `liuliupi-ui/src/components/admin/main.vue`：主页面，负责布局和接口调用。
- `liuliupi-ui/src/components/admin/stats/OverviewCard.vue`：总览卡片。
- `liuliupi-ui/src/components/admin/stats/TodayCard.vue`：今日访问卡片。
- `liuliupi-ui/src/components/admin/stats/YesterdayCard.vue`：昨日访问卡片。
- `liuliupi-ui/src/components/admin/stats/ProvinceMap.vue`：echarts 中国地图。
- `liuliupi-ui/src/components/admin/stats/StatKpi.vue`：复用 KPI 组件。
- `liuliupi-ui/src/components/admin/stats/VisitorList.vue`：复用用户列表。

### 风险与缓解

| 风险 | 缓解措施 |
|---|---|
| 中国地图 GeoJSON 加载失败 | 实现 fallback 表格；将 GeoJSON 文件本地静态化 |
| 地图包体积增大 | 仅按需引入 echarts 地图模块；生产环境 gzip |
| 移动端地图可点击区域过小 | 地图容器高度在手机端不小于 200px；Tooltip 放大字号 |
| 省份名称与 GeoJSON 命名不一致 | 数据映射时统一使用「省/市」后缀处理 |

---

## 6. 测试策略

- **渲染测试**：验证三个卡片标题、KPI 数字、表格列是否正确渲染。
- **数据转换测试**：验证 `ip_history_province` 是否正确转换为 echarts 地图所需格式。
- **降级测试**：模拟地图加载失败，验证是否渲染 fallback 表格。
- **空状态测试**：模拟今日 / 昨日用户为空，验证 `el-empty` 是否出现。
- **响应式测试**：断言不同断点下 `el-col` 的类名（可通过组件 props 或 DOM class 检查）。

---

## 7. 实现检查清单

- [ ] 安装 `echarts` 并引入中国地图 GeoJSON
- [ ] 重构 `main.vue` 为卡片式响应式布局
- [ ] 新增 / 拆分 `ProvinceMap`、`StatKpi`、`VisitorList` 组件
- [ ] 实现地图加载失败 fallback
- [ ] 补充单元测试
- [ ] 在真机 / 浏览器 DevTools 验证移动端效果
- [ ] 运行 UI 测试并通过

---

## 8. 参考文件

- 当前实现：`liuliupi-ui/src/components/admin/main.vue`
- 视觉伴侣原型：`/LiuLiuPiBLOG/.superpowers/brainstorm/350-1781433013/content/`
