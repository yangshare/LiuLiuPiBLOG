<template>
  <div class="admin-main">
    <el-row :gutter="16" class="admin-main__content" v-loading="loading">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <OverviewCard
          :total-count="historyInfo.ip_history_count"
          :province-data="historyInfo.ip_history_province"
          :ip-data="historyInfo.ip_history_ip"
        />
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8" class="admin-main__side">
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
.admin-main__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.admin-main__content {
  align-items: stretch;
}
</style>
