<template>
	<view class="screen-page">
		<view class="page-header">
			<text class="page-title">数据大屏</text>
		</view>

		<view class="stat-grid">
			<view class="stat-card" v-for="s in stats" :key="s.label">
				<text class="stat-value" :style="{ color: s.color }">{{ s.value }}</text>
				<text class="stat-label">{{ s.label }}</text>
			</view>
		</view>

		<view class="chart-card">
			<view class="chart-header">
				<text class="chart-title">近7日访问趋势</text>
			</view>
			<view class="bar-chart">
				<view class="bar-col" v-for="d in trend" :key="d.day">
					<view class="bar-track">
						<view class="bar-fill" :style="{ height: d.percent + '%' }" />
					</view>
					<text class="bar-label">{{ d.day }}</text>
				</view>
			</view>
		</view>

		<view class="chart-card">
			<view class="chart-header">
				<text class="chart-title">待办事项</text>
			</view>
			<view class="todo-list">
				<view class="todo-item" v-for="t in todos" :key="t.title">
					<view :class="['todo-dot', t.level]" />
					<text class="todo-title">{{ t.title }}</text>
					<text class="todo-count">{{ t.count }}</text>
				</view>
			</view>
		</view>

		<view class="tips">
			<text class="tips-text">示例数据 · 大屏接口接入后自动更新</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'

const stats = ref([
	{ label: '今日访问', value: '1,286', color: '#1890ff' },
	{ label: '在线用户', value: '32', color: '#52c41a' },
	{ label: '未读通知', value: '5', color: '#fa8c16' },
	{ label: '待办事项', value: '12', color: '#f5222d' }
])

const trend = ref([
	{ day: '周一', percent: 45 },
	{ day: '周二', percent: 62 },
	{ day: '周三', percent: 38 },
	{ day: '周四', percent: 78 },
	{ day: '周五', percent: 90 },
	{ day: '周六', percent: 55 },
	{ day: '周日', percent: 30 }
])

const todos = ref([
	{ title: '待审批流程', count: '6 项', level: 'high' },
	{ title: '异常日志', count: '3 条', level: 'high' },
	{ title: '账号审核', count: '2 个', level: 'mid' },
	{ title: '系统公告待发布', count: '1 篇', level: 'low' }
])
</script>

<style scoped>
.screen-page {
	min-height: 100vh;
	background: #f5f6fa;
	padding-bottom: 30rpx;
}
.page-header {
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 60rpx 36rpx 50rpx;
}
.page-title {
	font-size: 40rpx;
	color: #fff;
	font-weight: bold;
}
.stat-grid {
	display: flex;
	flex-wrap: wrap;
	margin: 24rpx 24rpx 0;
}
.stat-card {
	width: calc(50% - 12rpx);
	background: #fff;
	border-radius: 16rpx;
	padding: 30rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 20rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.stat-card:nth-child(odd) { margin-right: 24rpx; }
.stat-value { font-size: 44rpx; font-weight: bold; }
.stat-label { font-size: 24rpx; color: #999; margin-top: 8rpx; }
.chart-card {
	margin: 0 24rpx 24rpx;
	background: #fff;
	border-radius: 16rpx;
	padding: 28rpx 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.chart-header { margin-bottom: 24rpx; }
.chart-title { font-size: 30rpx; color: #333; font-weight: 600; }
.bar-chart {
	display: flex;
	align-items: flex-end;
}
.bar-col {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
}
.bar-track {
	width: 28rpx;
	height: 240rpx;
	background: #f0f2f5;
	border-radius: 8rpx;
	display: flex;
	align-items: flex-end;
	overflow: hidden;
}
.bar-fill {
	width: 100%;
	background: linear-gradient(180deg, #1890ff 0%, #69c0ff 100%);
	border-radius: 8rpx;
}
.bar-label { font-size: 20rpx; color: #999; margin-top: 12rpx; }
.todo-list { display: flex; flex-direction: column; }
.todo-item {
	display: flex;
	align-items: center;
	padding: 20rpx 0;
	border-bottom: 1rpx solid #f5f5f5;
}
.todo-item:last-child { border-bottom: none; }
.todo-dot {
	width: 14rpx;
	height: 14rpx;
	border-radius: 50%;
	margin-right: 16rpx;
	flex-shrink: 0;
}
.todo-dot.high { background: #f5222d; }
.todo-dot.mid { background: #fa8c16; }
.todo-dot.low { background: #52c41a; }
.todo-title { flex: 1; font-size: 28rpx; color: #333; }
.todo-count { font-size: 26rpx; color: #999; }
.tips {
	display: flex;
	justify-content: center;
	margin-top: 10rpx;
}
.tips-text { font-size: 22rpx; color: #bbb; }
</style>
