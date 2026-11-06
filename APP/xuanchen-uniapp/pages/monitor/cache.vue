<template>
	<view class="page">
		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">Redis 信息</text>
				<text class="refresh-link" @click="loadInfo">刷新</text>
			</view>
			<view class="usage-bar-wrap">
				<view class="usage-bar" :style="{ width: memoryPercent + '%' }" />
			</view>
			<text class="usage-text">内存使用 {{ info.usedMemoryHuman || '-' }} / 峰值 {{ info.usedMemoryPeakHuman || info.usedMemoryPeak || '-' }}</text>
			<view class="info-grid">
				<view class="info-cell" v-for="cell in cells" :key="cell.label">
					<text class="info-value">{{ cell.value }}</text>
					<text class="info-label">{{ cell.label }}</text>
				</view>
			</view>
		</view>

		<view class="section-card">
			<view class="section-card-header">
				<text class="card-title">Key 详情（{{ keys.length }}）</text>
				<text class="usage-text">共 {{ info.dbSize || 0 }} 个</text>
			</view>
			<view class="key-row" v-for="k in keys" :key="k.keyName">
				<view class="key-main">
					<text class="key-name">{{ k.keyName }}</text>
					<text class="key-sub">{{ k.type }} · {{ k.size }}</text>
				</view>
				<text class="key-ttl">TTL {{ k.ttl }}</text>
			</view>
			<empty-state v-if="keys.length === 0" text="暂无 Key" icon="📭" />
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const info = ref({})
const keys = ref([])

const memoryPercent = computed(() => {
	const used = Number(info.value.usedMemory)
	const max = Number(info.value.maxMemory)
	if (!used || !max || max <= 0) return 0
	return Math.min(100, Math.round((used / max) * 100))
})

const cells = computed(() => [
	{ label: '版本', value: info.value.version || '-' },
	{ label: '模式', value: info.value.mode || '-' },
	{ label: '连接客户端', value: info.value.connectedClients ?? '-' },
	{ label: '最大连接', value: info.value.maxClients ?? '-' },
	{ label: '每秒命令数', value: info.value.instantaneousOpsPerSec ?? '-' },
	{ label: '命中次数', value: info.value.keyspaceHits ?? '-' },
	{ label: '未命中次数', value: info.value.keyspaceMisses ?? '-' },
	{ label: '运行系统', value: info.value.os || '-' }
])

async function loadInfo() {
	try {
		const res = await getAction('/monitor/cache/redis', null, { showLoading: true })
		if (res.code === 200 && res.data) {
			info.value = res.data
			keys.value = res.data.keyDetails || []
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
.info-grid {
	display: flex;
	flex-wrap: wrap;
	margin-top: 20rpx;
}
.info-cell {
	width: 50%;
	display: flex;
	flex-direction: column;
	padding: 12rpx 0;
}
.info-value {
	font-size: 26rpx;
	color: #333;
	font-weight: 600;
}
.info-label {
	font-size: 20rpx;
	color: #999;
	margin-top: 4rpx;
}
.key-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 16rpx 0;
	border-bottom: 1rpx solid #f7f7f7;
}
.key-row:last-child { border-bottom: none; }
.key-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.key-name {
	font-size: 24rpx;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.key-sub { font-size: 20rpx; color: #999; margin-top: 4rpx; }
.key-ttl { font-size: 20rpx; color: #1890ff; margin-left: 16rpx; flex-shrink: 0; }
</style>
