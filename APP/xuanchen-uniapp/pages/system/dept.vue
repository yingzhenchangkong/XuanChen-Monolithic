<template>
	<view class="page">
		<view class="tree">
			<view class="node-row" v-for="node in flatNodes" :key="node.deptCode" :style="{ paddingLeft: 24 + node._level * 40 + 'rpx' }">
				<view class="node-icon">
					<text class="node-icon-text">🏢</text>
				</view>
				<view class="node-main">
					<text class="node-title">{{ node.deptName }}</text>
					<text class="node-sub">{{ node.deptCode }}<template v-if="node.leaderUser"> · 负责人：{{ node.leaderUser }}</template></text>
				</view>
				<text :class="['status-tag', node.status === 1 ? 'on' : 'off']">{{ node.status === 1 ? '启用' : '停用' }}</text>
			</view>
		</view>
		<empty-state v-if="flatNodes.length === 0 && !loading" text="暂无部门" icon="📭" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import config from '@/utils/config'
import { getToken } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const treeData = ref([])
const loading = ref(false)

// 部门树接口返回裸 JSON（无 Result 包装），直接拿数组
const flatNodes = computed(() => {
	const result = []
	const walk = (nodes, level) => {
		(nodes || []).forEach(n => {
			result.push({ ...n, _level: level })
			if (n.children && n.children.length > 0 && n._expanded !== false) {
				walk(n.children, level + 1)
			}
		})
	}
	walk(treeData.value, 0)
	return result
})

async function loadTree() {
	loading.value = true
	try {
		// 该接口返回裸 JSON 数组（无 Result 包装），需绕过通用 http 封装
		const res = await new Promise((resolve, reject) => {
			uni.request({
				url: config.baseUrl + '/system/dept/getDeptTree',
				method: 'GET',
				header: { [config.tokenHeader]: getToken() },
				success: (r) => r.statusCode === 200 ? resolve(r.data) : reject(r),
				fail: reject
			})
		})
		treeData.value = Array.isArray(res) ? res : (Array.isArray(res?.data) ? res.data : [])
	} catch {} finally {
		loading.value = false
	}
}

onMounted(() => {
	loadTree()
})
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f6fa;
	padding: 20rpx 24rpx 60rpx;
}
.tree {
	background: #fff;
	border-radius: 16rpx;
	padding: 12rpx 0;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.node-row {
	display: flex;
	align-items: center;
	padding: 20rpx 24rpx;
	border-bottom: 1rpx solid #f7f7f7;
}
.node-row:last-child { border-bottom: none; }
.node-icon {
	width: 52rpx;
	height: 52rpx;
	border-radius: 12rpx;
	background: #e6f7ff;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 14rpx;
	flex-shrink: 0;
}
.node-icon-text { font-size: 26rpx; }
.node-main {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.node-title {
	font-size: 27rpx;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.node-sub {
	font-size: 20rpx;
	color: #999;
	margin-top: 4rpx;
}
.status-tag {
	font-size: 20rpx;
	padding: 2rpx 12rpx;
	border-radius: 8rpx;
	flex-shrink: 0;
}
.status-tag.on { background: #f6ffed; color: #52c41a; }
.status-tag.off { background: #fff1f0; color: #f5222d; }
</style>
