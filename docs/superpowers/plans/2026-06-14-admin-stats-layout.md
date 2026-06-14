# Admin 统计信息页卡片式布局实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 `liuliupi-ui/src/components/admin/main.vue` 从纵向平铺结构改造为卡片式响应式布局：PC 横向展示左侧「总览」大卡片 + 右侧「今日 / 昨日」双卡；手机竖排堆叠；左侧引入 echarts 中国地图展示省份访问分布。

**架构：** 主页面负责布局和接口调用，拆分为 `StatKpi`、`VisitorList`、`ProvinceMap`、`OverviewCard`、`TodayCard`、`YesterdayCard` 六个子组件；echarts 中国地图 GeoJSON 放在 `public/geo/china.json` 本地静态加载并降级；复用现有 `GET /webInfo/getHistoryInfo` 接口。

**技术栈：** Vue 2.7 + Element UI 2.15 + echarts 5 + @vue/test-utils 1.3 + jest

---

## 文件结构

| 文件 | 类型 | 职责 |
|---|---|---|
| `liuliupi-ui/package.json` | 修改 | 新增 `echarts` 依赖 |
| `liuliupi-ui/public/geo/china.json` | 创建 | 中国地图 GeoJSON 静态资源 |
| `liuliupi-ui/src/components/admin/stats/StatKpi.vue` | 创建 | 复用 KPI 数字组件 |
| `liuliupi-ui/src/components/admin/stats/VisitorList.vue` | 创建 | 复用访问用户列表 |
| `liuliupi-ui/src/components/admin/stats/ProvinceMap.vue` | 创建 | echarts 中国地图组件 |
| `liuliupi-ui/src/components/admin/stats/OverviewCard.vue` | 创建 | 左侧总览卡片 |
| `liuliupi-ui/src/components/admin/stats/TodayCard.vue` | 创建 | 右侧今日访问卡片 |
| `liuliupi-ui/src/components/admin/stats/YesterdayCard.vue` | 创建 | 右侧昨日访问卡片 |
| `liuliupi-ui/src/components/admin/main.vue` | 修改 | 重构为响应式卡片布局外壳 |
| `liuliupi-ui/tests/unit/main.spec.js` | 创建 | 主页面与子组件单元测试 |

---

## 任务 1：安装 echarts 依赖

**文件：**
- 修改：`liuliupi-ui/package.json`

- [ ] **步骤 1：安装 echarts**

```bash
cd liuliupi-ui
pnpm add echarts
```

- [ ] **步骤 2：验证安装**

```bash
grep '"echarts"' package.json
```

预期输出包含：
```json
"echarts": "^5.x.x"
```

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-ui/package.json
git commit -m "chore(ui): 安装 echarts 用于中国地图可视化"
```

---

## 任务 2：准备中国地图 GeoJSON

**文件：**
- 创建：`liuliupi-ui/public/geo/china.json`

- [ ] **步骤 1：从 echarts 包复制 GeoJSON**

```bash
mkdir -p liuliupi-ui/public/geo
cp liuliupi-ui/node_modules/echarts/map/json/china.json liuliupi-ui/public/geo/china.json
```

- [ ] **步骤 2：验证文件存在且非空**

```bash
test -s liuliupi-ui/public/geo/china.json && echo "ok"
```

预期输出：`ok`

- [ ] **步骤 3：Commit**

```bash
git add liuliupi-ui/public/geo/china.json
git commit -m "chore(ui): 添加中国地图 GeoJSON 静态资源"
```

---

## 任务 3：创建 StatKpi 组件

**文件：**
- 创建：`liuliupi-ui/src/components/admin/stats/StatKpi.vue`
- 创建：`liuliupi-ui/tests/unit/stats/StatKpi.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/stats/StatKpi.spec.js
import { shallowMount } from '@vue/test-utils'
import StatKpi from '@/components/admin/stats/StatKpi.vue'

