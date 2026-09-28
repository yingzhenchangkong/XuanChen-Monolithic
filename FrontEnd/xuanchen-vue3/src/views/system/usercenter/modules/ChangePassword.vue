<template>
  <a-card title="修改密码" style="height: 400px;">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" class="modal-form-style">
      <a-form-item label="原密码" :labelCol="labelCol" :wrapperCol="wrapperCol" name="oldPassword">
        <a-input-password v-model:value="model.oldPassword" placeholder="请输入原密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item label="新密码" :labelCol="labelCol" :wrapperCol="wrapperCol" name="password">
        <a-input-password v-model:value="model.password" placeholder="请输入新密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item label="确认密码" :labelCol="labelCol" :wrapperCol="wrapperCol" name="confirmPassword">
        <a-input-password v-model:value="model.confirmPassword" placeholder="请再次输入新密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 18, span: 5 }">
        <a-button @click="save" style="margin-left: 10px;" type="primary">保存</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { useRouter } from 'vue-router';
import { changePassword } from '../../user/user.api';
import { logout } from '@/views/auth/auth.api';
import { useAuthStore } from '@/stores';

const router = useRouter();
const authStore = useAuthStore();

const labelCol = { span: 5 };
const wrapperCol = { span: 17 };

const model = reactive({
  oldPassword: '',
  password: '',
  confirmPassword: '',
})
const validateConfirmPassword = (_rule: Rule, value: string) => {
  if (value !== model.password) {
    return Promise.reject('两次输入的密码不一致！');
  } else {
    return Promise.resolve();
  }
}
const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  oldPassword: [
    { required: true, message: '请输入原密码！', trigger: 'blur' }
  ],
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, trigger: 'blur' }
  ],
  confirmPassword: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

const save = async () => {
  await rulesRef.value.validate();
  const res = await changePassword(model.oldPassword, model.password);
  if (res.code === 200) {
    message.success('修改成功');
    await logout();
    // 同步清空响应式会话状态（token/userInfo ref），否则守卫仍会读到内存中的旧凭据
    authStore.clearAuth();
    window.sessionStorage.clear();
    router.push("/login");
  } else {
    message.error(res.msg);
  }
}
</script>