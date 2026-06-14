<template>
  <el-card class="daily-card">
    <div slot="header" class="daily-card__header">昨日访问</div>
    <div class="daily-card__body">
      <StatKpi label="昨日访问量" :value="count" />
      <VisitorList class="daily-card__list" :users="users" />
    </div>
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
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}
.daily-card ::v-deep .el-card__body {
  flex: 1;
  overflow: hidden;
}
.daily-card__body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.daily-card__header {
  font-weight: bold;
}
.daily-card__list {
  flex: 1;
  overflow: auto;
  margin-top: 12px;
  min-height: 0;
}
</style>
