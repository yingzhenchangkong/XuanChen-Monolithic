<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="configCode" label="参数编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.configCode" placeholder="请输入参数编码" allowClear :disabled="configCodeDisabled" />
      </a-form-item>
      <a-form-item name="configName" label="参数名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.configName" placeholder="请输入参数名称" allowClear />
      </a-form-item>
      <a-form-item label="参数描述" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.configDescription" placeholder="请输入参数描述" allowClear />
      </a-form-item>
      <a-form-item name="configType" label="参数类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.configType" :options="optionsConfigType"
          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择参数类型" allowClear></a-select>
      </a-form-item>
      <a-form-item name="configKey" label="参数键名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.configKey" placeholder="请输入参数键名" allowClear />
      </a-form-item>
      <a-form-item name="configValue" label="参数键值" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.configValue" placeholder="请输入参数键值" allowClear />
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
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import type { ConfigModel } from '../config.types';
import { validateConfigCodeApi, validateConfigNameApi, validateConfigKeyApi, validateConfigValueApi, saveOrUpdate } from '../config.api';
import { getDictSelect } from '../../dict/dict.api';

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
const configCodeDisabled = ref(false);
const optionsConfigType = ref([]);

const model = reactive<ConfigModel>({
  id: '',
  configCode: '',
  configName: '',
  configDescription: '',
  configKey: '',
  configValue: '',
  configType: undefined,
  orderNo: undefined,
  status: true,
})

const validateConfigCode = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateConfigCodeApi(model.id, value);
}

const validateConfigName = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateConfigNameApi(model.id, value);
}

const validateConfigKey = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateConfigKeyApi(model.id, value);
}

const validateConfigValue = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateConfigValueApi(model.id, value);
}

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  configCode: [
    { required: true, message: '请输入参数编码', trigger: 'blur' },
    { required: true, validator: validateConfigCode, trigger: 'blur' }
  ],
  configName: [
    { required: true, message: '请输入参数名称', trigger: 'blur' },
    { required: true, validator: validateConfigName, trigger: 'blur' }
  ],
  configType: [
    { required: true, message: '请选择参数类型', trigger: 'blur' }
  ],
  configKey: [
    { required: true, message: '请输入参数键名', trigger: 'blur' },
    { required: true, validator: validateConfigKey, trigger: 'blur' }
  ],
  configValue: [
    { required: true, message: '请输入参数键值', trigger: 'blur' },
    { required: true, validator: validateConfigValue, trigger: 'blur' }
  ]
}

//打开弹窗
const add = () => {
  visible.value = true;
  configCodeDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.configCode = '';
  model.configName = '';
  model.configDescription = '';
  model.configKey = '';
  model.configValue = '';
  model.configType = undefined;
  model.orderNo = undefined;
  model.status = true;
}
const edit = (records: any) => {
  visible.value = true;
  configCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.configCode = records.configCode;
  model.configName = records.configName;
  model.configDescription = records.configDescription;
  model.configKey = records.configKey;
  model.configValue = records.configValue;
  model.configType = records.configType;
  model.orderNo = records.orderNo;
  model.status = records.status;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdate(model);
  message.success(res.msg);
  emit('childOK');
  visible.value = false;
};

const getConfigType = async () => {
  const res: any = await getDictSelect('config_type');
  optionsConfigType.value = res;
}
getConfigType();

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>