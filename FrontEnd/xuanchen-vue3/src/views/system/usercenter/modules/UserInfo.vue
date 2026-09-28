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
      <a-form-item :wrapper-col="{ offset: 18, span: 2 }">
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
// 会话信息理论上登录后必存在，极端情况下给空壳避免页面 NPE
const userInfo = userStore.getUserInfo() ?? { userName: '' };

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const model = reactive({
  id: userInfo.id,
  userName: userInfo.userName,
  nickName: userInfo.nickName,
  mobile: userInfo.mobile,
  email: userInfo.email,
})

const validateMobile = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateMobileApi(String(model.id ?? ''), value);
}
const validateEmail = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateEmailApi(String(model.id ?? ''), value);
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
  try {
    // validate() 校验不通过时 reject 的是 { values, errorFields } 对象，
    // 不能 catch 后把 error 拼进 message.error——否则会提示 "[object Object]"。
    // 表单字段下方已有规则提示，这里直接静默返回即可
    await rulesRef.value.validate();
  } catch {
    return;
  }
  try {
    const res = await userCenterEdit(model);
    if (res.code === 200) {
      message.success(res.msg);
      // 整体替换 store 中的用户对象（浅响应式要求引用变化才触发更新）
      userStore.setUserInfo({
        ...userInfo,
        nickName: model.nickName,
        mobile: model.mobile,
        email: model.email,
      });
    } else {
      // 业务失败（HTTP 200 + code!==200，如邮箱被占用）提示后端 msg；
      // HTTP 4xx/5xx 走 catch，由响应拦截器统一弹错，这里不再重复提示
      message.error(res.msg || '保存失败！');
    }
  } catch (error) {
    console.warn('【用户中心】保存失败:', error);
  }
}
</script>