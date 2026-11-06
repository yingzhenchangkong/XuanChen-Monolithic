<template>
  <a-drawer v-model:open="open" :title="operationTitle" placement="right" :width="360">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-form-item name="userName" label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.userName" placeholder="请输入用户名" allowClear :disabled="userNameDisabled" />
      </a-form-item>
      <a-form-item name="password" label="密码" :labelCol="labelCol" :wrapperCol="wrapperCol" v-if="passwordVisible">
        <a-input-password v-model:value="model.password" placeholder="无输入时自动生成临时密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item name="nickName" label="昵称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.nickName" placeholder="系统内展示名，留空取用户名" allowClear />
      </a-form-item>
      <a-form-item name="mobile" label="手机号" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.mobile" placeholder="登录验证码/通知使用" allowClear />
      </a-form-item>
      <a-form-item name="email" label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.email" placeholder="找回密码使用，全系统唯一" allowClear />
      </a-form-item>
      <a-form-item name="personId" label="绑定人员" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.personId" style="width: 100%" :options="optionsPerson"
          :fieldNames="{ label: 'realName', value: 'id' }" placeholder="请选择绑定人员（可空）" allowClear show-search
          option-filter-prop="realName">
        </a-select>
      </a-form-item>
      <a-form-item name="roleIds" label="角色" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.roleIds" mode="multiple" style="width: 100%" :options="optionsRoleIds"
          :fieldNames="{ label: 'roleName', value: 'id' }" placeholder="请选择角色" :max-tag-count="maxTagCount" allowClear>
          <template #maxTagPlaceholder="omittedValues">
            <span style="color: red">+ {{ omittedValues.length }} ...</span>
          </template>
        </a-select>
      </a-form-item>
      <a-form-item name="status" label="账号状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <XCSelect v-model="model.status" apiUrl="/system/dict/select" :params="{ dictCode: 'account_status' }"
          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择账号状态" />
      </a-form-item>
      <a-form-item name="avatar" label="头像" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <XCUploadImage v-model:file-list="avatarFiles" image-path="avatar" :max-count="1" :max-size="2" />
      </a-form-item>
    </a-form>
    <template #footer>
      <a-button type="primary" @click="handleOk" style="float: right;">确定</a-button>
      <a-button @click="handleCancel" style="float: right;margin-right: 10px;">取消</a-button>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import XCSelect from '@/components/xuanchen/XCSelect.vue';
import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';
import { useAuthStore } from '@/stores';

import { validateUserNameApi, validateMobileApi, validateEmailApi, saveOrUpdate } from '../account.api';
import { getPersonSelect } from '@/views/masterdata/person/person.api';
import type { AccountRecord } from '../account.types';
import { getRoleSelect } from '../../role/role.api';
import type { SelectOption } from '@/types/api';

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
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' }
  ],
  nickName: [
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
}

const open = ref(false);
const userNameDisabled = ref(false);
const passwordVisible = ref(true);

type IdList = Array<string | number> | undefined;
const model = reactive({
  id: '' as string | number,
  userName: '',
  /** 头像相对路径（sys_account.avatar） */
  avatar: '',
  nickName: '',
  mobile: '',
  email: '',
  personId: undefined as string | undefined,
  password: undefined as string | undefined,
  roleIds: undefined as IdList,
  status: undefined as string | undefined,
})

const maxTagCount = ref(3);

/** 头像上传契约：XCUploadImage 的 v-model 是「相对路径数组」，表单只存单个路径字符串 */
const avatarFiles = computed<string[]>({
  get: () => (model.avatar ? [model.avatar] : []),
  set: (list) => {
    model.avatar = list[0] ?? '';
  }
});

const optionsRoleIds = ref<SelectOption[]>([])
const getSelectRole = async () => {
  optionsRoleIds.value = await getRoleSelect();
}
getSelectRole();

/** 可绑定的人员下拉（人员管理维护） */
const optionsPerson = ref<SelectOption[]>([]);
const getSelectPerson = async () => {
  optionsPerson.value = await getPersonSelect();
}
getSelectPerson();

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
  model.avatar = '';
  model.nickName = '';
  model.mobile = '';
  model.email = '';
  model.personId = undefined;
  model.password = undefined;
  model.roleIds = undefined;
  model.status = '1';
}
const edit = (records: AccountRecord) => {
  open.value = true;
  userNameDisabled.value = true;
  passwordVisible.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id ?? '';
  model.userName = records.userName ?? '';
  model.avatar = records.avatar ?? '';
  model.nickName = records.nickName ?? '';
  model.mobile = records.mobile ?? '';
  model.email = records.email ?? '';
  model.personId = records.personId ?? undefined;
  model.roleIds = records.roleIds;
  if (model.roleIds == null) {
    model.roleIds = undefined;
  }
  model.status = records.status != null ? String(records.status) : undefined;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdate(model);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    syncSessionUserInfo();
    message.success(res.msg);
    emit('childOK');
    open.value = false;
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，此处仅阻止“成功提示+关窗”
  }
};

/**
 * 会话同步：右上角头像/用户中心读的是登录时写入的 authStore.userInfo（缓存于 sessionStorage），
 * 账号管理改头像/昵称只更新数据库，当前会话不会自动感知。
 * 若编辑的正是当前登录账号（userName 唯一且编辑时不可改），把新值整体替换回 store 即可立即生效；
 * 编辑他人账号时对方需重新登录获取新值（管理员无法触达他人会话，属预期语义）。
 */
const authStore = useAuthStore();
const syncSessionUserInfo = () => {
  const current = authStore.getUserInfo();
  if (!current || !model.userName || current.userName !== model.userName) {
    return;
  }
  //与登录接口 COALESCE(NULLIF(nick_name,''), user_name) 的展示名规则保持一致；
  //mobile/email 直接取表单值（清空也应生效），不回退旧值
  authStore.setUserInfo({
    ...current,
    nickName: model.nickName || current.userName,
    avatar: model.avatar || '',
    mobile: model.mobile ?? current.mobile,
    email: model.email ?? current.email,
  });
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
