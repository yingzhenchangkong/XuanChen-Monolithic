<template>
	<view class="slide-verify-mask" v-if="visible" @click.stop="handleClose">
		<view class="slide-verify-panel" @click.stop>
			<view class="sv-header">
				<text class="sv-title">安全验证</text>
				<text class="sv-close" @click="handleClose">×</text>
			</view>
			<view class="sv-image-area" :style="{ width: imageWidth + 'px', height: imageHeight + 'px' }">
				<image
					class="sv-bg-image"
					:src="bgImage"
					mode="aspectFill"
					:style="{ width: imageWidth + 'px', height: imageHeight + 'px' }"
				/>
				<image
					class="sv-block-image"
					:src="blockImage"
					mode="aspectFill"
					:style="{
						width: blockWidth + 'px',
						height: blockHeight + 'px',
						top: blockTop + 'px',
						left: sliderX + 'px'
					}"
				/>
				<view class="sv-tip" v-if="showTip">
					<text :class="['sv-tip-text', resultClass]">{{ tipText }}</text>
				</view>
			</view>
			<view class="sv-slider-area" :style="{ width: imageWidth + 'px' }">
				<view class="sv-slider-track">
					<view class="sv-slider-fill" :style="{ width: sliderX + 'px' }" />
					<text class="sv-slider-placeholder" v-if="sliderX === 0">向右滑动完成验证</text>
				</view>
				<view
					class="sv-slider-handle"
					:style="{ left: sliderX + 'px' }"
					@touchstart.prevent="onTouchStart"
					@touchmove.prevent="onTouchMove"
					@touchend="onTouchEnd"
				>
					<text class="sv-slider-arrow">»</text>
				</view>
			</view>
			<view class="sv-footer">
				<text class="sv-refresh" @click="handleRefresh">刷新</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { getAction, postAction } from '@/utils/http'

const props = defineProps({
	visible: { type: Boolean, default: false }
})

const emit = defineEmits(['success', 'fail', 'close', 'update:visible'])

const imageWidth = ref(280)
const imageHeight = ref(150)
const blockWidth = ref(50)
const blockHeight = ref(50)
const bgImage = ref('')
const blockImage = ref('')
const captchaId = ref('')
const sliderX = ref(0)
const startX = ref(0)
const blockTop = ref(0)
const showTip = ref(false)
const tipText = ref('')
const isVerified = ref(false)
const verifyResult = ref(null)
const gapMargin = ref(10)

const maxSliderX = computed(() => imageWidth.value - 40)

const resultClass = computed(() => {
	if (verifyResult.value === true) return 'sv-tip-success'
	if (verifyResult.value === false) return 'sv-tip-error'
	return ''
})

async function loadCaptcha() {
	try {
		const res = await getAction('/captcha/generate', null, { showError: false, showLoading: false })
		if (res.code === 200 && res.data) {
			bgImage.value = res.data.bgImage
			blockImage.value = res.data.sliderImage
			captchaId.value = res.data.captchaId
			blockTop.value = res.data.sliderTop || Math.floor(Math.random() * (imageHeight.value - blockHeight.value))
			if (res.data.imageWidth) imageWidth.value = res.data.imageWidth
			if (res.data.imageHeight) imageHeight.value = res.data.imageHeight
			if (res.data.sliderWidth) blockWidth.value = res.data.sliderWidth
			if (res.data.sliderHeight) blockHeight.value = res.data.sliderHeight
			resetSlider()
		}
	} catch (e) {
		console.error('加载验证码失败:', e)
	}
}
function resetSlider() {
	sliderX.value = 0
	showTip.value = false
	isVerified.value = false
	verifyResult.value = null
	tipText.value = ''
}

function onTouchStart(e) {
	if (isVerified.value) return
	startX.value = e.touches[0].clientX - sliderX.value
}

function onTouchMove(e) {
	if (isVerified.value) return
	const moveX = e.touches[0].clientX - startX.value
	sliderX.value = Math.max(0, Math.min(moveX, maxSliderX.value))
}

async function onTouchEnd() {
	if (isVerified.value || sliderX.value === 0) return
	try {
		const res = await postAction('/captcha/verify', {
			captchaId: captchaId.value,
			captchaOffset: sliderX.value
		}, { showError: false, showLoading: false })
		if (res.code === 200 && res.data?.captchaToken) {
			isVerified.value = true
			verifyResult.value = true
			tipText.value = '验证成功'
			showTip.value = true
			setTimeout(() => {
				emit('success', { captchaId: captchaId.value, token: res.data.captchaToken })
				emit('update:visible', false)
			}, 500)
		} else {
			verifyResult.value = false
			tipText.value = '验证失败，请重试'
			showTip.value = true
			setTimeout(() => {
				loadCaptcha()
			}, 1000)
		}
	} catch {
		verifyResult.value = false
		tipText.value = '验证失败，请重试'
		showTip.value = true
		setTimeout(() => loadCaptcha(), 1000)
	}
}

function handleRefresh() {
	loadCaptcha()
}

function handleClose() {
	emit('close')
	emit('update:visible', false)
	resetSlider()
}

defineExpose({ loadCaptcha, resetSlider })

import { watch } from 'vue'
watch(() => props.visible, (val) => {
	if (val) loadCaptcha()
})
</script>

<style scoped>
.slide-verify-mask {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	z-index: 9999;
	display: flex;
	align-items: center;
	justify-content: center;
}
.slide-verify-panel {
	width: 320px;
	background: #fff;
	border-radius: 12px;
	overflow: hidden;
}
.sv-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 14px 16px;
	border-bottom: 1px solid #eee;
}
.sv-title { font-size: 15px; color: #333; font-weight: 600; }
.sv-close { font-size: 22px; color: #999; padding: 0 4px; }
.sv-image-area {
	position: relative;
	margin: 12px auto 0;
	overflow: hidden;
	border-radius: 4px;
}
.sv-bg-image { position: absolute; top: 0; left: 0; }
.sv-block-image { position: absolute; left: 0; z-index: 2; }
.sv-tip {
	position: absolute;
	bottom: 0; left: 0; right: 0;
	height: 28px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(0,0,0,0.5);
	z-index: 3;
}
.sv-tip-text { font-size: 12px; color: #fff; }
.sv-tip-success { color: #67c23a; }
.sv-tip-error { color: #f56c6c; }
.sv-slider-area {
	position: relative;
	height: 40px;
	margin: 12px auto;
	background: #e8e8e8;
	border-radius: 20px;
}
.sv-slider-track {
	position: absolute;
	top: 0; left: 0; right: 0; bottom: 0;
	border-radius: 20px;
	overflow: hidden;
}
.sv-slider-fill {
	position: absolute;
	top: 0; left: 0; bottom: 0;
	background: rgba(24, 144, 255, 0.15);
	border-radius: 20px;
}
.sv-slider-placeholder {
	position: absolute;
	top: 50%; left: 50%;
	transform: translate(-50%, -50%);
	font-size: 13px;
	color: #999;
}
.sv-slider-handle {
	position: absolute;
	top: 0;
	width: 40px;
	height: 40px;
	background: #fff;
	border-radius: 50%;
	box-shadow: 0 2px 6px rgba(0,0,0,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 2;
}
.sv-slider-arrow {
	font-size: 18px;
	color: #1890ff;
	font-weight: bold;
}
.sv-footer {
	padding: 0 16px 12px;
	display: flex;
	justify-content: flex-end;
}
.sv-refresh {
	font-size: 13px;
	color: #1890ff;
}
</style>
