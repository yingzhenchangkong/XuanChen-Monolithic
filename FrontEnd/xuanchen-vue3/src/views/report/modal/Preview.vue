<template>
  <XCModal v-model:open="visible" :title="title" :width="1200" hide-footer default-fullscreen
    wrap-class-name="report-preview-modal">
    <!-- 弹窗打开才挂载渲染组件：保证 Univer 容器就绪，关闭时自动 dispose -->
    <ReportRender v-if="open && reportCode" :report-code="reportCode" class="preview-render" />
  </XCModal>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import XCModal from '@/components/xuanchen/XCModal.vue';
import ReportRender from '../viewer/ReportRender.vue';

const props = defineProps<{
  open: boolean;
  /** 弹窗标题（默认用报表名） */
  title?: string;
  /** 报表编码 */
  reportCode?: string;
}>();

const emit = defineEmits(['update:open']);

const visible = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val),
});
</script>

<style lang="scss">
/* 非全屏时的弹窗体也要撑满可用高度，画布才有布局空间 */
.report-preview-modal {
  .ant-modal-body {
    height: calc(85vh - 55px);
    padding: 0;
    display: flex;
  }

  .preview-render {
    flex: 1;
    min-width: 0;
  }
}
</style>
