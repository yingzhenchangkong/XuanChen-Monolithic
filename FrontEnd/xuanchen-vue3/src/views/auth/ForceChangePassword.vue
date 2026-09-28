<template>
  <div class="forceChangeContainer">
    <div class="forceChangeBox">
      <a-typography-title :level="3" class="changeTitle">萱晨管理系统</a-typography-title>
      <a-alert
        type="warning"
        show-icon
        message="首次登录或密码已被管理员重置"
        description="为保障账号安全，请先修改初始密码，修改成功后将自动进入系统，无需重新登录。"
        style="margin-bottom: 18px;"
      />
      <a-form :model="model" :rules="rules" ref="rulesRef" @keyup.enter="handleOk" autocomplete="off">
        <a-form-item name="oldPassword">
          <a-input-password v-model:value="model.oldPassword" placeholder="请输入当前（初始）密码" allowClear autocomplete="off">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="password">
          <a-input-password v-model:value="model.password" placeholder="请输入新密码" allowClear autocomplete="off">
            <template #prefix>
              <KeyOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="confirmPassword">
          <a-input-password v-model:value="model.confirmPassword" placeholder="请再次输入新密码" allowClear autocomplete="off">
            <template #prefix>
              <SafetyOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 2, span: 22 }">
          <a-button type="primary" :loading="submitting" @click="handleOk" style="width: 100%;">确认修改并进入系统</a-button>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 2, span: 22 }">
          <a-button type="link" style="width: 100%; padding: 0;" @click="handleLogout">退出登录，换个账号</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { changePassword } from '@/views/system/user/user.api';
import { logout } from '@/views/auth/auth.api';
import { useAuthStore, useMenuStore } from '@/stores';

const router = useRouter();
const authStore = useAuthStore();
const menuStore = useMenuStore();

const submitting = ref(false);
const rulesRef = ref();

const model = reactive({
  oldPassword: '',
  password: '',
  confirmPassword: '',
})

// 与 Layout 头部改密弹窗保持同一套密码规则
const validateConfirmPassword = (_rule: Rule, value: string) => {
  if (value !== model.password) {
    return Promise.reject('两次输入的密码不一致！');
  } else {
    return Promise.resolve();
  }
}

const rules = {
  oldPassword: [
    { required: true, message: '请输入当前（初始）密码！', trigger: 'blur' }
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

onMounted(() => {
  // 无会话（如直接刷新到本页但会话已失效）则回到登录页，避免死页
  if (!authStore.getToken()) {
    router.replace({ name: 'login' });
  }
})

const handleOk = async () => {
  await rulesRef.value.validate();
  submitting.value = true;
  try {
    const res = await changePassword(model.oldPassword, model.password);
    if (res.code === 200) {
      // 后端改密成功后已踢掉全部旧会话，并随响应返回一套全新会话（与 /login 同构）：
      // 直接落地新 token/用户信息并进入首页，无需再手动登录
      if (res.data?.token) {
        const { token, ...userInfo } = res.data;
        authStore.setToken(token);
        authStore.setUserInfo(userInfo);
        // 菜单可能还是上个会话的残留，强制守卫重新拉取
        menuStore.setMenuList(undefined);
        message.success('密码修改成功，正在进入系统…');
        router.replace({ path: '/' });
      } else {
        // 兼容旧后端：未返回新会话时回退为手动登录
        message.success(res.msg || '密码修改成功，请使用新密码登录！');
        authStore.clearAuth();
        window.sessionStorage.clear();
        router.replace({ name: 'login' });
      }
    } else {
      message.error(res.msg);
    }
  } catch {
    // 401/403 已由全局响应拦截器统一提示或跳转，这里不重复弹错
  } finally {
    submitting.value = false;
  }
}

const handleLogout = async () => {
  try {
    await logout();
  } catch (error) {
    // 忽略登出接口异常，保证本地会话一定被清理
  }
  authStore.clearAuth();
  window.sessionStorage.clear();
  router.replace({ name: 'login' });
}
</script>

<style scoped>
.forceChangeContainer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-image: url("@/assets/images/bg.png");
  background-size: 100% 100%;
  background-repeat: no-repeat;
  background-position: center;
}

.forceChangeBox {
  width: 440px;
  padding: 24px;
  border: 1px solid #ccc;
  background: rgb(255, 255, 255, 0.85);
  border-radius: 8px;
}

.changeTitle {
  text-align: center;
  margin-bottom: 18px !important;
}
</style>
