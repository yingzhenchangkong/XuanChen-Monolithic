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
        <a-form-item>
          <a-checkbox v-model:checked="loginForm.rememberMe">记住我?</a-checkbox>
          <a @click="forgotPassword">
            <QuestionCircleOutlined style="margin-left: 195px;" />忘记密码?
          </a>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 2, span: 22 }">
          <a-button type="primary" @click="showVerify" style="width: 40%;">登录</a-button>
          <a-button @click="jumpRegister" style="width: 40%;margin-left: 10%;">注册</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
  <SlideVerify ref="refOperation" @childOK="loginSubmit"/>
</template>

<script setup lang="ts">
import { postAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores';
import router from '@/router';
import SlideVerify from './modal/SlideVerify.vue';

const authStore = useAuthStore();
// 表单数据
const loginForm = reactive({
  userName: 'admin',
  password: '888888',
  captcha: '',
  rememberMe: false,
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
const showVerify = () => {
  refOperation.value.show()
}
// 登录
const loginSubmit = () => {
  loginRef.value.validate().then(() => {
    postAction('/login', loginForm).then((res: any) => {
      if (res && res.code === 200) {
        authStore.setToken(res.data.token);
        delete res.data.token;
        authStore.setUserInfo(res.data);
        router.push({ path: "/" });
        message.success(res.msg);
      } else {
        message.error(res.msg);
      }
    })
  })
}
// 忘记密码
const forgotPassword = () => {

}
// 跳转注册
const jumpRegister = () => {

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
  height: 300px;
  padding: 20px;
  border: 1px solid #ccc;
  background: rgb(255, 255, 255, 0.6);
}

.loginTitle {
  text-align: center;
}
</style>