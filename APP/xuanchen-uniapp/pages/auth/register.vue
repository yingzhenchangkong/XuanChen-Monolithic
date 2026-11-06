<template>
	<view class="register-page">
		<view class="register-header">
			<text class="register-title">注册账号</text>
		</view>

		<view class="register-form">
			<view class="form-item">
				<input class="form-input" type="text" placeholder="请输入用户名" v-model="form.userName" />
			</view>
			<view class="form-item">
				<input class="form-input" type="text" placeholder="请输入邮箱" v-model="form.email" />
			</view>
			<view class="form-item">
				<input class="form-input" type="text" :password="!showPassword" placeholder="请输入密码" v-model="form.password" />
				<text class="form-eye" @click="showPassword = !showPassword">{{ showPassword ? '👁' : '👁‍🗨' }}</text>
			</view>
			<view class="form-item">
				<input class="form-input" type="text" :password="!showConfirm" placeholder="请确认密码" v-model="form.confirmPassword" />
				<text class="form-eye" @click="showConfirm = !showConfirm">{{ showConfirm ? '👁' : '👁‍🗨' }}</text>
			</view>

			<button class="register-btn" :loading="loading" :disabled="loading" @click="handleRegister">
				注 册
			</button>

			<view class="login-link" @click="goLogin">
				<text class="link-text">已有账号？</text>
				<text class="link-highlight">去登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { postAction } from '@/utils/http'

const form = reactive({
	userName: '',
	email: '',
	password: '',
	confirmPassword: ''
})
const showPassword = ref(false)
const showConfirm = ref(false)
const loading = ref(false)

async function handleRegister() {
	if (!form.userName.trim()) return uni.showToast({ title: '请输入用户名', icon: 'none' })
	if (!form.email.trim()) return uni.showToast({ title: '请输入邮箱', icon: 'none' })
	if (!form.password.trim()) return uni.showToast({ title: '请输入密码', icon: 'none' })
	if (form.password.length < 6) return uni.showToast({ title: '密码至少6位', icon: 'none' })
	if (form.password !== form.confirmPassword) return uni.showToast({ title: '两次密码不一致', icon: 'none' })

	loading.value = true
	try {
		const res = await postAction('/register', {
			userName: form.userName,
			email: form.email,
			password: form.password
		}, { showLoading: true })
		if (res.code === 200) {
			uni.showToast({ title: '注册成功', icon: 'success' })
			setTimeout(() => {
				uni.navigateBack()
			}, 1000)
		}
	} catch {} finally {
		loading.value = false
	}
}

function goLogin() {
	uni.navigateBack()
}
</script>

<style scoped>
.register-page {
	min-height: 100vh;
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 0 50rpx;
}
.register-header {
	padding-top: 180rpx;
	padding-bottom: 60rpx;
	display: flex;
	justify-content: center;
}
.register-title {
	font-size: 48rpx;
	color: #fff;
	font-weight: bold;
}
.register-form {
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
.register-btn {
	width: 100%;
	height: 88rpx;
	background: #1890ff;
	color: #fff;
	font-size: 32rpx;
	border-radius: 44rpx;
	border: none;
	margin-top: 50rpx;
	letter-spacing: 8rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.register-btn[disabled] { opacity: 0.6; }
.login-link {
	display: flex;
	justify-content: center;
	margin-top: 30rpx;
}
.link-text { font-size: 26rpx; color: #999; }
.link-highlight { font-size: 26rpx; color: #1890ff; }
</style>
