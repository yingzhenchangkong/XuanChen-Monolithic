<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="520" @ok="handleOk" :confirm-loading="loading">
    <a-form ref="formRef" :model="formState" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-item label="知识库名称" name="kbName" :rules="[{ required: true, message: '请输入知识库名称' }]">
        <a-input v-model:value="formState.kbName" placeholder="请输入知识库名称" :maxlength="100" />
      </a-form-item>
      <a-form-item label="描述" name="kbDesc">
        <a-textarea v-model:value="formState.kbDesc" placeholder="知识库用途说明（选填）" :rows="3" :maxlength="500" />
      </a-form-item>
      <a-form-item label="分块大小" name="chunkSize" extra="每块字符数，留空使用全局默认（500）">
        <a-input-number v-model:value="formState.chunkSize" :min="100" :max="2000" :step="50" style="width: 100%" />
      </a-form-item>
      <a-form-item label="分块重叠" name="chunkOverlap" extra="相邻块重叠字符数，留空使用全局默认（50）">
        <a-input-number v-model:value="formState.chunkOverlap" :min="0" :max="500" :step="10" style="width: 100%" />
      </a-form-item>
      <a-alert v-if="isEdit" type="warning" show-icon
        message="修改分块参数后，已有文档需重新解析才会按新参数分块" />
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';

import { saveKbApi } from '../knowledge.api';

const visible = ref(false);
const loading = ref(false);
const operationTitle = ref('新增知识库');
const formRef = ref<FormInstance>();

const formState = reactive({
  id: '',
  kbName: '',
  kbDesc: '',
  chunkSize: undefined as number | undefined,
  chunkOverlap: undefined as number | undefined,
});

const isEdit = ref(false);

const add = () => {
  isEdit.value = false;
  operationTitle.value = '新增知识库';
  Object.assign(formState, { id: '', kbName: '', kbDesc: '', chunkSize: undefined, chunkOverlap: undefined });
  visible.value = true;
};

const edit = (record: Record<string, any>) => {
  isEdit.value = true;
  operationTitle.value = '编辑知识库';
  Object.assign(formState, {
    id: record.id,
    kbName: record.kbName,
    kbDesc: record.kbDesc ?? '',
    chunkSize: record.chunkSize ?? undefined,
    chunkOverlap: record.chunkOverlap ?? undefined,
  });
  visible.value = true;
};

const handleOk = async () => {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const res = await saveKbApi({
      id: formState.id || undefined,
      kbName: formState.kbName.trim(),
      kbDesc: formState.kbDesc || undefined,
      chunkSize: formState.chunkSize,
      chunkOverlap: formState.chunkOverlap,
    });
    if (res.code === 200) {
      message.success(res.msg || '保存成功');
      visible.value = false;
      emit('childOK');
    } else {
      message.error(res.msg || '保存失败');
    }
  } finally {
    loading.value = false;
  }
};

const emit = defineEmits<{ (e: 'childOK'): void }>();

defineExpose({ add, edit });
</script>
