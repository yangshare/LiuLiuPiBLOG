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
