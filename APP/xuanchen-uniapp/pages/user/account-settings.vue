<template>
	<view class="settings-page">
		<view class="avatar-section">
			<view class="avatar-area" @click="handleAvatarTap">
				<image class="avatar" :src="avatarUrl" mode="aspectFill" v-if="avatarUrl" />
				<view class="avatar-placeholder" v-else>
					<text class="placeholder-text">{{ avatarText }}</text>
				</view>
				<view class="avatar-edit">
					<text class="edit-icon">📷</text>
				</view>
			</view>
			<text class="avatar-tip">点击更换头像</text>
		</view>

		<view class="form-card">
			<view class="form-item">
				<text class="form-label">用户名</text>
				<text class="form-value disabled">{{ authStore.userName }}</text>
			</view>
			<view class="form-divider" />
			<view class="form-item">
				<text class="form-label">昵称</text>
				<input class="form-input" type="text" placeholder="请输入昵称" v-model="form.nickName" />
			</view>
			<view class="form-divider" />
			<view class="form-item">
				<text class="form-label">手机号</text>
				<input class="form-input" type="number" placeholder="请输入手机号" v-model="form.mobile" maxlength="11" />
			</view>
			<view class="form-divider" />
			<view class="form-item">
				<text class="form-label">邮箱</text>
				<input class="form-input" type="text" placeholder="请输入邮箱" v-model="form.email" />
			</view>
		</view>

		<button class="save-btn" :loading="loading" :disabled="loading" @click="handleSave">
			保存修改
		</button>
	</view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { putAction, uploadAction } from '@/utils/http'
import { config } from '@/utils/config'

const authStore = useAuthStore()

const form = reactive({
	nickName: '',
	mobile: '',
	email: ''
})
const loading = ref(false)

const avatarUrl = computed(() => {
	if (authStore.avatar) {
		return config.baseUrl + '/filemanage/static/' + authStore.avatar
	}
	return ''
})

const avatarText = computed(() => {
	const name = authStore.nickName || authStore.userName || ''
	return name.charAt(0).toUpperCase()
})

onMounted(() => {
	form.nickName = authStore.userInfo?.nickName || ''
	form.mobile = authStore.userInfo?.mobile || ''
	form.email = authStore.userInfo?.email || ''
})

function handleAvatarTap() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			const tempFilePath = res.tempFilePaths[0]
			uni.showLoading({ title: '上传中...' })
			try {
				const uploadRes = await uploadAction('/filemanage/upload', {
					filePath: tempFilePath,
					name: 'file'
				}, { showLoading: false })
				if (uploadRes.code === 200 && uploadRes.data) {
					await putAction('/system/account/userCenterUpdateAvatar', {
						avatar: uploadRes.data
					})
					authStore.updateUserInfo({ avatar: uploadRes.data })
					uni.showToast({ title: '头像更新成功', icon: 'success' })
				}
			} catch {} finally {
				uni.hideLoading()
			}
		}
	})
}

async function handleSave() {
	if (!form.nickName.trim()) return uni.showToast({ title: '请输入昵称', icon: 'none' })
	if (form.mobile && !/^1\d{10}$/.test(form.mobile)) return uni.showToast({ title: '手机号格式不正确', icon: 'none' })
	if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return uni.showToast({ title: '邮箱格式不正确', icon: 'none' })

	loading.value = true
	try {
		const res = await putAction('/system/account/userCenterEdit', {
			nickName: form.nickName,
			mobile: form.mobile,
			email: form.email
		}, { showLoading: true })
		if (res.code === 200) {
			authStore.updateUserInfo({
				nickName: form.nickName,
				mobile: form.mobile,
				email: form.email
			})
			uni.showToast({ title: '保存成功', icon: 'success' })
			setTimeout(() => uni.navigateBack(), 800)
		}
	} catch {} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.settings-page {
	min-height: 100vh;
	background: #f5f6fa;
	padding-bottom: 40rpx;
}
.avatar-section {
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 50rpx 0 60rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}
.avatar-area {
	position: relative;
	width: 140rpx;
	height: 140rpx;
}
.avatar {
	width: 140rpx;
	height: 140rpx;
	border-radius: 70rpx;
	border: 4rpx solid rgba(255,255,255,0.5);
}
.avatar-placeholder {
	width: 140rpx;
	height: 140rpx;
	border-radius: 70rpx;
	background: rgba(255,255,255,0.3);
	display: flex;
	align-items: center;
	justify-content: center;
	border: 4rpx solid rgba(255,255,255,0.5);
}
.placeholder-text {
	font-size: 52rpx;
	color: #fff;
	font-weight: bold;
}
.avatar-edit {
	position: absolute;
	bottom: 0;
	right: 0;
	width: 44rpx;
	height: 44rpx;
	background: #fff;
	border-radius: 22rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.15);
}
.edit-icon { font-size: 24rpx; }
.avatar-tip {
	font-size: 24rpx;
	color: rgba(255,255,255,0.7);
	margin-top: 16rpx;
}

.form-card {
	margin: -30rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 0 28rpx;
	box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
	position: relative;
	z-index: 2;
}
.form-item {
	display: flex;
	align-items: center;
	padding: 28rpx 0;
}
.form-label {
	font-size: 28rpx;
	color: #333;
	width: 140rpx;
	flex-shrink: 0;
}
.form-input {
	flex: 1;
	font-size: 28rpx;
	color: #333;
	height: 44rpx;
	text-align: right;
}
.form-value {
	flex: 1;
	font-size: 28rpx;
	color: #999;
	text-align: right;
}
.form-value.disabled { color: #bbb; }
.form-divider {
	height: 1rpx;
	background: #f0f0f0;
}

.save-btn {
	margin: 40rpx 24rpx;
	width: calc(100% - 48rpx);
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
.save-btn[disabled] { opacity: 0.6; }
</style>
