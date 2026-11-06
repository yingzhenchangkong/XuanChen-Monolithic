<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="500" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="title" label="标题" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.title" placeholder="请输入待办标题" :maxlength="200" allowClear />
      </a-form-item>
      <a-form-item name="content" label="内容" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-textarea v-model:value="model.content" placeholder="请输入待办内容（选填）" :maxlength="1000" :rows="3" />
      </a-form-item>
      <a-form-item name="priority" label="优先级" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-radio-group v-model:value="model.priority">
          <a-radio v-for="opt in TODO_PRIORITY_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item name="dueDate" label="截止时间" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-date-picker
          v-model:value="model.dueDate"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择截止时间（选填）"
          style="width: 100%;"
        />
      </a-form-item>
    </a-form>
  </XCModal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import { saveOrUpdateTodoApi } from '../todo.api';
import { TODO_PRIORITY_OPTIONS } from '../todo.data';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const emit = defineEmits(['childOK']);

const visible = ref(false);

const model = reactive({
  id: '',
  title: '',
  content: '',
  priority: 2,
  dueDate: '' as string | undefined,
})

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  title: [
    { required: true, whitespace: true, message: '请输入待办标题', trigger: 'blur' },
  ],
}

//打开弹窗（新增）
const add = () => {
  visible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.title = '';
  model.content = '';
  model.priority = 2;
  model.dueDate = '';
}

//打开弹窗（编辑，仅未完成的待办可编辑）
const edit = (records: Record<string, any>) => {
  visible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.title = records.title;
  model.content = records.content ?? '';
  model.priority = records.priority ?? 2;
  model.dueDate = records.dueDate ?? '';
}

const handleOk = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdateTodoApi({
      id: model.id || undefined,
      title: model.title.trim(),
      content: model.content?.trim() || null,
      priority: model.priority,
      dueDate: model.dueDate || null,
    });
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    emit('childOK');
    visible.value = false;
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，此处仅保持弹窗打开
  }
};

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>
