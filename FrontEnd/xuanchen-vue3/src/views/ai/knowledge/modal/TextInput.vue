<template>
  <a-modal v-model:open="visible" title="手动录入知识" :width="640" @ok="handleOk" :confirm-loading="saving"
    ok-text="保存并解析" destroy-on-close>
    <a-form layout="vertical" style="margin-top: 12px">
      <a-form-item label="知识标题" required>
        <a-input v-model:value="form.title" placeholder="如：产品退货流程说明（留空则按时间自动命名）" :maxlength="100" show-count
          allow-clear />
      </a-form-item>
      <a-form-item label="知识正文" required>
        <a-textarea v-model:value="form.content" placeholder="粘贴纯文本或 Markdown 内容，保存后自动按知识库分块参数向量化入库"
          :rows="14" :maxlength="200000" show-count />
      </a-form-item>
      <a-alert type="info" show-icon message="正文将以 txt 形式存入文件中心（可在「我的文件」查看），并与知识库文档关联。" />
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';

import { addDocTextApi } from '../knowledge.api';
import type { AiKbRecord } from '../knowledge.types';

const props = defineProps<{ kb: AiKbRecord | null }>();
const open = defineModel<boolean>('open', { default: false });
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = open;
const saving = ref(false);
const form = reactive({ title: '', content: '' });

watch(visible, (val) => {
  if (val) {
    form.title = '';
    form.content = '';
  }
});

const handleOk = async () => {
  if (!props.kb?.id) return;
  if (!form.content.trim()) {
    message.warning('请填写正文内容');
    return;
  }
  saving.value = true;
  try {
    const res = await addDocTextApi(props.kb.id, form.title.trim(), form.content);
    if (res.code === 200) {
      message.success('录入成功，解析中');
      visible.value = false;
      emit('saved');
    } else {
      message.error(res.msg || '录入失败');
    }
  } finally {
    saving.value = false;
  }
};
</script>
