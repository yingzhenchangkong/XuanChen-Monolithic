<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-form-item name="dictCode" label="字典编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.dictCode" placeholder="请输入字典编码" allowClear :disabled="dictCodeDisabled" />
      </a-form-item>
      <a-form-item name="dictName" label="字典名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.dictName" placeholder="请输入字典名称" allowClear />
      </a-form-item>
      <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-switch v-model:checked="model.status" checked-children="启用" un-checked-children="停用" />
      </a-form-item>
      <a-form-item label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { validateDictCodeApi,validateDictNameApi, saveOrUpdateDict } from '../dict.api';
import type { DictModel } from '../dict.types';

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})
const emit = defineEmits(['childOK']);

const validateDictCode = async (_rule: Rule, value: string) => {
  await validateDictCodeApi(model.id, value);
}

const validateDictName = async (_rule: Rule, value: string) => {
  await validateDictNameApi(model.id, value);
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  dictCode: [
    { required: true, message: '请输入字典编码', trigger: 'blur' },
    { required: true, validator: validateDictCode, trigger: 'blur' }
  ],
  dictName: [
    { required: true, message: '请输入字典名称', trigger: 'blur' },
    { required: true, validator: validateDictName, trigger: 'blur' }
  ]
}

const visible = ref(false);
const dictCodeDisabled = ref(false);

const model = reactive<DictModel>({
  id: '',
  dictCode: '',
  dictName: '',
  status: true,
  orderNo: undefined,
})

//打开弹窗
const add = () => {
  visible.value = true;
  dictCodeDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.dictCode = '';
  model.dictName = '';
  model.status = true;
  model.orderNo = undefined;
}
const edit = (records: any) => {
  visible.value = true;
  dictCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.dictCode = records.dictCode;
  model.dictName = records.dictName;
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdateDict(model);
  message.success(res.msg);
  emit('childOK');
  visible.value = false;
};

//子组件方法默认为私有
defineExpose({
  add,
  edit
})

</script>