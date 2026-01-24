<template>
  <a-modal v-model:open="visible" title="重置密码" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" @keyup.enter="handleOk"
      class="modal-form-style">
      <a-form-item label="新密码" name="password" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-password v-model:value="model.password" placeholder="请输入新密码" allowClear autocomplete="off" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-password v-model:value="model.confirmPassword" placeholder="请输入确认密码" allowClear autocomplete="off" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { resetPassword } from '../user.api';

const visible = ref(false);

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const model = reactive({
  id: '',
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
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, trigger: 'blur' }
  ],
  confirmPassword: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

const show = (id: string) => {
  model.id = id;
  model.password = 'XuanChen@888888';
  model.confirmPassword = 'XuanChen@888888';
  visible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await resetPassword(model.id, model.password);
  message.success(res.msg);
  visible.value = false;
}

defineExpose({
  show,
});
</script>