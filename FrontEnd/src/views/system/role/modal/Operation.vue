<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="roleCode" label="角色编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.roleCode" placeholder="请输入角色编码" allowClear :disabled="roleCodeDisabled" />
      </a-form-item>
      <a-form-item name="roleName" label="角色名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.roleName" placeholder="请输入角色名称" allowClear />
      </a-form-item>
      <a-form-item label="角色描述" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.roleDescription" placeholder="请输入角色描述" allowClear />
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
import { getAction, httpAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const emit = defineEmits(['childOK']);
const url = {
  add: '/system/role/add',
  edit: '/system/role/edit',
  validateRoleCode: '/system/role/validateRoleCode',
  validateRoleName: '/system/role/validateRoleName',
}
const visible = ref(false);
const roleCodeDisabled = ref(false);

const model = reactive({
  id: '',
  roleCode: '',
  roleName: '',
  roleDescription: '',
  status: true,
  orderNo: '',
})

const validateRoleCode = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateRoleCode, { id: model.id, roleCode: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

const validateRoleName = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateRoleName, { id: model.id, roleName: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { required: true, validator: validateRoleCode, trigger: 'blur' }
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { required: true, validator: validateRoleName, trigger: 'blur' }
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
  model.roleCode = '';
  model.roleName = '';
  model.roleDescription = '';
  model.status = true;
  model.orderNo = '';
}
const edit = (records: any) => {
  visible.value = true;
  roleCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.roleCode = records.roleCode;
  model.roleName = records.roleName;
  model.roleDescription = records.roleDescription;
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = () => {
  rulesRef.value.validate().then(() => {
    let httpurl = '';
    let method = '';
    if (model.id === '') {
      httpurl += url.add;
      method = 'post';
    } else {
      httpurl += url.edit;
      method = 'put';
    }
    httpAction(httpurl, model, method).then((res: any) => {
      message.success(res.msg);
      emit('childOK');
    })
    visible.value = false;
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
};

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>