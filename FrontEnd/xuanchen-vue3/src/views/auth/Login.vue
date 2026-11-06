<template>
  <div class="loginContainer">
    <div class="loginBox">
      <a-typography-title :level="3" class="loginTitle">萱晨管理系统</a-typography-title>
      <a-form :model="loginForm" :rules="loginRules" ref="loginRef" @keyup.enter="showVerify" autocomplete="off">
        <a-form-item name="userName">
          <a-input placeholder="用户名" v-model:value="loginForm.userName">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="password">
          <a-input-password placeholder="请输入密码" v-model:value="loginForm.password" autocomplete="off">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <div class="loginOptions">
          <a-checkbox v-model:checked="loginForm.rememberMe">记住我</a-checkbox>
          <a class="forgetPwd" @click="showForgot">忘记密码？</a>
        </div>
        <a-form-item>
          <a-button type="primary" @click="showVerify" style="width: 100%;">登录</a-button>
        </a-form-item>
        <div class="registerEntry">
          还没有账号？<a @click="goRegister">立即注册</a>
        </div>
      </a-form>
    </div>
  </div>
  <SlideVerify ref="refOperation" @childOK="handleCaptchaVerify" />
  <ForgotPassword ref="forgotRef" />
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { ref, reactive } from 'vue';
import { useAuthStore, useMenuStore, useTabsStore } from '@/stores';
import router from '@/router';
import SlideVerify from './modal/SlideVerify.vue';
import ForgotPassword from './modal/ForgotPassword.vue';
import { login } from './auth.api';
import { getConfigKeyValueApi } from '../system/config/config.api';
import { loadMenus } from '@/permission';

const authStore = useAuthStore();
const menuStore = useMenuStore();
const tabsStore = useTabsStore();

// 表单数据
const loginForm = reactive({
  userName: 'admin4',
  password: '888888',
  rememberMe: false,
  captchaId: '',
  captchaToken: '',
})

// 表单验证
const loginRef = ref();
const loginRules = reactive({
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
})
const refOperation = ref()
const forgotRef = ref()
const showVerify = async () => {
  await loginRef.value.validate();
  const res = await getConfigKeyValueApi("captchaEnabled");
  // 与后端一致：仅当配置显式为 "false" 时关闭验证码，缺省/异常均按开启处理
  if (res?.data?.configValue !== "false") {
    refOperation.value.show();
  } else {
    loginSubmit();
  }
}
const handleCaptchaVerify = async (captchaId: string, captchaToken: string) => {
  loginForm.captchaId = captchaId;
  loginForm.captchaToken = captchaToken;
  loginSubmit();
}
// 登录
const loginSubmit = async () => {
  try {
    const res = await login(
      loginForm.userName,
      loginForm.password,
      loginForm.captchaId,
      loginForm.captchaToken,
      loginForm.rememberMe,
    );
    if (res?.code === 200) {
      const { token, ...userInfo } = res.data;
      authStore.setToken(token);
      authStore.setUserInfo(userInfo);
      // 新会话开始：清掉上一账号残留的菜单与动态路由，强制路由守卫按新 token 重新拉取。
      // 否则"退出登录/会话过期"后再登录，守卫会把残留的 [] 或旧菜单误判为"已加载"，
      // 导致左侧只剩首页（或显示上个账号的菜单），必须刷新才恢复
      menuStore.setMenuList(undefined);
      if (router.hasRoute('parent')) {
        router.removeRoute('parent');
      }
      // 新会话只保留首页页签，确保不残留上一账号打开过的标签
      tabsStore.clearTabsList();
      tabsStore.setActiveKey('');
      // 初始/被重置密码：不进系统，直接引导到独立强制改密页（路由守卫也会兜底）
      if (userInfo.pwdResetRequired === 1) {
        router.push({ name: "forceChangePassword" });
        message.warning('当前为初始/重置密码，请先修改密码后再进行其他操作！');
      } else {
        // 登录成功后立即并行发起菜单加载——与 router.push 的守卫链路并行执行。
        // loadMenus 内部有 Promise 去重，守卫进来时若请求仍在飞，会直接 await 同一个 Promise，
        // 不会打第二次 /system/menu/authList；若已完成则菜单已在 store 里，守卫直接放行。
        void loadMenus().catch((err) => {
          console.warn('[Login] 预加载菜单失败:', err);
        });
        router.push({ path: "/" });
        message.success(res.msg);
      }
    } else {
      message.error(res.msg);
    }
  } catch (error) {
    message.error('登录失败,请重试');
  } finally {
    refOperation.value?.reset();
  }
}
// 忘记密码：打开重置弹窗
const showForgot = () => {
  forgotRef.value?.show();
}
// 跳转注册页
const goRegister = () => {
  router.push({ name: 'register' });
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

.loginBox {
  width: 400px;
  min-height: 240px;
  padding: 20px;
  border: 1px solid #ccc;
  background: rgb(255, 255, 255, 0.6);
}

.loginTitle {
  text-align: center;
}

.loginOptions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.forgetPwd {
  font-size: 14px;
}

.registerEntry {
  text-align: center;
  font-size: 14px;
  color: #666;
}
</style>
