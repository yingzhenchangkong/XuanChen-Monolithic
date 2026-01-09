<template>
  <a-modal title="请完成安全验证" :width="360" v-model:open="visible" :footer="null" @cancel="handleCancel">
    <slide-verify @success="onSuccess" :imgs="customImages" @fail="onFail" slider-text="向右滑动验证" />
  </a-modal>
</template>

<script setup lang="ts">
//TODO: 1.与后台结合
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import SlideVerify from 'vue3-slide-verify';
import "vue3-slide-verify/dist/style.css";
import bg1 from '@/assets/images/slideverify/bg1.png';
import bg2 from '@/assets/images/slideverify/bg2.png';
import bg3 from '@/assets/images/slideverify/bg3.png';
import bg4 from '@/assets/images/slideverify/bg4.png';
import bg5 from '@/assets/images/slideverify/bg5.png';
import bg6 from '@/assets/images/slideverify/bg6.png';

const customImages = ref([bg1, bg2, bg3, bg4, bg5, bg6]);
const emit = defineEmits(['childOK']);
const visible = ref(false);
// 滑动验证状态
const isVerified = ref(false);

// 滑动验证成功回调
const onSuccess = () => {
  isVerified.value = true;
  visible.value = false;
  emit('childOK');
};

// 滑动验证失败回调
const onFail = () => {
  message.error('验证失败,请重新验证');
};
const handleCancel = () => {
  visible.value = false;
}

//打开弹窗
const show = () => {
  visible.value = true;
}
//子组件方法默认为私有
defineExpose({
  show
})
</script>