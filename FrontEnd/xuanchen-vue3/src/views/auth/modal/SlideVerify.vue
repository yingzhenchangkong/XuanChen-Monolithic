<template>
  <a-modal title="请完成安全验证" :width="360" v-model:open="open" :footer="null" @cancel="handleCancel">
    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="加载验证码..." />
    </div>
    <div v-else-if="!bgImage" class="loading-container">
      <span>加载验证码失败</span>
    </div>
    <div v-else class="captcha-container">
      <div class="captcha-wrapper">
        <div class="captcha-bg" :style="{
          backgroundImage: `url(${bgImage})`,
          backgroundSize: `${captchaConfig.imageWidth}px ${captchaConfig.imageHeight}px`,
          width: captchaConfig.imageWidth + 'px',
          height: captchaConfig.imageHeight + 'px'
        }">
          <div class="slider-preview" :style="{
            left: sliderLeft + 'px',
            top: sliderTop + 'px',
            width: captchaConfig.sliderWidth + 'px',
            height: captchaConfig.sliderHeight + 'px'
          }">
            <img :src="sliderImage" alt="滑块" />
          </div>
        </div>
        <div class="slider-track" :style="{ width: captchaConfig.imageWidth + 'px' }">
          <div class="slider" :style="{ left: sliderLeft + 'px' }" :class="{ dragging: isDragging }"
            @mousedown="startDrag" @touchstart="startDrag">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="12 5 15 8 9 14 12 17"></polyline>
            </svg>
          </div>
          <span class="track-tip">拖动滑块完成验证</span>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { captchaGenerate, captchaVerify } from "@/views/auth/auth.api";
import type { CaptchaConfig } from "@/views/auth/auth.types";

const captchaId = ref<string>('');
const bgImage = ref<string>('');
const sliderImage = ref<string>('');
const captchaToken = ref<string>('');

const captchaConfig = reactive<CaptchaConfig>({
  imageWidth: 280,
  imageHeight: 150,
  sliderWidth: 50,
  sliderHeight: 50,
  sliderTop: 50
});

const open = ref(false);
const loading = ref(false);

// 滑块状态
const isDragging = ref(false);
const sliderLeft = ref(0);
const startX = ref(0);
const startLeft = ref(0);
const sliderTop = ref(0);

const emit = defineEmits(['childOK']);

const show = async () => {
  open.value = true;
  captchaToken.value = '';
  sliderLeft.value = 0;
  await initCaptcha();
}

const initCaptcha = async () => {
  loading.value = true;
  try {
    const res = await captchaGenerate();
    if (res.code === 200) {
      captchaId.value = res.data.captchaId;
      bgImage.value = res.data.bgImage;
      sliderImage.value = res.data.sliderImage;
      sliderTop.value = res.data.sliderTop !== undefined ? res.data.sliderTop : (captchaConfig.imageHeight - captchaConfig.sliderHeight) / 2;;

      // 更新配置
      if (res.data.imageWidth) {
        captchaConfig.imageWidth = res.data.imageWidth;
      }
      if (res.data.imageHeight) {
        captchaConfig.imageHeight = res.data.imageHeight;
      }
      if (res.data.sliderWidth) {
        captchaConfig.sliderWidth = res.data.sliderWidth;
      }
      if (res.data.sliderHeight) {
        captchaConfig.sliderHeight = res.data.sliderHeight;
      }
    } else {
      message.error(res.msg);
      bgImage.value = '';
    }
  } catch (error) {
    message.error('加载验证码失败');
    bgImage.value = '';
  } finally {
    loading.value = false;
  }
}

type DragEvent = MouseEvent | TouchEvent;
const getClientX = (e: DragEvent): number =>
  'touches' in e ? e.touches[0].clientX : e.clientX;

const startDrag = (e: DragEvent) => {
  if (isDragging.value) return;

  isDragging.value = true;
  startX.value = getClientX(e);
  startLeft.value = sliderLeft.value;

  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  document.addEventListener('touchmove', onDrag);
  document.addEventListener('touchend', stopDrag);
}

const onDrag = (e: DragEvent) => {
  if (!isDragging.value) return;

  const currentX = getClientX(e);
  let newLeft = currentX - startX.value + startLeft.value;

  // 限制在有效范围内
  const maxLeft = captchaConfig.imageWidth - captchaConfig.sliderWidth;
  newLeft = Math.max(0, Math.min(maxLeft, newLeft));
  sliderLeft.value = newLeft;
}

const stopDrag = async () => {
  isDragging.value = false;

  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);

  // 验证滑动位置
  await verifyCaptcha();
}

const verifyCaptcha = async () => {
  if (!captchaId.value) return;

  try {
    const res = await captchaVerify(captchaId.value, sliderLeft.value);
    if (res.code === 200) {
      captchaToken.value = res.data.captchaToken;
      setTimeout(() => {
        open.value = false;
        emit('childOK', captchaId.value, captchaToken.value);
      }, 500);
    } else {
      message.error(res.msg);
      await resetCaptcha();
    }
  } catch (error) {
    message.error('验证失败');
    await resetCaptcha();
  }
}

const resetCaptcha = async () => {
  sliderLeft.value = 0;
  await initCaptcha();
}

const handleCancel = () => {
  open.value = false;
  captchaToken.value = '';
  sliderLeft.value = 0;
}

const getVerifyToken = () => {
  return captchaToken.value;
}

const reset = async () => {
  captchaToken.value = '';
  sliderLeft.value = 0;
  await initCaptcha();
}

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

.captcha-container {
  padding: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.captcha-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.captcha-bg {
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.slider-preview {
  position: absolute;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}

.slider-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.slider-track {
  position: relative;
  height: 40px;
  background: rgba(240, 240, 240, 0.9);
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slider {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  cursor: grab;
  transition: transform 0.1s, box-shadow 0.1s;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.4);
  z-index: 10;
}

.slider:hover {
  transform: translateY(-50%) scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.6);
}

.slider.dragging {
  cursor: grabbing;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.8);
}

.track-tip {
  font-size: 12px;
  color: #999;
  user-select: none;
}
</style>
