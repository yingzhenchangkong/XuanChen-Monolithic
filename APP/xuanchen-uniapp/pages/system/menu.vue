<template>
	<view class="page">
		<view class="tree">
			<view class="node" v-for="node in treeData" :key="node.id">
				<view class="node-row" :style="{ paddingLeft: 24 + node._level * 40 + 'rpx' }" @click="toggle(node)">
					<view class="node-icon" :class="['type-' + node.menuType]">
						<text class="node-icon-text">{{ typeIcon(node.menuType) }}</text>
					</view>
					<text class="node-title">{{ node.title }}</text>
					<text :class="['status-tag', node.status === 1 ? 'on' : 'off']">{{ node.status === 1 ? '启用' : '停用' }}</text>
					<text class="arrow" v-if="node.children && node.children.length > 0">{{ node._expanded ? '▾' : '▸' }}</text>
				</view>
				<template v-if="node._expanded && node.children && node.children.length > 0">
					<view class="node-row child" v-for="child in node.children" :key="child.id" :style="{ paddingLeft: 24 + (node._level + 1) * 40 + 'rpx' }">
						<view class="node-icon" :class="['type-' + child.menuType]">
							<text class="node-icon-text">{{ typeIcon(child.menuType) }}</text>
						</view>
						<text class="node-title">{{ child.title }}</text>
						<text :class="['status-tag', child.status === 1 ? 'on' : 'off']">{{ child.status === 1 ? '启用' : '停用' }}</text>
					</view>
				</template>
			</view>
		</view>
		<empty-state v-if="treeData.length === 0 && !loading" text="暂无菜单" icon="📭" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const treeData = ref([])
const loading = ref(false)

function typeIcon(menuType) {
	if (menuType === 'M') return '📁'
	if (menuType === 'C') return '📄'
	return '🔘'
}

function toggle(node) {
	node._expanded = !node._expanded
}

async function loadTree() {
	loading.value = true
	try {
		const res = await getAction('/system/menu/list', null, { showLoading: true })
		if (res.code === 200 && res.data) {
			const records = res.data.records || res.data || []
			treeData.value = records.map(m => ({ ...m, _level: 0, _expanded: true }))
		}
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
.node-row.child {
	border-bottom: none;
}
.node-icon {
	width: 52rpx;
	height: 52rpx;
	border-radius: 12rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 14rpx;
	flex-shrink: 0;
}
.node-icon.type-M { background: #e6f7ff; }
.node-icon.type-C { background: #f6ffed; }
.node-icon.type-F { background: #fff7e6; }
.node-icon-text { font-size: 26rpx; }
.node-title {
	flex: 1;
	font-size: 27rpx;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.status-tag {
	font-size: 20rpx;
	padding: 2rpx 12rpx;
	border-radius: 8rpx;
	flex-shrink: 0;
}
.status-tag.on { background: #f6ffed; color: #52c41a; }
.status-tag.off { background: #fff1f0; color: #f5222d; }
.arrow {
	font-size: 24rpx;
	color: #999;
	margin-left: 12rpx;
	flex-shrink: 0;
}
</style>
