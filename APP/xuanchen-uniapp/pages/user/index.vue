<template>
	<view class="user-page">
		<view class="user-header">
			<view class="avatar-area" @click="handleAvatarTap">
				<image
					class="avatar"
					:src="avatarUrl"
					mode="aspectFill"
				/>
			</view>
			<view class="user-info">
				<text class="nick-name">{{ authStore.nickName || authStore.userName }}</text>
				<text class="user-name">@{{ authStore.userName }}</text>
			</view>
		</view>

		<view class="menu-group">
			<view class="menu-item" @click="goPage('/pages/user/account-settings')">
				<view class="menu-left">
					<text class="menu-icon">👤</text>
					<text class="menu-label">个人信息</text>
				</view>
				<text class="menu-arrow">›</text>
			</view>
			<view class="menu-item" @click="goPage('/pages/user/change-password')">
				<view class="menu-left">
					<text class="menu-icon">🔒</text>
					<text class="menu-label">修改密码</text>
				</view>
				<text class="menu-arrow">›</text>
			</view>
		</view>

		<view class="menu-group">
			<view class="menu-item" @click="goPage('/pages/user/about')">
				<view class="menu-left">
					<text class="menu-icon">ℹ️</text>
					<text class="menu-label">关于</text>
				</view>
				<text class="menu-arrow">›</text>
			</view>
		</view>

		<view class="logout-area">
			<button class="logout-btn" @click="handleLogout">退出登录</button>
		</view>
	</view>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { config } from '@/utils/config'

const authStore = useAuthStore()

const avatarUrl = computed(() => {
	if (authStore.avatar) {
		return config.baseUrl + '/filemanage/static/' + authStore.avatar
	}
	return ''
})

function handleAvatarTap() {
	uni.navigateTo({ url: '/pages/user/account-settings' })
}

function goPage(url) {
	uni.navigateTo({ url })
}

function handleLogout() {
	uni.showModal({
		title: '提示',
		content: '确定退出登录？',
		success: (res) => {
			if (res.confirm) {
				authStore.logout()
			}
		}
	})
}
</script>

<style scoped>
.user-page {
	min-height: 100vh;
	background: #f5f6fa;
}
.user-header {
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 80rpx 40rpx 50rpx;
	display: flex;
	align-items: center;
}
.avatar-area {
	width: 120rpx;
	height: 120rpx;
	border-radius: 60rpx;
	overflow: hidden;
	border: 4rpx solid rgba(255,255,255,0.4);
	margin-right: 28rpx;
	flex-shrink: 0;
	background: rgba(255,255,255,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
}
.avatar {
	width: 120rpx;
	height: 120rpx;
}
.user-info {
	display: flex;
	flex-direction: column;
}
.nick-name {
	font-size: 36rpx;
	color: #fff;
	font-weight: bold;
}
.user-name {
	font-size: 26rpx;
	color: rgba(255,255,255,0.7);
	margin-top: 8rpx;
}

.menu-group {
	margin: 24rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.menu-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 30rpx 28rpx;
	border-bottom: 1rpx solid #f5f5f5;
}
.menu-item:last-child { border-bottom: none; }
.menu-left {
	display: flex;
	align-items: center;
}
.menu-icon {
	font-size: 36rpx;
	margin-right: 20rpx;
}
.menu-label {
	font-size: 28rpx;
	color: #333;
}
.menu-arrow {
	font-size: 32rpx;
	color: #ccc;
}

.logout-area {
	margin: 50rpx 24rpx;
}
.logout-btn {
	width: 100%;
	height: 88rpx;
	background: #fff;
	color: #ff4d4f;
	font-size: 30rpx;
	border-radius: 16rpx;
	border: none;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
</style>
