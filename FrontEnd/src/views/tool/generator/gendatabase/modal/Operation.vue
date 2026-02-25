<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="connType" label="连接类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.connType" :options="optionsConnType"
          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择连接类型" allowClear></a-select>
      </a-form-item>
      <a-form-item name="connName" label="连接名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.connName" placeholder="请输入连接名称" allowClear />
      </a-form-item>
      <a-form-item name="host" label="主机" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.host" placeholder="请输入主机" allowClear />
      </a-form-item>
      <a-form-item name="port" label="端口" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.port" placeholder="请输入端口" allowClear />
      </a-form-item>
      <a-form-item name="dbName" label="数据库名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.dbName" placeholder="请输入数据库名称" allowClear />
      </a-form-item>
      <a-form-item name="userName" label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.userName" placeholder="请输入用户名" allowClear />
      </a-form-item>
      <a-form-item name="password" label="密码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.password" placeholder="请输入密码" allowClear />
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
import type { GenDatabase } from '../gendatabase.types';
import { saveOrUpdate } from '../gendatabase.api';
import { getDictSelect } from '@/views/system/dict/dict.api';

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
const roleCodeDisabled = ref(false);

const model = reactive<GenDatabase>({
  id: '',
  connType: undefined,
  connName: '',
  host: '',
  port: '',
  dbName: '',
  userName: '',
  password: '',
  status: true,
  orderNo: undefined,
})

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
  ],
}

//打开弹窗
const add = () => {
  visible.value = true;
  roleCodeDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.connType = undefined;
  model.connName = '';
  model.host = '';
  model.port = '';
  model.dbName = '';
  model.userName = '';
  model.password = '';
  model.status = true;
  model.orderNo = undefined;
}
const edit = (records: any) => {
  visible.value = true;
  roleCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.connType = records.connType;
  model.connName = records.connName;
  model.host = records.host;
  model.port = records.port;
  model.dbName = records.dbName;
  model.userName = records.userName;
  model.password = records.password;
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdate(model);
  message.success(res.msg);
  emit('childOK');
  visible.value = false;
};

const optionsConnType = ref([]);
const getConnType = async () => {
  const res: any = await getDictSelect('db_conn_type');
  optionsConnType.value = res;
}
getConnType();

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>