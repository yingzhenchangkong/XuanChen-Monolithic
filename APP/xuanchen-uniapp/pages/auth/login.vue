<template>
	<view class="login-page">
		<view class="login-header">
			<text class="login-app-name">萱晨</text>
			<text class="login-slogan">智慧管理，高效协同</text>
		</view>

		<view class="login-form">
			<view class="form-item">
				<text class="form-icon">👤</text>
				<input
					class="form-input"
					type="text"
					placeholder="请输入用户名"
					v-model="form.userName"
					:focus="false"
					@confirm="handleLogin"
				/>
			</view>

			<view class="form-item">
				<text class="form-icon">🔒</text>
				<input
					class="form-input"
					type="text"
					:password="!showPassword"
					placeholder="请输入密码"
					v-model="form.password"
					:focus="false"
					@confirm="handleLogin"
				/>
				<text class="form-eye" @click="showPassword = !showPassword">
					{{ showPassword ? '👁' : '👁‍🗨' }}
				</text>
			</view>

			<view class="form-options">
				<view class="remember-me" @click="rememberMe = !rememberMe">
					<view :class="['remember-checkbox', { checked: rememberMe }]">
						<text v-if="rememberMe" class="remember-check">✓</text>
					</view>
					<text class="remember-text">记住我</text>
				</view>
				<text class="forgot-link" @click="goRegister">注册账号</text>
			</view>

			<button class="login-btn" :loading="loading" :disabled="loading" @click="handleLogin">
				登 录
			</button>
		</view>

		<slide-verify
			:visible="showSlideVerify"
			@update:visible="showSlideVerify = $event"
			@success="onCaptchaSuccess"
			@close="showSlideVerify = false"
		/>
	</view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'
import SlideVerify from '@/components/slide-verify.vue'

const authStore = useAuthStore()

const form = reactive({
	userName: '',
	password: ''
})
const showPassword = ref(false)
const rememberMe = ref(false)
const loading = ref(false)
const showSlideVerify = ref(false)
const captchaEnabled = ref(false)
const captchaToken = ref('')
const captchaId = ref('')

onMounted(async () => {
	try {
		const res = await getAction('/system/config/getConfigKeyValue', { configKey: 'captchaEnabled' }, { showError: false, showLoading: false })
		if (res.code === 200 && res.data) {
			captchaEnabled.value = res.data.configValue === 'true' || res.data.configValue === true
		}
	} catch {}
})

async function handleLogin() {
	if (!form.userName.trim()) {
		return uni.showToast({ title: '请输入用户名', icon: 'none' })
	}
	if (!form.password.trim()) {
		return uni.showToast({ title: '请输入密码', icon: 'none' })
	}

	if (captchaEnabled.value && !captchaToken.value) {
		showSlideVerify.value = true
		return
	}

	await doLogin()
}

function onCaptchaSuccess(data) {
	captchaId.value = data.captchaId
	captchaToken.value = data.token
	showSlideVerify.value = false
	doLogin()
}

async function doLogin() {
	loading.value = true
	try {
		const data = await authStore.login({
			userName: form.userName,
			password: form.password,
			captchaId: captchaId.value,
			captchaToken: captchaToken.value,
			rememberMe: rememberMe.value
		})

		if (data.pwdResetRequired === 1) {
			uni.showToast({ title: '请修改密码', icon: 'none' })
			uni.redirectTo({ url: '/pages/user/change-password?forced=1' })
			return
		}

		uni.showToast({ title: '登录成功', icon: 'success' })
		setTimeout(() => {
			uni.switchTab({ url: '/pages/index/index' })
		}, 500)
	} catch (e) {
		captchaToken.value = ''
		captchaId.value = ''
		console.error('登录失败:', e)
	} finally {
		loading.value = false
	}
}

function goRegister() {
	uni.navigateTo({ url: '/pages/auth/register' })
}
</script>

<style scoped>
.login-page {
	min-height: 100vh;
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 0 50rpx;
	display: flex;
	flex-direction: column;
}
.login-header {
	padding-top: 180rpx;
	padding-bottom: 80rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}
.login-app-name {
	font-size: 64rpx;
	color: #fff;
	font-weight: bold;
	letter-spacing: 8rpx;
}
.login-slogan {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.75);
	margin-top: 16rpx;
}
.login-form {
	background: #fff;
	border-radius: 24rpx;
	padding: 50rpx 40rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.12);
}
.form-item {
	display: flex;
	align-items: center;
	border-bottom: 1rpx solid #eee;
	padding: 24rpx 0;
	margin-bottom: 10rpx;
}
.form-icon {
	font-size: 36rpx;
	color: #999;
	margin-right: 20rpx;
	width: 40rpx;
	text-align: center;
}
.form-input {
	flex: 1;
	font-size: 30rpx;
	color: #333;
	height: 44rpx;
}
.form-eye {
	font-size: 36rpx;
	padding: 0 10rpx;
}
.form-options {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin: 30rpx 0 40rpx;
}
.remember-me {
	display: flex;
	align-items: center;
}
.remember-checkbox {
	width: 32rpx;
	height: 32rpx;
	border: 2rpx solid #d9d9d9;
	border-radius: 6rpx;
	margin-right: 10rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.remember-checkbox.checked {
	background: #1890ff;
	border-color: #1890ff;
}
.remember-check {
	font-size: 20rpx;
	color: #fff;
}
.remember-text {
	font-size: 26rpx;
	color: #666;
}
.forgot-link {
	font-size: 26rpx;
	color: #1890ff;
}
.login-btn {
	width: 100%;
	height: 88rpx;
	background: #1890ff;
	color: #fff;
	font-size: 32rpx;
	border-radius: 44rpx;
	border: none;
	letter-spacing: 8rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.login-btn[disabled] {
	opacity: 0.6;
}
</style>
