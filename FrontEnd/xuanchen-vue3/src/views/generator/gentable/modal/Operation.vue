<template>
  <XCModal v-model:open="visible" :title="operationTitle" :defaultFullscreen="true" width="100%" @ok="handleOk">
    <!-- 配置主体抽到 GenTableEditor：弹窗（编辑）与全屏页（新增）共用一份逻辑 -->
    <GenTableEditor ref="editorRef" />
  </XCModal>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';

import XCModal from '@/components/xuanchen/XCModal.vue';
import GenTableEditor from '../components/GenTableEditor.vue';
import type { GenTable } from '../gentable.types';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
});

const emit = defineEmits(['childOK']);
const visible = ref(false);
const editorRef = ref<InstanceType<typeof GenTableEditor> | null>(null);

/** 弹窗确认：保存成功后关闭并通知列表刷新 */
const handleOk = async () => {
  const result = await editorRef.value?.save();
  if (result?.ok) {
    emit('childOK');
    visible.value = false;
  }
};

defineExpose({
  /** 打开弹窗（新增） */
  add: () => {
    visible.value = true;
    nextTick(() => editorRef.value?.add());
  },
  /** 打开弹窗（编辑）：主表字段 + 各子表字段回显 */
  edit: async (record: GenTable) => {
    visible.value = true;
    await nextTick();
    await editorRef.value?.edit(record);
  },
});
</script>
