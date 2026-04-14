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
import { message } from 'ant-design-vue';
import type { RoleModel } from '../role.types';
import { validateRoleCodeApi, validateRoleNameApi, saveOrUpdate } from '../role.api';

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

const model = reactive<RoleModel>({
  id: '',
  roleCode: '',
  roleName: '',
  roleDescription: '',
  status: true,
  orderNo: '',
})

const validateRoleCode = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateRoleCodeApi(model.id, value);
}

const validateRoleName = async (_rule: Rule, value: string) => {
  if(!value) return;
  await validateRoleNameApi(model.id, value);
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

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdate(model);
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