<template>
  <a-modal v-model:open="visible" title="详情" :width="1000">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
    <a-typography>
      <a-typography-title :level="4" style="text-align: center;">{{ model.title }}</a-typography-title>
      <a-typography-paragraph type="success">发布人：{{ model.createName }} <span style="float: right;">发布时间：{{
        model.createTime
      }}</span></a-typography-paragraph>
      <a-typography-paragraph v-html="model.content"></a-typography-paragraph>
    </a-typography>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { NoticeModel } from '../notice.types';

const visible = ref(false);
const model = reactive<NoticeModel>({
  id: '',
  title: '',
  content: '',
  createName: '',
  createTime: undefined,
  noticeStatusId: '',
})
const detail = (records: NoticeModel) => {
  visible.value = true;
  model.title = records.title;
  model.content = records.content;
  model.createName = records.createName;
  model.createTime = records.createTime;
}

const handleCancel = () => {
  visible.value = false;
};

defineExpose({
  detail
})
</script>