<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-form-item name="userName" label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.userName" placeholder="请输入用户名" allowClear :disabled="userNameDisabled" />
      </a-form-item>
      <a-form-item name="nickName" label="昵称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.nickName" placeholder="请输入昵称" allowClear />
      </a-form-item>
      <a-form-item name="mobile" label="手机号" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.mobile" placeholder="请输入手机号" allowClear />
      </a-form-item>
      <a-form-item name="email" label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.email" placeholder="请输入邮箱" allowClear />
      </a-form-item>
      <a-form-item name="password" label="密码" :labelCol="labelCol" :wrapperCol="wrapperCol" v-if="passwordVisible">
        <a-input-password v-model:value="model.password" placeholder="无输入时自动设置:XuanChen@888888" allowClear
          autocomplete="off" />
      </a-form-item>
      <a-form-item name="deptIds" label="部门" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-tree-select v-model:value="model.deptIds" show-search style="width: 100%"
          :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" placeholder="请选择部门" allow-clear
          tree-default-expand-all :tree-data="treeData" tree-node-filter-prop="label" multiple
          :field-names="{ children: 'children', label: 'title', value: 'key', }">
        </a-tree-select>
      </a-form-item>
      <a-form-item name="roleIds" label="角色" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.roleIds" mode="multiple" style="width: 100%" :options="optionsRoleIds"
          :fieldNames="{ label: 'roleName', value: 'id' }" placeholder="请选择角色" :max-tag-count="maxTagCount" allowClear>
          <template #maxTagPlaceholder="omittedValues">
            <span style="color: red">+ {{ omittedValues.length }} ...</span>
          </template>
        </a-select>
      </a-form-item>
      <a-form-item name="postIds" label="岗位" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.postIds" mode="multiple" style="width: 100%" :options="optionsPostIds"
          :fieldNames="{ label: 'postName', value: 'id' }" placeholder="请选择岗位" :max-tag-count="maxTagCount" allowClear>
          <template #maxTagPlaceholder="omittedValues">
            <span style="color: red">+ {{ omittedValues.length }} ...</span>
          </template>
        </a-select>
      </a-form-item>
      <a-form-item name="status" label="账号状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-radio-group v-model:value="model.status">
          <a-radio value="1">正常</a-radio>
          <a-radio value="0">禁用</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item name="avatar" label="头像" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <UploadImageXC v-model:file-list="model.fileList" image-path="avatar" :max-count="maxCount" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import UploadImageXC from '@/components/xuanchen/UploadImageXC.vue';

import { validateUserNameApi, validateMobileApi, validateEmailApi, saveOrUpdate } from '../user.api';
import { getRoleSelect } from '../../role/role.api';
import { getDeptTreeApi } from '../../dept/dept.api';
import { getPostSelect } from '../../post/post.api';

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

defineProps({
  modalTitle: {
    type: String,
    default: '编辑'
  }
})
const emit = defineEmits(['childOK']);

const validateUserName = async (_rule: Rule, value: string) => {
  await validateUserNameApi(model.id, value);
}
const validateMobile = async (_rule: Rule, value: string) => {
  await validateMobileApi(model.id, value);
}
const validateEmail = async (_rule: Rule, value: string) => {
  await validateEmailApi(model.id, value);
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' },
    { required: true, validator: validateUserName, trigger: 'blur' }
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { required: true, validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' }
  ],
}

const visible = ref(false);
const userNameDisabled = ref(false);
const passwordVisible = ref(true);

const model = reactive({
  id: '',
  userName: '',
  nickName: '',
  mobile: '',
  email: '',
  password: undefined,
  roleIds: undefined,
  deptIds: undefined,
  postIds: undefined,
  avatar: undefined,
  fileList: [] as string[],
  status: 1
})

const maxCount = ref(1);
const maxTagCount = ref(3);

const optionsRoleIds = ref([])
const getSelectRole = async () => {
  optionsRoleIds.value = await getRoleSelect();
}
getSelectRole();

const treeData = ref();
const getDeptTree = async () => {
  treeData.value = await getDeptTreeApi();
}
getDeptTree();

const optionsPostIds = ref([]);
const getSelectPost = async () => {
  optionsPostIds.value = await getPostSelect();
}
getSelectPost();

//打开弹窗
const add = () => {
  visible.value = true;
  userNameDisabled.value = false;
  passwordVisible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.userName = '';
  model.nickName = '';
  model.mobile = '';
  model.email = '';
  model.password = undefined;
  model.roleIds = undefined;
  model.avatar = undefined;
  model.fileList = [];
}
const edit = (records: any) => {
  visible.value = true;
  userNameDisabled.value = true;
  passwordVisible.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.userName = records.userName;
  model.nickName = records.nickName;
  model.mobile = records.mobile;
  model.email = records.email;
  model.roleIds = records.roleIds;
  if (model.roleIds == null) {
    model.roleIds = undefined;
  }
  model.deptIds = records.deptIds;
  if (model.deptIds == null) {
    model.deptIds = undefined;
  }
  model.avatar = records.avatar;
  model.fileList = [];
  model.fileList.push(records.avatar);
  model.status = records.status;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const fileList: any = model.fileList[0];
  model.avatar = fileList?.response?.msg ?? '';
  model.fileList = [];
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