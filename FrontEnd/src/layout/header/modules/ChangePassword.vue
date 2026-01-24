<template>
  <a-modal v-model:open="visible" title="修改密码" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" @keyup.enter="handleOk" class="modal-form-style">
      <a-form-item label="新密码" name="password" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-password v-model:value="model.password" placeholder="请输入新密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-password v-model:value="model.confirmPassword" placeholder="请再次输入新密码" allowClear autocomplete="off" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import { getAction, postAction } from '@/utils/httpAction';
import { useRouter } from 'vue-router';
import type { Rule } from 'ant-design-vue/es/form';

const router = useRouter();
const authStore = useAuthStore();

const visible = ref(false);

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const model = reactive({
  password: '',
  confirmPassword: '',
})

const url = {
  changePassword: '/system/user/changePassword',
  logout: '/logout',
}

const validateConfirmPassword = (_rule: Rule, value: string) => {
  if (value !== model.password) {
    return Promise.reject('两次输入的密码不一致！');
  } else {
    return Promise.resolve();
  }
}

const rulesRef = ref();
const rules = {
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, trigger: 'blur' }
  ],
  confirmPassword: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

const show = () => {
  visible.value = true;
  model.password = '';
  model.confirmPassword = '';
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
};

const handleOk = () => {
  rulesRef.value.validate().then(() => {
    postAction(url.changePassword, { token: authStore.getToken(), password: model.password }).then((res: any) => {
      if (res.code === 200) {
        message.success(res.msg);
        getAction(url.logout, {}).then(() => {
          window.sessionStorage.clear();
          router.push("/login");
        });
      } else {
        message.error(res.msg);
      }
    })
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
}

defineExpose({
  show
})
</script>