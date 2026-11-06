<template>
	<view class="app-page">
		<view class="page-header">
			<text class="page-title">应用</text>
		</view>

		<view v-if="menuGroups.length === 0" class="loading-wrap">
			<empty-state text="暂无可用应用" icon="📱" />
		</view>

		<view class="group" v-for="group in menuGroups" :key="group.id">
			<view class="group-header">
				<view class="group-icon" :style="{ background: group.bgColor }">
					<text class="group-icon-text">{{ group.icon }}</text>
				</view>
				<text class="group-title">{{ group.title }}</text>
			</view>
			<view class="item-grid">
				<view
					class="item"
					v-for="(item, idx) in group.children"
					:key="item.id"
					@click="handleItemClick(item)"
				>
					<view class="item-icon" :style="{ background: item.bgColor }">
						<text class="item-icon-text">{{ item.icon }}</text>
					</view>
					<text class="item-label">{{ item.title }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import { resolveIcon, resolveColor, navigateToMenu } from '@/utils/menu'
import EmptyState from '@/components/empty-state.vue'

const rawMenus = ref([])

const menuGroups = computed(() => {
	return rawMenus.value.map((m, i) => ({
		id: m.id,
		title: m.title,
		icon: resolveIcon(m.meta?.icon, i),
		bgColor: resolveColor(i),
		children: (m.children || [])
			.filter(c => c.menuType === 'C' && c.status === 1)
			.map((c, j) => ({
				id: c.id,
				title: c.title,
				path: c.path || c.component,
				icon: resolveIcon(c.meta?.icon, j + i),
				bgColor: resolveColor(i + j + 1)
			}))
	})).filter(g => g.children.length > 0 || g.title)
})

onMounted(async () => {
	await loadMenu()
})

async function loadMenu() {
	try {
		const res = await getAction('/system/menu/authList', null, { showError: false, showLoading: false })
		if (res.code === 200 && res.data) {
			rawMenus.value = res.data.filter(m => m.menuType === 'M' && m.status === 1)
		}
	} catch {}
}

function handleItemClick(item) {
	navigateToMenu(item)
}
</script>

<style scoped>
.app-page {
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
.loading-wrap {
	margin-top: 200rpx;
}
.group {
	margin: 24rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 28rpx 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.group-header {
	display: flex;
	align-items: center;
	margin-bottom: 24rpx;
}
.group-icon {
	width: 56rpx;
	height: 56rpx;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 16rpx;
}
.group-icon-text { font-size: 30rpx; }
.group-title {
	font-size: 30rpx;
	color: #333;
	font-weight: 600;
}
.item-grid {
	display: flex;
	flex-wrap: wrap;
}
.item {
	width: 25%;
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 28rpx;
}
.item-icon {
	width: 84rpx;
	height: 84rpx;
	border-radius: 20rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10rpx;
}
.item-icon-text { font-size: 38rpx; }
.item-label {
	font-size: 22rpx;
	color: #666;
	text-align: center;
	max-width: 150rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
</style>
