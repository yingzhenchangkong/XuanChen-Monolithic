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
        <a-input v-model:value="model.password" allowClear
          :placeholder="model.id ? '密码已设置，留空表示不修改' : '请输入密码'" autocomplete="new-password" />
      </a-form-item>
      <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-switch v-model:checked="model.status" :checked-value="1" :un-checked-value="0" checked-children="启用" un-checked-children="停用" />
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
import type { SelectOption } from '@/types/api';
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
  status: 1,
  orderNo: undefined,
})

// 与后端 DBUtil 白名单保持一致（最终以后端校验为准）
const HOST_PATTERN = /^[A-Za-z0-9_.:[\]-]{1,255}$/;
const DB_NAME_PATTERN = /^[A-Za-z0-9_$-]{1,64}$/;
const USERNAME_PATTERN = /^[A-Za-z0-9_.@$-]{1,64}$/;

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  connType: [
    { required: true, message: '请选择连接类型', trigger: 'change' },
  ],
  connName: [
    { required: true, message: '请输入连接名称', trigger: 'blur' },
  ],
  host: [
    { required: true, message: '请输入主机', trigger: 'blur' },
    { pattern: HOST_PATTERN, message: '主机仅允许域名、IPv4 或 [IPv6]，禁止空格及 / ? & # @ 等字符', trigger: 'blur' },
  ],
  port: [
    { required: true, message: '请输入端口', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string) => {
        const p = Number(value);
        if (!/^\d{1,5}$/.test(value ?? '') || p < 1 || p > 65535) {
          return Promise.reject(new Error('端口必须为 1-65535 的数字'));
        }
        return Promise.resolve();
      },
      trigger: 'blur',
    },
  ],
  dbName: [
    { required: true, message: '请输入数据库名称', trigger: 'blur' },
    { pattern: DB_NAME_PATTERN, message: '库名仅允许字母、数字、下划线、连字符与 $，禁止 ?、&、反引号等字符', trigger: 'blur' },
  ],
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: USERNAME_PATTERN, message: '用户名仅允许字母、数字及 . _ - @ $ 字符', trigger: 'blur' },
  ],
  password: [
    {
      // 编辑态留空表示不修改原密码；新增时必填
      validator: (_rule: unknown, value: string) => {
        if (!model.id && !value) {
          return Promise.reject(new Error('请输入密码'));
        }
        return Promise.resolve();
      },
      trigger: 'blur',
    },
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
  model.status = 1;
  model.orderNo = undefined;
}
const edit = (records: GenDatabase) => {
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
  //接口仅回传掩码 ******，不回填真实密码；留空保存表示不修改
  model.password = '';
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdate(model);
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

const optionsConnType = ref<SelectOption[]>([]);
const getConnType = async () => {
  const res = await getDictSelect('db_conn_type');
  optionsConnType.value = res ?? [];
}
getConnType();

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>