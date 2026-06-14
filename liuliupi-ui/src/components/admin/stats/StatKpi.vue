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
