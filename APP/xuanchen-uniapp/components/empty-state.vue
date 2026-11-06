<template>
	<view class="empty-state">
		<text class="empty-icon">{{ icon }}</text>
		<text class="empty-text">{{ text }}</text>
		<view class="empty-action" v-if="actionText" @click="$emit('action')">
			<text class="empty-action-text">{{ actionText }}</text>
		</view>
	</view>
</template>

<script setup>
defineProps({
	icon: { type: String, default: '∅' },
	text: { type: String, default: '暂无数据' },
	actionText: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<style scoped>
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 80rpx 40rpx;
}
.empty-icon {
	font-size: 80rpx;
	color: #d0d0d0;
	margin-bottom: 20rpx;
}
.empty-text {
	font-size: 28rpx;
	color: #999;
}
.empty-action {
	margin-top: 30rpx;
	padding: 12rpx 40rpx;
	background: #1890ff;
	border-radius: 30rpx;
}
.empty-action-text {
	font-size: 26rpx;
	color: #fff;
}
</style>