describe('StatKpi.vue', () => {
  it('renders label and value', () => {
    const wrapper = shallowMount(StatKpi, {
      propsData: {
        label: '总访问量',
        value: 18883
      }
    })
    expect(wrapper.text()).toContain('总访问量')
    expect(wrapper.text()).toContain('18883')
  })

  it('renders suffix when provided', () => {
    const wrapper = shallowMount(StatKpi, {
      propsData: {
        label: '今日访问量',
        value: 6,
        suffix: '次'
      }
    })
    expect(wrapper.text()).toContain('次')
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd liuliupi-ui
pnpm test:unit tests/unit/stats/StatKpi.spec.js
```

预期：FAIL，报错 `Cannot find module '@/components/admin/stats/StatKpi.vue'`

- [ ] **步骤 3：编写 StatKpi 组件**

```vue
<template>
  <div class="stat-kpi">
    <div class="stat-kpi__label">{{ label }}</div>
    <div class="stat-kpi__value">
      {{ value }}<span v-if="suffix" class="stat-kpi__suffix">{{ suffix }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StatKpi',
  props: {
    label: {
      type: String,
      required: true
    },
    value: {
      type: [Number, String],
      required: true
    },
    suffix: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.stat-kpi {
  text-align: center;
}
.stat-kpi__label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}
.stat-kpi__value {
  color: var(--maxLightRed);
  font-size: 28px;
  font-weight: bold;
  line-height: 1.2;
}
.stat-kpi__suffix {
  font-size: 14px;
  margin-left: 2px;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

```bash
pnpm test:unit tests/unit/stats/StatKpi.spec.js
```

预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/stats/StatKpi.vue liuliupi-ui/tests/unit/stats/StatKpi.spec.js
git commit -m "feat(ui): 添加 StatKpi 组件"
```

---

## 任务 4：创建 VisitorList 组件

**文件：**
- 创建：`liuliupi-ui/src/components/admin/stats/VisitorList.vue`
- 创建：`liuliupi-ui/tests/unit/stats/VisitorList.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/stats/VisitorList.spec.js
import { shallowMount } from '@vue/test-utils'
import VisitorList from '@/components/admin/stats/VisitorList.vue'

describe('VisitorList.vue', () => {
  it('renders user list', () => {
    const users = [
      { username: 'alice', avatar: 'https://example.com/a.jpg' },
      { username: 'bob', avatar: 'https://example.com/b.jpg' }
    ]
    const wrapper = shallowMount(VisitorList, {
      propsData: { users },
      stubs: ['el-table', 'el-table-column', 'el-avatar', 'el-empty']
    })
    expect(wrapper.findAll('el-table-column-stub')).toHaveLength(2)
  })

  it('shows empty state when no users', () => {
    const wrapper = shallowMount(VisitorList, {
      propsData: { users: [] },
      stubs: ['el-table', 'el-table-column', 'el-avatar', 'el-empty']
    })
    expect(wrapper.find('el-empty-stub').exists()).toBe(true)
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
pnpm test:unit tests/unit/stats/VisitorList.spec.js
```

预期：FAIL，找不到模块

- [ ] **步骤 3：编写 VisitorList 组件**

```vue
<template>
  <div class="visitor-list">
    <el-table v-if="users && users.length" :data="users" size="small">
      <el-table-column align="center" label="头像" width="80">
        <template slot-scope="scope">
          <el-avatar class="visitor-list__avatar" :size="30" :src="scope.row.avatar" />
        </template>
      </el-table-column>
      <el-table-column prop="username" align="center" label="用户" />
    </el-table>
    <el-empty v-else description="暂无访问用户" />
  </div>
</template>

<script>
export default {
  name: 'VisitorList',
  props: {
    users: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.visitor-list__avatar {
  vertical-align: middle;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

```bash
pnpm test:unit tests/unit/stats/VisitorList.spec.js
```

预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/stats/VisitorList.vue liuliupi-ui/tests/unit/stats/VisitorList.spec.js
git commit -m "feat(ui): 添加 VisitorList 组件"
```

---

## 任务 5：创建 ProvinceMap 组件

**文件：**
- 创建：`liuliupi-ui/src/components/admin/stats/ProvinceMap.vue`
- 创建：`liuliupi-ui/tests/unit/stats/ProvinceMap.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/stats/ProvinceMap.spec.js
import { shallowMount } from '@vue/test-utils'
import ProvinceMap from '@/components/admin/stats/ProvinceMap.vue'

describe('ProvinceMap.vue', () => {
  it('renders map container', () => {
    const wrapper = shallowMount(ProvinceMap, {
      propsData: {
        provinceData: [
          { province: '山西省', num: 294 },
          { province: '浙江省', num: 157 }
        ]
      }
    })
    expect(wrapper.find('.province-map').exists()).toBe(true)
    expect(wrapper.find('.province-map__fallback').exists()).toBe(false)
  })

  it('shows fallback table when load fails', async () => {
    const wrapper = shallowMount(ProvinceMap, {
      propsData: {
        provinceData: [{ province: '山西省', num: 294 }]
      }
    })
    wrapper.setData({ loadFailed: true })
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.province-map__fallback').exists()).toBe(true)
    expect(wrapper.text()).toContain('山西省')
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
pnpm test:unit tests/unit/stats/ProvinceMap.spec.js
```

预期：FAIL，找不到模块

- [ ] **步骤 3：编写 ProvinceMap 组件**

```vue
<template>
  <div class="province-map">
    <div v-if="!loadFailed" ref="chart" class="province-map__chart" />
    <div v-else class="province-map__fallback">
      <p class="province-map__fallback-tip">地图加载失败，已按访问量排序展示省份</p>
      <el-table :data="sortedData" size="small">
        <el-table-column type="index" align="center" width="50" />
        <el-table-column prop="province" align="center" label="省份" />
        <el-table-column prop="num" align="center" label="数量" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'ProvinceMap',
  props: {
    provinceData: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      chart: null,
      loadFailed: false
    }
  },
  computed: {
    sortedData() {
      return [...this.provinceData].sort((a, b) => b.num - a.num)
    }
  },
  watch: {
    provinceData: {
      handler() {
        this.renderChart()
      },
      deep: true
    }
  },
  mounted() {
    this.renderChart()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
    }
  },
  methods: {
    async renderChart() {
      if (!this.provinceData || !this.provinceData.length) return
      try {
        const res = await fetch('/geo/china.json')
        if (!res.ok) throw new Error('geojson fetch failed')
        const chinaGeoJson = await res.json()
        if (!echarts.getMap('china')) {
          echarts.registerMap('china', chinaGeoJson)
        }
        if (!this.chart) {
          this.chart = echarts.init(this.$refs.chart)
        }
        this.chart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{b}<br/>访问量：{c}'
          },
          visualMap: {
            min: 0,
            max: Math.max(...this.provinceData.map(i => i.num)),
            left: 'left',
            bottom: '0',
            text: ['高', '低'],
            calculable: true,
            inRange: {
              color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695']
            }
          },
          series: [{
            name: '省份访问量',
            type: 'map',
            map: 'china',
            roam: false,
            emphasis: {
              label: { show: true },
              itemStyle: { areaColor: '#ffd700' }
            },
            data: this.provinceData.map(item => ({
              name: this.normalizeProvinceName(item.province),
              value: item.num
            }))
          }]
        })
      } catch (e) {
        this.loadFailed = true
        this.$emit('error', e)
      }
    },
    normalizeProvinceName(name) {
      if (!name) return ''
      // 数据可能是「山西省」，GeoJSON 需要「山西」
      return name.replace(/省|市|自治区|壮族|回族|维吾尔|特别行政区/g, '')
    }
  }
}
</script>

<style scoped>
.province-map,
.province-map__chart {
  width: 100%;
  height: 220px;
}
.province-map__fallback-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-bottom: 8px;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

```bash
pnpm test:unit tests/unit/stats/ProvinceMap.spec.js
```

预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/stats/ProvinceMap.vue liuliupi-ui/tests/unit/stats/ProvinceMap.spec.js
git commit -m "feat(ui): 添加 ProvinceMap 中国地图组件"
```

---

## 任务 6：创建 OverviewCard 组件

**文件：**
- 创建：`liuliupi-ui/src/components/admin/stats/OverviewCard.vue`
- 创建：`liuliupi-ui/tests/unit/stats/OverviewCard.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/stats/OverviewCard.spec.js
import { shallowMount } from '@vue/test-utils'
import OverviewCard from '@/components/admin/stats/OverviewCard.vue'

describe('OverviewCard.vue', () => {
  it('renders total count, map and ip table', () => {
    const wrapper = shallowMount(OverviewCard, {
      propsData: {
        totalCount: 18883,
        provinceData: [{ province: '山西省', num: 294 }],
        ipData: [{ ip: '116.179.37.13', num: 77 }]
      },
      stubs: ['el-card', 'el-table', 'el-table-column', 'el-row', 'el-col', 'StatKpi', 'ProvinceMap']
    })
    expect(wrapper.find('statkpi-stub').exists()).toBe(true)
    expect(wrapper.find('provincemap-stub').exists()).toBe(true)
    expect(wrapper.findAll('el-table-column-stub')).toHaveLength(3)
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
pnpm test:unit tests/unit/stats/OverviewCard.spec.js
```

预期：FAIL，找不到模块

- [ ] **步骤 3：编写 OverviewCard 组件**

```vue
<template>
  <el-card class="overview-card">
    <div slot="header" class="overview-card__header">总览</div>
    <StatKpi label="总访问量（每个 IP 每天记一次）" :value="totalCount" />
    <el-row :gutter="16" class="overview-card__body">
      <el-col :xs="24" :sm="24" :md="14">
        <ProvinceMap :province-data="provinceData" />
      </el-col>
      <el-col :xs="24" :sm="24" :md="10">
        <el-table :data="ipData" size="small" max-height="260">
          <el-table-column type="index" align="center" width="50" />
          <el-table-column prop="ip" align="center" label="IP" min-width="140" />
          <el-table-column prop="num" align="center" label="数量" width="80" />
        </el-table>
      </el-col>
    </el-row>
  </el-card>
</template>

<script>
import StatKpi from './StatKpi.vue'
import ProvinceMap from './ProvinceMap.vue'

export default {
  name: 'OverviewCard',
  components: { StatKpi, ProvinceMap },
  props: {
    totalCount: {
      type: Number,
      default: 0
    },
    provinceData: {
      type: Array,
      default: () => []
    },
    ipData: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.overview-card {
  height: 100%;
}
.overview-card__header {
  font-weight: bold;
}
.overview-card__body {
  margin-top: 16px;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

```bash
pnpm test:unit tests/unit/stats/OverviewCard.spec.js
```

预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add liuliupi-ui/src/components/admin/stats/OverviewCard.vue liuliupi-ui/tests/unit/stats/OverviewCard.spec.js
git commit -m "feat(ui): 添加 OverviewCard 总览卡片"
```

---

## 任务 7：创建 TodayCard 和 YesterdayCard 组件

**文件：**
- 创建：`liuliupi-ui/src/components/admin/stats/TodayCard.vue`
- 创建：`liuliupi-ui/src/components/admin/stats/YesterdayCard.vue`
- 创建：`liuliupi-ui/tests/unit/stats/DailyCard.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/stats/DailyCard.spec.js
import { shallowMount } from '@vue/test-utils'
import TodayCard from '@/components/admin/stats/TodayCard.vue'
import YesterdayCard from '@/components/admin/stats/YesterdayCard.vue'

describe('DailyCard.vue', () => {
  it('TodayCard renders count and visitor list', () => {
    const wrapper = shallowMount(TodayCard, {
      propsData: {
        count: 6,
        users: [{ username: 'alice', avatar: 'a.jpg' }]
      },
      stubs: ['el-card', 'StatKpi', 'VisitorList']
    })
    expect(wrapper.find('statkpi-stub').props('value')).toBe(6)
    expect(wrapper.find('visitorlist-stub').exists()).toBe(true)
  })

  it('YesterdayCard renders count and visitor list', () => {
    const wrapper = shallowMount(YesterdayCard, {
      propsData: {
        count: 4,
        users: [{ username: 'bob', avatar: 'b.jpg' }]
      },
      stubs: ['el-card', 'StatKpi', 'VisitorList']
    })
    expect(wrapper.find('statkpi-stub').props('value')).toBe(4)
    expect(wrapper.text()).toContain('昨日访问')
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
pnpm test:unit tests/unit/stats/DailyCard.spec.js
```

预期：FAIL，找不到模块

- [ ] **步骤 3：编写 TodayCard 组件**

```vue
<template>
  <el-card class="daily-card">
    <div slot="header" class="daily-card__header">今日访问</div>
    <StatKpi label="今日访问量" :value="count" />
    <VisitorList class="daily-card__list" :users="users" />
  </el-card>
</template>

<script>
import StatKpi from './StatKpi.vue'
import VisitorList from './VisitorList.vue'

export default {
  name: 'TodayCard',
  components: { StatKpi, VisitorList },
  props: {
    count: {
      type: Number,
      default: 0
    },
    users: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.daily-card {
  margin-bottom: 16px;
}
.daily-card__header {
  font-weight: bold;
}
.daily-card__list {
  margin-top: 12px;
}
</style>
```

- [ ] **步骤 4：编写 YesterdayCard 组件**

```vue
<template>
  <el-card class="daily-card">
    <div slot="header" class="daily-card__header">昨日访问</div>
    <StatKpi label="昨日访问量" :value="count" />
    <VisitorList class="daily-card__list" :users="users" />
  </el-card>
</template>

<script>
import StatKpi from './StatKpi.vue'
import VisitorList from './VisitorList.vue'

export default {
  name: 'YesterdayCard',
  components: { StatKpi, VisitorList },
  props: {
    count: {
      type: Number,
      default: 0
    },
    users: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.daily-card {
  margin-bottom: 16px;
}
.daily-card__header {
  font-weight: bold;
}
.daily-card__list {
  margin-top: 12px;
}
</style>
```

- [ ] **步骤 5：运行测试验证通过**

```bash
pnpm test:unit tests/unit/stats/DailyCard.spec.js
```

预期：PASS

- [ ] **步骤 6：Commit**

```bash
git add liuliupi-ui/src/components/admin/stats/TodayCard.vue liuliupi-ui/src/components/admin/stats/YesterdayCard.vue liuliupi-ui/tests/unit/stats/DailyCard.spec.js
git commit -m "feat(ui): 添加 TodayCard 和 YesterdayCard 组件"
```

---

## 任务 8：重构 main.vue

**文件：**
- 修改：`liuliupi-ui/src/components/admin/main.vue`
- 创建：`liuliupi-ui/tests/unit/main.spec.js`

- [ ] **步骤 1：编写失败的测试**

```javascript
// liuliupi-ui/tests/unit/main.spec.js
import { shallowMount } from '@vue/test-utils'
import AdminMain from '@/components/admin/main.vue'

describe('admin/main.vue', () => {
  it('renders overview, today and yesterday cards', () => {
    const wrapper = shallowMount(AdminMain, {
      data() {
        return {
          historyInfo: {
            ip_history_count: 18883,
            ip_history_province: [{ province: '山西省', num: 294 }],
            ip_history_ip: [{ ip: '116.179.37.13', num: 77 }],
            ip_count_today: 6,
            username_today: [{ username: 'alice', avatar: 'a.jpg' }],
            ip_count_yest: 4,
            username_yest: [{ username: 'bob', avatar: 'b.jpg' }]
          }
        }
      },
      stubs: [
        'el-tag',
        'el-row',
        'el-col',
        'OverviewCard',
        'TodayCard',
        'YesterdayCard'
      ]
    })
    expect(wrapper.find('overviewcard-stub').exists()).toBe(true)
    expect(wrapper.find('todaycard-stub').exists()).toBe(true)
    expect(wrapper.find('yesterdaycard-stub').exists()).toBe(true)
  })

  it('fetches history info on created', () => {
    const getHistoryInfoSpy = jest.spyOn(AdminMain.methods, 'getHistoryInfo').mockImplementation(() => {})
    shallowMount(AdminMain, {
      stubs: ['el-tag', 'el-row', 'el-col', 'OverviewCard', 'TodayCard', 'YesterdayCard']
    })
    expect(getHistoryInfoSpy).toHaveBeenCalled()
    getHistoryInfoSpy.mockRestore()
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

```bash
pnpm test:unit tests/unit/main.spec.js
```

预期：FAIL，因为 main.vue 还未重构为使用新组件

- [ ] **步骤 3：重构 main.vue**

```vue
<template>
  <div class="admin-main">
    <el-tag effect="dark" class="admin-main__tag">
      <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
        <path
          d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
          fill="#8C7BFD"></path>
        <path
          d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
          fill="#FFE37B"></path>
      </svg>
      统计信息
    </el-tag>

    <el-row :gutter="16" class="admin-main__content" v-loading="loading">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <OverviewCard
          :total-count="historyInfo.ip_history_count"
          :province-data="historyInfo.ip_history_province"
          :ip-data="historyInfo.ip_history_ip"
        />
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <TodayCard
          :count="historyInfo.ip_count_today"
          :users="historyInfo.username_today"
        />
        <YesterdayCard
          :count="historyInfo.ip_count_yest"
          :users="historyInfo.username_yest"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import OverviewCard from './stats/OverviewCard.vue'
import TodayCard from './stats/TodayCard.vue'
import YesterdayCard from './stats/YesterdayCard.vue'

export default {
  name: 'AdminMain',
  components: { OverviewCard, TodayCard, YesterdayCard },
  data() {
    return {
      loading: false,
      historyInfo: {}
    }
  },
  created() {
    this.getHistoryInfo()
  },
  methods: {
    getHistoryInfo() {
      this.loading = true
      this.$http.get(this.$constant.baseURL + '/webInfo/getHistoryInfo', {}, true)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.historyInfo = res.data
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error'
          })
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.admin-main__tag {
  width: 100%;
  text-align: left;
  background: var(--lightYellow);
  border: none;
  height: 40px;
  line-height: 40px;
  font-size: 16px;
  color: var(--black);
  margin-bottom: 16px;
}
.admin-main__content {
  align-items: stretch;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

```bash
pnpm test:unit tests/unit/main.spec.js
```

预期：PASS

- [ ] **步骤 5：运行完整测试套件**

```bash
pnpm test:unit
```

预期：所有测试通过

- [ ] **步骤 6：Commit**

```bash
git add liuliupi-ui/src/components/admin/main.vue liuliupi-ui/tests/unit/main.spec.js
git commit -m "feat(ui): 重构 admin 统计信息页为卡片式响应式布局"
```

---

## 任务 9：移动端验证与收尾

**文件：**
- 修改：`liuliupi-ui/src/components/admin/stats/ProvinceMap.vue`（如需移动端高度调整）
- 修改：`liuliupi-ui/src/components/admin/main.vue`（如需间距微调）

- [ ] **步骤 1：启动开发服务器并验证 PC 布局**

```bash
cd liuliupi-ui
pnpm serve
```

在浏览器打开 `http://localhost:8080`，进入 admin 统计信息页，确认：
- 左侧「总览」大卡片占 2/3
- 右侧「今日 / 昨日」两张卡片上下排列
- 中国地图正常渲染
- IP TOP10 表格正常显示
- 数字颜色为 `--maxLightRed`

- [ ] **步骤 2：使用浏览器 DevTools 验证移动端布局**

切换至 iPhone SE / iPhone 12 Pro 等移动设备尺寸，确认：
- 三个卡片垂直堆叠
- 地图高度不低于 200px
- 表格可横向滚动
- 文字不溢出

- [ ] **步骤 3：验证地图降级**

临时重命名 `public/geo/china.json` 为 `china.json.bak`，刷新页面，确认：
- 地图区域显示 fallback 表格
- 省份数据按访问量排序

验证完成后恢复文件名。

- [ ] **步骤 4：运行 lint**

```bash
pnpm lint
```

预期：无 error（允许 warning）

- [ ] **步骤 5：运行完整测试套件**

```bash
pnpm test:unit
```

预期：全部通过

- [ ] **步骤 6：最终 Commit**

```bash
git add liuliupi-ui/
git commit -m "style(ui): 微调 admin 统计页移动端样式与验证"
```

---

## 自检

### 规格覆盖度

对照设计规格 `docs/superpowers/specs/2026-06-14-admin-stats-layout-design.md`：

| 规格章节 | 对应任务 |
|---|---|
| 卡片式布局 | 任务 6、7、8 |
| PC 横向 / 手机竖排 | 任务 8（el-row/el-col）、任务 9（验证） |
| 左侧总览 = KPI + 地图 + IP 表格 | 任务 5、6 |
| 右侧今日 / 昨日 = KPI + 用户列表 | 任务 4、7 |
| echarts 中国地图 | 任务 1、2、5 |
| 地图加载失败 fallback | 任务 5 |
| 空状态处理 | 任务 4 |
| 测试策略 | 各任务的测试步骤 |

### 占位符扫描

- 无「待定」、「TODO」、「后续实现」等占位符。
- 每个任务包含实际代码、命令和预期结果。

### 类型一致性

- `provinceData` 统一为 `{province: string, num: number}[]`
- `ipData` 统一为 `{ip: string, num: number}[]`
- `users` 统一为 `{username: string, avatar: string}[]`
- 组件名在测试 stubs 与实际文件名中一致

---

## 执行选项

计划已完成并保存到 `docs/superpowers/plans/2026-06-14-admin-stats-layout.md`。

**两种执行方式：**

1. **子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代。需要调用 `superpowers:subagent-driven-development`。
2. **内联执行** - 在当前会话中使用 `superpowers:executing-plans` 执行任务，批量执行并设有检查点。

**选哪种方式？**
