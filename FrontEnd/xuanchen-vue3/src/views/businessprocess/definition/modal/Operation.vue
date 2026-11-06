<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="640" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="code" label="流程编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.code" placeholder="请输入流程编码" allowClear :maxlength="64"
              :disabled="isEdit" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="name" label="流程名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.name" placeholder="请输入流程名称" allowClear :maxlength="100" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="businessType" label="业务类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.businessType" placeholder="请选择业务类型" allowClear
              :options="businessTypeOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="remark" label="备注" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.remark" placeholder="请输入备注" allowClear :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="code-tip">
      提示：流程编码创建后不可修改。保存后点击列表「设计」进入 BPMN 流程图设计器，
      启用前会校验流程图（节点可达性、审批人规则、网关条件等）。
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';

import XCModal from '@/components/xuanchen/XCModal.vue';
import { saveDefinitionApi } from '../definition.api';
import { businessTypeOptions } from '../../businessprocess.data';
import type { BpmDefinitionRecord } from '../definition.types';

const labelCol = { span: 7 };
const wrapperCol = { span: 17 };

defineProps({
  operationTitle: {
    type: String,
    default: '编辑',
  },
});
const emit = defineEmits(['childOK']);

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  code: [
    { required: true, message: '请输入流程编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码以字母开头，仅含字母/数字/下划线', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
};

const visible = ref(false);
const model = reactive({
  id: '' as string,
  code: '',
  name: '',
  businessType: undefined as string | undefined,
  remark: '',
});

const isEdit = computed(() => !!model.id);

const add = () => {
  visible.value = true;
  model.id = '';
  model.code = '';
  model.name = '';
  model.businessType = 'common';
  model.remark = '';
  rulesRef.value?.clearValidate();
};

const edit = (record: BpmDefinitionRecord) => {
  visible.value = true;
  model.id = record?.id ? String(record.id) : '';
  model.code = record?.code ? String(record.code) : '';
  model.name = record?.name ? String(record.name) : '';
  model.businessType = record?.businessType ? String(record.businessType) : undefined;
  model.remark = record?.remark ? String(record.remark) : '';
  rulesRef.value?.clearValidate();
};

const handleOk = async () => {
  await rulesRef.value?.validate();
  const res = await saveDefinitionApi({
    id: model.id || undefined,
    code: model.code.trim(),
    name: model.name.trim(),
    businessType: model.businessType,
    remark: model.remark,
  });
  if (res.code !== 200) {
    message.error(res.msg || '保存失败');
    return Promise.reject();
  }
  message.success('保存成功！');
  visible.value = false;
  emit('childOK');
};

defineExpose({
  add,
  edit,
});
</script>

<style scoped>
.code-tip {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.65;
  line-height: 1.7;
}
</style>
