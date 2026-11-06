<template>
  <div class="viewer-page">
    <ReportRender :report-code="reportCode" />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import ReportRender from './ReportRender.vue';

const route = useRoute();
const reportCode = String(route.params.code ?? '');
</script>

<style scoped>
.viewer-page {
  height: calc(100vh - 110px);
  min-height: 480px;
}
</style>
