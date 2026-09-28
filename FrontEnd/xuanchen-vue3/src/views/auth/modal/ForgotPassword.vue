<template>
  <a-modal v-model:open="open" title="找回密码" :confirm-loading="loading" :width="420" @ok="submitReset"
    okText="重置密码" cancelText="取消" @cancel="handleCancel">
    <a-alert type="info" show-icon style="margin-bottom: 16px;"
      message="通过向账号绑定邮箱发送验证码完成身份认证；验证码 5 分钟内有效，重置成功后该账号原有登录状态将全部失效。" />
    <a-form :model="resetForm" :rules="resetRules" ref="resetRef" autocomplete="off">
      <a-form-item name="userName" label="用户名">
        <a-input placeholder="请输入要找回的用户名" v-model:value="resetForm.userName" :maxlength="10"
          @blur="resetForm.userName = resetForm.userName.trim()" />
      </a-form-item>
      <a-form-item name="emailCode" label="邮箱验证码">
        <a-input-group compact style="display: flex;">
          <a-input v-model:value="resetForm.emailCode" placeholder="6 位验证码" :maxlength="6" allow-clear />
          <a-button type="primary" style="min-width: 120px; border-radius: 0 6px 6px 0;" :loading="sending"
            :disabled="countdown > 0" @click="showSendVerify">
            {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
          </a-button>
        </a-input-group>
      </a-form-item>
      <a-form-item name="password" label="新密码">
        <a-input-password placeholder="6-20 个字符" v-model:value="resetForm.password" autocomplete="new-password"
          :maxlength="20" />
      </a-form-item>
      <a-form-item name="confirmPassword" label="确认密码">
        <a-input-password placeholder="请再次输入新密码" v-model:value="resetForm.confirmPassword"
          autocomplete="new-password" :maxlength="20" />
      </a-form-item>
    </a-form>
  </a-modal>
  <SlideVerify ref="refOperation" @childOK="handleCaptchaVerify" />
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { reactive, ref, onBeforeUnmount } from 'vue';
import { sendPasswordEmailCode, verifyPasswordEmailCode, resetPasswordByEmail } from '../auth.api';
import SlideVerify from './SlideVerify.vue';

const open = ref(false);
const loading = ref(false);
const sending = ref(false);
// 发送按钮倒计时（秒）
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const resetRef = ref();
const refOperation = ref();
// 滑块验证通过后要执行的动作（本弹窗仅"发送验证码"一种）
const pendingCaptcha = ref<(captchaId: string, captchaToken: string) => void>(() => {});
// 验证码校验通过后换取的一次性重置令牌
const resetToken = ref('');

const defaultForm = () => ({
  userName: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
});
const resetForm = reactive(defaultForm());

const resetRules = reactive({
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) =>
        value === resetForm.password ? Promise.resolve() : Promise.reject('两次输入的密码不一致'),
      trigger: 'blur',
    },
  ],
});

// 供父组件打开弹窗；每次打开重置为干净状态
const show = () => {
  Object.assign(resetForm, defaultForm());
  resetToken.value = '';
  countdown.value = 0;
  resetRef.value?.clearValidate?.();
  open.value = true;
}

const handleCancel = () => {
  open.value = false;
}

const clearTimer = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}
onBeforeUnmount(clearTimer);

const startCountdown = () => {
  countdown.value = 60;
  clearTimer();
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearTimer();
    }
  }, 1000);
}

// 点击"发送验证码"：先校验用户名，然后强制滑块验证（与后端一致，发信不受验证码开关影响）
const showSendVerify = async () => {
  await resetRef.value.validateFields(['userName']);
  pendingCaptcha.value = async (captchaId: string, captchaToken: string) => {
    sending.value = true;
    try {
      const res = await sendPasswordEmailCode({
        userName: resetForm.userName,
        captchaId,
        captchaToken,
      });
      if (res?.code === 200) {
        message.success(res.msg || '验证码已发送，请注意查收！');
        startCountdown();
      } else {
        message.error(res.msg);
      }
    } catch (error) {
      message.error('验证码发送失败，请重试');
    } finally {
      sending.value = false;
      refOperation.value?.reset();
    }
  };
  refOperation.value.show();
}

const handleCaptchaVerify = (captchaId: string, captchaToken: string) => {
  pendingCaptcha.value(captchaId, captchaToken);
}

// 点击"重置密码"：校验表单 → 验证码换一次性令牌 → 凭令牌设新密码
const submitReset = async () => {
  await resetRef.value.validate();
  loading.value = true;
  try {
    //令牌只在本次重置流程内使用；之前没换过或上次失败则先换取
    if (!resetToken.value) {
      const verifyRes = await verifyPasswordEmailCode({
        userName: resetForm.userName,
        emailCode: resetForm.emailCode,
      });
      if (verifyRes?.code !== 200) {
        message.error(verifyRes?.msg || '验证码校验失败');
        return;
      }
      resetToken.value = verifyRes.data.resetToken;
    }
    const res = await resetPasswordByEmail({
      resetToken: resetToken.value,
      password: resetForm.password,
    });
    if (res?.code === 200) {
      message.success(res.msg || '密码已重置，请使用新密码登录！');
      open.value = false;
    } else {
      //令牌一次性/过期等失败：清掉令牌，要求重新走验证码
      resetToken.value = '';
      message.error(res.msg);
    }
  } catch (error) {
    resetToken.value = '';
    message.error('重置失败，请重试');
  } finally {
    loading.value = false;
  }
}

defineExpose({ show });
</script>
