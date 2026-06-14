<template>
  <el-card class="overview-card">
    <div slot="header" class="overview-card__header">总览</div>
    <StatKpi label="总访问量（每个 IP 每天记一次）" :value="totalCount" />
    <el-row :gutter="16" class="overview-card__body">
      <el-col :xs="24" :sm="24" :md="14">
        <ProvinceMap :province-data="provinceData" />
      </el-col>
      <el-col :xs="24" :sm="24" :md="10">
        <el-table :data="ipData" size="small">
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
