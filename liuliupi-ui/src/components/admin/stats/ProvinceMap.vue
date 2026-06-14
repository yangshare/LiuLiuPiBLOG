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
  height: 380px;
}
.province-map__fallback-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-bottom: 8px;
}
</style>
