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
