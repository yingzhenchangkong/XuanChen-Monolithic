<template>
  <a-modal v-model:open="visible" :title="props.operationTitle" :width="500" @ok="handleOk" ok-text="确认"
    cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-form-item name="dictItemValue" label="数据值" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.dictItemValue" placeholder="请输入数据值" allowClear
          :disabled="dictItemValueDisabled" />
      </a-form-item>
      <a-form-item name="dictItemText" label="名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.dictItemText" placeholder="请输入名称" allowClear />
      </a-form-item>
      <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-switch v-model:checked="model.status" checked-children="启用" un-checked-children="停用" />
      </a-form-item>
      <a-form-item name="orderNo" label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import type { DictItemModel } from '../dict.types';
import { validateDictItemTextApi, validateDictItemValueApi, saveOrUpdateDictItem } from '../dict.api';

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const props = defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  },
  dictCode: {
    type: String,
    default: ''
  }
})
const emit = defineEmits(['childOK']);

const validateDictItemText = async (_rule: Rule, value: string) => {
  await validateDictItemTextApi(model.id, model.dictCode, value);
}

const validateDictItemValue = async (_rule: Rule, value: string) => {
  await validateDictItemValueApi(model.id, model.dictCode, value);
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  dictItemValue: [
    { required: true, message: '请输入数据值', trigger: 'blur' },
    { required: true, validator: validateDictItemValue, trigger: 'blur' }
  ],
  dictItemText: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { required: true, validator: validateDictItemText, trigger: 'blur' }
  ]
}

const visible = ref(false);
const dictItemValueDisabled = ref(false);

const model = reactive<DictItemModel>({
  id: '',
  dictCode: '',
  dictItemValue: '',
  dictItemText: '',
  status: true,
  orderNo: undefined,
})

//打开弹窗
const add = () => {
  visible.value = true;
  dictItemValueDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.dictCode = props.dictCode;
  model.dictItemValue = '';
  model.dictItemText = '';
  model.status = true;
  model.orderNo = undefined;
}
const edit = (records: any) => {
  visible.value = true;
  dictItemValueDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.dictCode = records.dictCode;
  model.dictItemValue = records.dictItemValue;
  model.dictItemText = records.dictItemText;
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdateDictItem(model);
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