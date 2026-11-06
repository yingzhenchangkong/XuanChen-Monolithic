<template>
  <!-- 默认只显示一个问号图标，鼠标悬停时通过 Tooltip 展开说明 -->
  <a-tooltip :title="title" placement="top">
    <QuestionCircleOutlined class="xc-tip-icon" />
  </a-tooltip>
</template>

<script setup lang="ts">
defineProps({
  /** 悬停提示内容 */
  title: {
    type: String,
    default: '',
  },
});
</script>

<style lang="scss" scoped>
.xc-tip-icon {
  flex-shrink: 0;
  cursor: help;
  margin-left: 6px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.35);

  &:hover {
    color: #1677ff;
  }
}
</style>
