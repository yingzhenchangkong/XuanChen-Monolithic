<template>
	<view class="page">
		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">CPU</text>
				<text class="refresh-link" @click="loadInfo">刷新</text>
			</view>
			<view class="usage-bar-wrap">
				<view class="usage-bar" :style="{ width: (info.cpuUsed || 0) + '%' }" />
			</view>
			<view class="mini-grid">
				<text class="mini-item">核心数：{{ info.cpuNum ?? '-' }}</text>
				<text class="mini-item">用户使用：{{ info.cpuUsed ?? '-' }}%</text>
				<text class="mini-item">系统使用：{{ info.cpuSys ?? '-' }}%</text>
				<text class="mini-item">空闲：{{ info.cpuFree ?? '-' }}%</text>
			</view>
		</view>

		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">内存</text>
			</view>
			<view class="usage-bar-wrap">
				<view class="usage-bar" :style="{ width: (info.memUsage || 0) + '%' }" />
			</view>
			<view class="mini-grid">
				<text class="mini-item">总内存：{{ fmtBytes(info.totalMem) }}</text>
				<text class="mini-item">已用：{{ fmtBytes(info.usedMem) }}</text>
				<text class="mini-item">空闲：{{ fmtBytes(info.freeMem) }}</text>
				<text class="mini-item">使用率：{{ info.memUsage ?? '-' }}%</text>
			</view>
		</view>

		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">JVM</text>
			</view>
			<view class="usage-bar-wrap">
				<view class="usage-bar" :style="{ width: (info.jvmUsage || 0) + '%' }" />
			</view>
			<view class="mini-grid">
				<text class="mini-item">总内存：{{ fmtBytes(info.jvmTotal) }}</text>
				<text class="mini-item">已用：{{ fmtBytes(info.jvmUsed) }}</text>
				<text class="mini-item">空闲：{{ fmtBytes(info.jvmFree) }}</text>
				<text class="mini-item">使用率：{{ info.jvmUsage ?? '-' }}%</text>
				<text class="mini-item full">Java 版本：{{ info.jvmVersion || '-' }}</text>
				<text class="mini-item full">名称：{{ info.jvmName || '-' }}</text>
			</view>
		</view>

		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">磁盘</text>
			</view>
			<view class="disk-item" v-for="(d, i) in disks" :key="i">
				<view class="disk-row">
					<text class="disk-name">{{ d.name }}</text>
					<text class="disk-usage">使用率 {{ d.usage }}%</text>
				</view>
				<view class="usage-bar-wrap">
					<view class="usage-bar" :style="{ width: (d.usage || 0) + '%' }" />
				</view>
				<text class="usage-text">总 {{ fmtBytes(d.total) }} · 已用 {{ fmtBytes(d.used) }} · 剩余 {{ fmtBytes(d.free) }}</text>
			</view>
			<empty-state v-if="disks.length === 0" text="暂无磁盘信息" icon="📭" />
		</view>

		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">系统</text>
			</view>
			<view class="mini-grid">
				<text class="mini-item full">操作系统：{{ info.osName || '-' }}</text>
				<text class="mini-item full">主机名：{{ info.computerName || '-' }}</text>
				<text class="mini-item full">安装路径：{{ info.userDir || '-' }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const info = ref({})
const disks = ref([])

function fmtBytes(bytes) {
	const n = Number(bytes)
	if (!n && n !== 0) return '-'
	if (n >= 1073741824) return (n / 1073741824).toFixed(2) + ' GB'
	if (n >= 1048576) return (n / 1048576).toFixed(1) + ' MB'
	if (n >= 1024) return (n / 1024).toFixed(1) + ' KB'
	return n + ' B'
}

async function loadInfo() {
	try {
		const res = await getAction('/monitor/server/info', null, { showLoading: true })
		if (res.code === 200 && res.data) {
			info.value = res.data
			disks.value = res.data.diskInfos || []
		}
	} catch {}
}

onMounted(() => {
	loadInfo()
})
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f6fa;
	padding: 20rpx 24rpx 60rpx;
}
.section-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.section-card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20rpx;
}
.card-title { font-size: 30rpx; color: #333; font-weight: 600; }
.refresh-link { font-size: 24rpx; color: #1890ff; }
.usage-bar-wrap {
	height: 20rpx;
	background: #f0f2f5;
	border-radius: 10rpx;
	overflow: hidden;
	margin-bottom: 12rpx;
}
.usage-bar {
	height: 100%;
	background: linear-gradient(90deg, #1890ff, #69c0ff);
	border-radius: 10rpx;
}
.usage-text { font-size: 22rpx; color: #999; }
.mini-grid {
	display: flex;
	flex-wrap: wrap;
	margin-top: 12rpx;
}
.mini-item {
	width: 50%;
	font-size: 24rpx;
	color: #555;
	padding: 8rpx 0;
}
.mini-item.full { width: 100%; word-break: break-all; }
.disk-item {
	margin-bottom: 24rpx;
}
.disk-item:last-child { margin-bottom: 0; }
.disk-row {
	display: flex;
	justify-content: space-between;
	margin-bottom: 8rpx;
}
.disk-name { font-size: 26rpx; color: #333; font-weight: 600; }
.disk-usage { font-size: 22rpx; color: #1890ff; }
</style>
