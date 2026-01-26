<template>
  <a-card title="修改资料" style="height: 400px;">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" class="modal-form-style">
      <a-form-item label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.userName" disabled />
      </a-form-item>
      <a-form-item label="昵称" :labelCol="labelCol" :wrapperCol="wrapperCol" name="nickName">
        <a-input v-model:value="model.nickName" placeholder="请输入昵称" allowClear />
      </a-form-item>
      <a-form-item label="手机号" :labelCol="labelCol" :wrapperCol="wrapperCol" name="mobile">
        <a-input v-model:value="model.mobile" placeholder="请输入手机号" allowClear />
      </a-form-item>
      <a-form-item label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol" name="email">
        <a-input v-model:value="model.email" placeholder="请输入邮箱" allowClear />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 20, span: 2 }">
        <a-button @click="save" style="margin-left: 10px;" type="primary">保存</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { useAuthStore } from '@/stores';
import { validateMobileApi, validateEmailApi, userCenterEdit } from '../../user/user.api';

const userStore = useAuthStore();
const userInfo = userStore.getUserInfo();

const labelCol = { span: 4 };
const wrapperCol = { span: 20 };

const model = reactive({
  id: userInfo.id,
  userName: userInfo.userName,
  nickName: userInfo.nickName,
  mobile: userInfo.mobile,
  email: userInfo.email,
})

const validateMobile = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateMobileApi(model.id, value);
}
const validateEmail = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateEmailApi(model.id, value);
}

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { required: true, validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
}
const save = async () => {
  await rulesRef.value.validate();
  const res: any = await userCenterEdit(model);
  if (res.code === 200) {
    message.success(res.msg);
    userInfo.nickName = model.nickName;
    userInfo.mobile = model.mobile;
    userInfo.email = model.email;
    userStore.setUserInfo(userInfo);
  }
}
</script>