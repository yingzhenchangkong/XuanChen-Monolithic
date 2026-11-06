<template>
	<view class="fm-mask" v-if="visible" @click.stop="$emit('close')">
		<view class="fm-panel" @click.stop>
			<view class="fm-header">
				<text class="fm-title">{{ title }}</text>
				<text class="fm-close" @click="$emit('close')">×</text>
			</view>
			<view class="fm-body">
				<view class="fm-field" v-for="f in fields" :key="f.key">
					<text class="fm-label">{{ f.label }}</text>
					<switch
						v-if="f.type === 'switch'"
						:checked="form[f.key] === 1"
						color="#1890ff"
						@change="form[f.key] = $event.detail.value ? 1 : 0"
					/>
					<textarea
						v-else-if="f.type === 'textarea'"
						class="fm-textarea"
						v-model="form[f.key]"
						:placeholder="f.placeholder || '请输入'"
					/>
					<input
						v-else
						class="fm-input"
						:type="f.type === 'number' ? 'number' : 'text'"
						v-model="form[f.key]"
						:placeholder="f.placeholder || '请输入'"
					/>
				</view>
			</view>
			<view class="fm-footer">
				<button class="fm-btn fm-cancel" @click="$emit('close')">取消</button>
				<button class="fm-btn fm-ok" @click="handleSubmit">保存</button>
			</view>
		</view>
	</view>
</template>

<script setup>
const props = defineProps({
	visible: { type: Boolean, default: false },
	title: { type: String, default: '' },
	fields: { type: Array, default: () => [] },
	form: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close', 'submit'])

function handleSubmit() {
	emit('submit', { ...props.form })
}
</script>

<style scoped>
.fm-mask {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	z-index: 9999;
	display: flex;
	align-items: center;
	justify-content: center;
}
.fm-panel {
	width: 580rpx;
	max-height: 80vh;
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
	display: flex;
	flex-direction: column;
}
.fm-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 24rpx 28rpx;
	border-bottom: 1rpx solid #eee;
}
.fm-title { font-size: 30rpx; color: #333; font-weight: 600; }
.fm-close { font-size: 40rpx; color: #999; padding: 0 8rpx; }
.fm-body {
	padding: 12rpx 28rpx;
	overflow-y: auto;
}
.fm-field {
	display: flex;
	align-items: flex-start;
	padding: 18rpx 0;
	border-bottom: 1rpx solid #f5f5f5;
}
.fm-field:last-child { border-bottom: none; }
.fm-label {
	width: 160rpx;
	font-size: 26rpx;
	color: #666;
	line-height: 60rpx;
	flex-shrink: 0;
}
.fm-input, .fm-textarea {
	flex: 1;
	font-size: 26rpx;
	color: #333;
	height: 60rpx;
}
.fm-textarea { height: 120rpx; padding-top: 8rpx; }
.fm-footer {
	display: flex;
	padding: 20rpx 28rpx;
	border-top: 1rpx solid #eee;
	gap: 20rpx;
}
.fm-btn {
	flex: 1;
	font-size: 28rpx;
	line-height: 72rpx;
	border-radius: 12rpx;
	margin: 0;
}
.fm-cancel {
	background: #f5f5f5;
	color: #666;
}
.fm-ok {
	background: #1890ff;
	color: #fff;
}
</style>
