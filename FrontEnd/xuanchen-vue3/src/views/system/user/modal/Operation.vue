<template>
  <a-drawer v-model:open="open" :title="operationTitle" placement="right" :width="360">
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
        <a-select v-model:value="model.status" :options="optionsStatus"
          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择账号状态" allowClear></a-select>
      </a-form-item>
      <a-form-item name="avatar" label="头像" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <XCUploadImage v-model:file-list="model.fileList" image-path="avatar" :max-count="maxCount" />
      </a-form-item>
    </a-form>
    <template #footer>
      <a-button type="primary" @click="handleOk" style="float: right;">确定</a-button>
      <a-button @click="handleCancel" style="float: right;margin-right: 10px;">取消</a-button>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';

import { validateUserNameApi, validateMobileApi, validateEmailApi, saveOrUpdate } from '../user.api';
import type { UserRecord } from '../user.types';
import { getRoleSelect } from '../../role/role.api';
import type { DeptRecord } from '../../dept/dept.types';
import { getDeptTreeApi } from '../../dept/dept.api';
import { getPostSelect } from '../../post/post.api';
import { getDictSelect } from '../../dict/dict.api';
import type { SelectOption } from '@/types/api';
import { useAuthStore } from '@/stores';

const labelCol = { span: 6 };
const wrapperCol = { span: 18 };

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})
const emit = defineEmits(['childOK']);

const validateUserName = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateUserNameApi(String(model.id ?? ''), value);
}
const validateMobile = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateMobileApi(String(model.id ?? ''), value);
}
const validateEmail = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateEmailApi(String(model.id ?? ''), value);
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

const open = ref(false);
const userNameDisabled = ref(false);
const passwordVisible = ref(true);

type IdList = Array<string | number> | undefined;
const model = reactive({
  id: '' as string | number,
  userName: '',
  nickName: '',
  mobile: '',
  email: '',
  password: undefined as string | undefined,
  roleIds: undefined as IdList,
  deptIds: undefined as IdList,
  postIds: undefined as IdList,
  avatar: undefined as string | undefined,
  fileList: [] as string[],
  status: undefined as string | undefined,
})

const maxCount = ref(1);
const maxTagCount = ref(3);

const optionsRoleIds = ref<SelectOption[]>([])
const getSelectRole = async () => {
  optionsRoleIds.value = await getRoleSelect();
}
getSelectRole();

const treeData = ref<DeptRecord[]>();
const getDeptTree = async () => {
  treeData.value = await getDeptTreeApi();
}
getDeptTree();

const optionsStatus = ref<SelectOption[]>([]);
const getUserStatus = async () => {
  optionsStatus.value = await getDictSelect('user_status');
}
getUserStatus();

const optionsPostIds = ref<SelectOption[]>([]);
const getSelectPost = async () => {
  optionsPostIds.value = await getPostSelect();
}
getSelectPost();

//打开弹窗
const add = () => {
  open.value = true;
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
  model.deptIds = undefined;
  model.postIds = undefined;
  model.avatar = undefined;
  model.fileList = [];
  model.status = '1';
}
const edit = (records: UserRecord) => {
  open.value = true;
  userNameDisabled.value = true;
  passwordVisible.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id ?? '';
  model.userName = records.userName ?? '';
  model.nickName = records.nickName ?? '';
  model.mobile = records.mobile ?? '';
  model.email = records.email ?? '';
  model.roleIds = records.roleIds;
  if (model.roleIds == null) {
    model.roleIds = undefined;
  }
  model.deptIds = records.deptIds;
  if (model.deptIds == null) {
    model.deptIds = undefined;
  }
  model.postIds = records.postIds;
  if (model.postIds == null) {
    model.postIds = undefined;
  }
  if (records.avatar) {
    model.avatar = records.avatar;
    model.fileList = [];
    model.fileList.push(records.avatar);
  }
  model.status = records.status != null ? String(records.status) : undefined;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  // XCUploadImage 的 v-model 直接回写服务端相对路径字符串数组，无需解析 UploadFile 结构
  model.avatar = model.fileList?.[0] ?? '';
  model.fileList = [];
  try {
    const res = await saveOrUpdate(model);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    // 编辑的是当前登录用户自己时，同步会话中的用户信息（头像/昵称/手机/邮箱），
    // 否则右上角头像、用户中心等读 authStore 的地方要等重新登录才刷新；
    // 编辑其他用户绝不能动当前会话
    const authStore = useAuthStore();
    const current = authStore.getUserInfo();
    if (current && String(current.id ?? '') === String(model.id ?? '')) {
      authStore.setUserInfo({
        ...current,
        avatar: model.avatar,
        nickName: model.nickName,
        mobile: model.mobile,
        email: model.email,
      });
    }
    emit('childOK');
    open.value = false;
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，此处仅阻止“成功提示+关窗”
  }
};

const handleCancel = () => {
  open.value = false;
}

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>