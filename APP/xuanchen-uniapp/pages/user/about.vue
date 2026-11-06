<template>
	<view class="about-page">
		<view class="about-header">
			<view class="app-icon-area">
				<text class="app-icon-text">萱</text>
			</view>
			<text class="app-name">萱晨</text>
			<text class="app-version">v1.0.0</text>
		</view>

		<view class="info-group">
			<view class="info-item">
				<text class="info-label">应用名称</text>
				<text class="info-value">萱晨管理系统</text>
			</view>
			<view class="info-divider" />
			<view class="info-item">
				<text class="info-label">版本号</text>
				<text class="info-value">1.0.0</text>
			</view>
			<view class="info-divider" />
			<view class="info-item">
				<text class="info-label">技术框架</text>
				<text class="info-value">uni-app + Vue 3</text>
			</view>
			<view class="info-divider" />
			<view class="info-item">
				<text class="info-label">后端服务</text>
				<text class="info-value">Spring Boot 4</text>
			</view>
		</view>

		<view class="copyright">
			<text class="copyright-text">© 2026 萱晨科技</text>
		</view>
	</view>
</template>

<script setup>
</script>

<style scoped>
.about-page {
	min-height: 100vh;
	background: #f5f6fa;
	display: flex;
	flex-direction: column;
	align-items: center;
}
.about-header {
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	width: 100%;
	padding: 80rpx 0 60rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}
.app-icon-area {
	width: 120rpx;
	height: 120rpx;
	background: rgba(255,255,255,0.2);
	border-radius: 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 20rpx;
}
.app-icon-text {
	font-size: 56rpx;
	color: #fff;
	font-weight: bold;
}
.app-name {
	font-size: 36rpx;
	color: #fff;
	font-weight: bold;
}
.app-version {
	font-size: 24rpx;
	color: rgba(255,255,255,0.7);
	margin-top: 8rpx;
}

.info-group {
	margin: -20rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 0 28rpx;
	box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
	position: relative;
	z-index: 2;
	width: calc(100% - 48rpx);
}
.info-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 28rpx 0;
}
.info-label { font-size: 28rpx; color: #666; }
.info-value { font-size: 28rpx; color: #333; }
.info-divider { height: 1rpx; background: #f0f0f0; }

.copyright {
	margin-top: 60rpx;
}
.copyright-text {
	font-size: 24rpx;
	color: #ccc;
}
</style>
