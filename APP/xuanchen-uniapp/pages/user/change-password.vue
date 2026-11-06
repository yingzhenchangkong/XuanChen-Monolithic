<template>
	<view class="password-page">
		<view class="form-card">
			<view class="form-item">
				<text class="form-label">当前密码</text>
				<input
					class="form-input"
					type="text"
					:password="!showOld"
					placeholder="请输入当前密码"
					v-model="form.oldPassword"
				/>
				<text class="form-eye" @click="showOld = !showOld">{{ showOld ? '👁' : '👁‍🗨' }}</text>
			</view>
			<view class="form-divider" />
			<view class="form-item">
				<text class="form-label">新密码</text>
				<input
					class="form-input"
					type="text"
					:password="!showNew"
					placeholder="请输入新密码（至少6位）"
					v-model="form.newPassword"
				/>
				<text class="form-eye" @click="showNew = !showNew">{{ showNew ? '👁' : '👁‍🗨' }}</text>
			</view>
			<view class="form-divider" />
			<view class="form-item">
				<text class="form-label">确认密码</text>
				<input
					class="form-input"
					type="text"
					:password="!showConfirm"
					placeholder="请再次输入新密码"
					v-model="form.confirmPassword"
				/>
				<text class="form-eye" @click="showConfirm = !showConfirm">{{ showConfirm ? '👁' : '👁‍🗨' }}</text>
			</view>
		</view>

		<view class="password-tip">
			<text class="tip-title">密码要求：</text>
			<text class="tip-item">• 长度至少6位</text>
			<text class="tip-item">• 建议包含字母和数字</text>
		</view>

		<button class="submit-btn" :loading="loading" :disabled="loading" @click="handleSubmit">
			确认修改
		</button>
	</view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { putAction } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const form = reactive({
	oldPassword: '',
	newPassword: '',
	confirmPassword: ''
})
const showOld = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
const loading = ref(false)
const isForced = ref(false)

onMounted(() => {
	const pages = getCurrentPages()
	const page = pages[pages.length - 1]
	isForced.value = page.options?.forced === '1'
})

async function handleSubmit() {
	if (!form.oldPassword) return uni.showToast({ title: '请输入当前密码', icon: 'none' })
	if (!form.newPassword) return uni.showToast({ title: '请输入新密码', icon: 'none' })
	if (form.newPassword.length < 6) return uni.showToast({ title: '新密码至少6位', icon: 'none' })
	if (form.newPassword !== form.confirmPassword) return uni.showToast({ title: '两次密码不一致', icon: 'none' })
	if (form.oldPassword === form.newPassword) return uni.showToast({ title: '新密码不能与旧密码相同', icon: 'none' })

	loading.value = true
	try {
		const res = await putAction('/system/account/changePassword', {
			oldPassword: form.oldPassword,
			newPassword: form.newPassword
		}, { showLoading: true })
		if (res.code === 200) {
			uni.showToast({ title: '密码修改成功', icon: 'success' })
			if (isForced.value) {
				authStore.updateUserInfo({ pwdResetRequired: 0 })
				setTimeout(() => {
					uni.reLaunch({ url: '/pages/index/index' })
				}, 1000)
			} else {
				setTimeout(() => {
					uni.navigateBack()
				}, 1000)
			}
		}
	} catch {} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.password-page {
	min-height: 100vh;
	background: #f5f6fa;
	padding: 24rpx;
}
.form-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 0 28rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.form-item {
	display: flex;
	align-items: center;
	padding: 28rpx 0;
}
.form-label {
	font-size: 28rpx;
	color: #333;
	width: 160rpx;
	flex-shrink: 0;
}
.form-input {
	flex: 1;
	font-size: 28rpx;
	color: #333;
	height: 44rpx;
}
.form-eye {
	font-size: 36rpx;
	padding: 0 10rpx;
}
.form-divider {
	height: 1rpx;
	background: #f0f0f0;
}
.password-tip {
	margin: 24rpx 12rpx;
	padding: 20rpx 24rpx;
	background: #fffbe6;
	border-radius: 12rpx;
	border: 1rpx solid #ffe58f;
}
.tip-title { font-size: 24rpx; color: #d48806; display: block; margin-bottom: 8rpx; }
.tip-item { font-size: 24rpx; color: #ad8b00; display: block; line-height: 1.8; }

.submit-btn {
	margin-top: 50rpx;
	width: 100%;
	height: 88rpx;
	background: #1890ff;
	color: #fff;
	font-size: 30rpx;
	border-radius: 44rpx;
	border: none;
	display: flex;
	align-items: center;
	justify-content: center;
}
.submit-btn[disabled] { opacity: 0.6; }
</style>
