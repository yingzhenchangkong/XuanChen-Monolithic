<template>
  <div class="loginContainer">
    <div class="registerBox">
      <a-typography-title :level="3" class="registerTitle">账号注册</a-typography-title>
      <a-form :model="registerForm" :rules="registerRules" ref="registerRef" autocomplete="off" @keyup.enter="showVerify">
        <a-form-item name="userName">
          <a-input placeholder="用户名（3-10 个字符）" v-model:value="registerForm.userName" :maxlength="10">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="nickName">
          <a-input placeholder="昵称（选填，不超过 20 个字符）" v-model:value="registerForm.nickName" :maxlength="20">
            <template #prefix>
              <SmileOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="email">
          <a-input placeholder="邮箱（用于找回密码）" v-model:value="registerForm.email" :maxlength="50">
            <template #prefix>
              <MailOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="password">
          <a-input-password placeholder="密码（6-20 个字符）" v-model:value="registerForm.password"
            autocomplete="new-password" :maxlength="20">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="confirmPassword">
          <a-input-password placeholder="请再次输入密码" v-model:value="registerForm.confirmPassword"
            autocomplete="new-password" :maxlength="20">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" style="width: 100%;" :loading="loading" @click="showVerify">注册</a-button>
        </a-form-item>
        <div class="loginEntry">
          已有账号？<a @click="goLogin">返回登录</a>
        </div>
      </a-form>
    </div>
  </div>
  <SlideVerify ref="refOperation" @childOK="handleCaptchaVerify" />
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { reactive, ref } from 'vue';
import { UserOutlined, LockOutlined, SmileOutlined, MailOutlined } from '@ant-design/icons-vue';
import router from '@/router';
import SlideVerify from './modal/SlideVerify.vue';
import { register } from './auth.api';
import { getConfigKeyValueApi } from '../system/config/config.api';

const registerRef = ref();
const refOperation = ref();
const loading = ref(false);

const defaultForm = () => ({
  userName: '',
  nickName: '',
  email: '',
  password: '',
  confirmPassword: '',
  captchaId: '',
  captchaToken: '',
});
const registerForm = reactive(defaultForm());

const registerRules = reactive({
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 50, message: '邮箱长度不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) =>
        value === registerForm.password ? Promise.resolve() : Promise.reject('两次输入的密码不一致'),
      trigger: 'blur',
    },
  ],
});

const showVerify = async () => {
  await registerRef.value.validate();
  const res = await getConfigKeyValueApi('captchaEnabled');
  // 与登录一致：仅当配置显式为 "false" 时关闭验证码，缺省/异常均按开启处理
  if (res?.data?.configValue !== 'false') {
    refOperation.value.show();
  } else {
    submitRegister();
  }
}

const handleCaptchaVerify = (captchaId: string, captchaToken: string) => {
  registerForm.captchaId = captchaId;
  registerForm.captchaToken = captchaToken;
  submitRegister();
}

const submitRegister = async () => {
  loading.value = true;
  try {
    const res = await register({
      userName: registerForm.userName.trim(),
      nickName: registerForm.nickName.trim(),
      email: registerForm.email.trim().toLowerCase(),
      password: registerForm.password,
      captchaId: registerForm.captchaId,
      captchaToken: registerForm.captchaToken,
    });
    if (res?.code === 200) {
      message.success(res.msg || '注册成功，请使用新账号登录！');
      router.push({ name: 'login' });
    } else {
      message.error(res.msg);
    }
  } catch (error) {
    message.error('注册失败，请重试');
  } finally {
    loading.value = false;
    refOperation.value?.reset();
  }
}

const goLogin = () => {
  router.push({ name: 'login' });
}
</script>

<style scoped>
.loginContainer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-image: url("@/assets/images/bg.png");
  background-size: 100% 100%;
  background-repeat: no-repeat;
  background-position: center;
}

.registerBox {
  width: 400px;
  padding: 20px;
  border: 1px solid #ccc;
  background: rgb(255, 255, 255, 0.6);
}

.registerTitle {
  text-align: center;
}

.loginEntry {
  text-align: center;
  font-size: 14px;
  color: #666;
}
</style>
