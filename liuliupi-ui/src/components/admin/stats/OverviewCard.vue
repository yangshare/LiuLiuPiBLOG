<template>
  <el-card class="overview-card">
    <div slot="header" class="overview-card__header">总览</div>
    <div class="overview-card__content">
      <StatKpi label="总访问量（每个 IP 每天记一次）" :value="totalCount" />
      <el-row :gutter="16" class="overview-card__body">
        <el-col :xs="24" :sm="24" :md="14" class="overview-card__col">
          <ProvinceMap :province-data="provinceData" />
        </el-col>
        <el-col :xs="24" :sm="24" :md="10" class="overview-card__col">
          <el-table :data="ipData" size="small" height="100%">
            <el-table-column type="index" align="center" width="50" />
            <el-table-column prop="ip" align="center" label="IP" min-width="140" />
            <el-table-column prop="num" align="center" label="数量" width="80" />
          </el-table>
        </el-col>
      </el-row>
    </div>
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
  display: flex;
  flex-direction: column;
}
.overview-card ::v-deep .el-card__body {
  flex: 1;
  overflow: hidden;
}
.overview-card__content {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.overview-card__header {
  font-weight: bold;
}
.overview-card__body {
  flex: 1;
  margin-top: 16px;
  min-height: 0;
}
.overview-card__col {
  height: 100%;
}
</style>
