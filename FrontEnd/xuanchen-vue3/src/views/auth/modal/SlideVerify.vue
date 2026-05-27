<template>
  <a-modal title="请完成安全验证" :width="360" v-model:open="open" :footer="null" @cancel="handleCancel">
    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="加载验证码..." />
    </div>
    <slide-verify v-else @success="onSuccess" @fail="onFail" :imgs="captchaImages" slider-text="向右滑动验证"
      :offset="captchaOffset" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import SlideVerify from 'vue3-slide-verify';
import "vue3-slide-verify/dist/style.css";

import { captchaGenerate, captchaVerify } from "@/views/auth/auth.api";

import bg1 from '@/assets/images/slideverify/bg1.png';
import bg2 from '@/assets/images/slideverify/bg2.png';
import bg3 from '@/assets/images/slideverify/bg3.png';
import bg4 from '@/assets/images/slideverify/bg4.png';
import bg5 from '@/assets/images/slideverify/bg5.png';
import bg6 from '@/assets/images/slideverify/bg6.png';

const captchaId = ref<string>('');
const captchaImages = ref([bg1, bg2, bg3, bg4, bg5, bg6]);
const captchaOffset = ref<number>(88.88);
const captchaToken = ref<string>('');

const open = ref(false);
const loading = ref(false);

const emit = defineEmits(['childOK']);

//打开弹窗
const show = async () => {
  open.value = true;
  captchaToken.value = '';
  await initCaptcha();
}
//初始化验证码
const initCaptcha = async () => {
  loading.value = true;
  const res: any = await captchaGenerate();
  if (res.code === 200) {
    captchaId.value = res.data.captchaId;
    captchaOffset.value = res.data.captchaOffset;
  } else {
    message.error(res.msg);
  }
  loading.value = false;
}
// 滑动验证成功回调
const onSuccess = async (captchaInfo: any) => {
  try {
    if (captchaId.value) {
      const res: any = await captchaVerify(captchaId.value, captchaInfo.left);
      if (res.code === 200) {
        captchaToken.value = res.data.captchaToken;
        open.value = false;
        emit('childOK', captchaId.value, captchaToken.value);
      } else {
        await initCaptcha();
      }
    }
  } catch (error) {
    await initCaptcha();
  }
};

// 滑动验证失败回调
const onFail = async () => {
  message.error('验证失败,请重新验证');
  await initCaptcha();
};
const handleCancel = () => {
  open.value = false;
  captchaToken.value = '';
}

const getVerifyToken = () => {
  return captchaToken.value;
}

const reset = async () => {
  captchaToken.value = '';
  await initCaptcha();
}

//子组件方法默认为私有
defineExpose({
  show,
  getVerifyToken,
  reset
})
</script>

<style scoped>
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>