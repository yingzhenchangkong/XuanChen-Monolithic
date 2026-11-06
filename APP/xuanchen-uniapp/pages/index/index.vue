<template>
	<view class="home-page">
		<view class="home-header">
			<view class="header-left">
				<text class="greeting">{{ greeting }}</text>
				<text class="user-name">{{ authStore.nickName || authStore.userName }}</text>
			</view>
			<view class="header-right">
				<view class="notice-badge" @click="switchToNotices">
					<text class="badge-icon">🔔</text>
					<view class="badge-count" v-if="noticeStore.unreadCount > 0">
						<text class="badge-text">{{ noticeStore.unreadCount > 99 ? '99+' : noticeStore.unreadCount }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="quick-actions">
			<view class="section-header">
				<text class="section-title">常用应用</text>
				<text class="section-more" @click="goApps">全部 ></text>
			</view>
			<view class="qa-grid">
				<view
					class="action-item"
					v-for="menu in menuList.slice(0, 8)"
					:key="menu.id"
					@click="handleMenuClick(menu)"
				>
					<view class="action-icon" :style="{ background: menu.bgColor }">
						<text class="icon-text">{{ menu.icon }}</text>
					</view>
					<text class="action-label">{{ menu.title }}</text>
				</view>
			</view>
		</view>

		<view class="section">
			<view class="section-header">
				<text class="section-title">待办事项</text>
			</view>
			<view class="todo-list" v-if="todos.length > 0">
				<view class="todo-item" v-for="t in todos" :key="t.title">
					<view :class="['todo-dot', t.level]" />
					<text class="todo-title">{{ t.title }}</text>
					<text class="todo-count">{{ t.count }}</text>
				</view>
			</view>
			<empty-state v-else text="暂无待办" icon="✅" />
		</view>

		<view class="section">
			<view class="section-header">
				<text class="section-title">最新通知</text>
				<text class="section-more" @click="switchToNotices">更多 ></text>
			</view>
			<view class="notice-list" v-if="recentNotices.length > 0">
				<view
					class="notice-item"
					v-for="item in recentNotices"
					:key="item.id"
					@click="goNoticeDetail(item)"
				>
					<view :class="['notice-dot', { unread: item.readStatus !== 1 }]" />
					<view class="notice-content">
						<text class="notice-title">{{ item.title }}</text>
						<text class="notice-time">{{ formatTime(item.createTime) }}</text>
					</view>
				</view>
			</view>
			<empty-state v-else text="暂无通知" icon="📭" />
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNoticeStore } from '@/stores/notice'
import { getAction } from '@/utils/http'
import { isWsConnected } from '@/utils/ws'
import { resolveIcon, resolveColor, navigateToMenu } from '@/utils/menu'
import EmptyState from '@/components/empty-state.vue'

const authStore = useAuthStore()
const noticeStore = useNoticeStore()
const wsConnected = ref(false)
const menuList = ref([])

const greeting = computed(() => {
	const hour = new Date().getHours()
	if (hour < 6) return '夜深了'
	if (hour < 9) return '早上好'
	if (hour < 12) return '上午好'
	if (hour < 14) return '中午好'
	if (hour < 18) return '下午好'
	return '晚上好'
})

const recentNotices = computed(() => {
	return noticeStore.noticeList.slice(0, 3)
})

const todos = ref([
	{ title: '待审批流程', count: '6 项', level: 'high' },
	{ title: '异常日志', count: '3 条', level: 'high' },
	{ title: '账号审核', count: '2 个', level: 'mid' }
])

onMounted(async () => {
	wsConnected.value = isWsConnected()
	await noticeStore.fetchUnreadCount()
	await noticeStore.fetchNoticeList({ current: 1, size: 5 })
	await loadMenu()
})

async function loadMenu() {
	try {
		const res = await getAction('/system/menu/authList', null, { showError: false, showLoading: false })
		if (res.code === 200 && res.data) {
			const menus = res.data.filter(m => m.menuType === 'M' && m.status === 1)
			menuList.value = menus.map((m, i) => ({
				id: m.id,
				title: m.title,
				icon: resolveIcon(m.meta?.icon, i),
				bgColor: resolveColor(i),
				children: m.children || []
			}))
		}
	} catch {}
}

function handleMenuClick(menu) {
	if (menu.children && menu.children.length > 0) {
		const subMenus = menu.children.filter(m => m.menuType === 'C' && m.status === 1)
		if (subMenus.length === 1) {
			navigateToMenu(subMenus[0])
		} else {
			uni.showActionSheet({
				itemList: subMenus.map(m => m.title),
				success: (res) => {
					navigateToMenu(subMenus[res.tapIndex])
				}
			})
		}
	} else {
		navigateToMenu(menu)
	}
}

function switchToNotices() {
	uni.navigateTo({ url: '/pages/notice/index' })
}

function goApps() {
	uni.switchTab({ url: '/pages/app/index' })
}

function goNoticeDetail(item) {
	uni.navigateTo({ url: `/pages/notice/detail?id=${item.id}` })
}

async function handleRefresh() {
	uni.showLoading({ title: '刷新中...' })
	await noticeStore.fetchUnreadCount()
	await noticeStore.fetchNoticeList({ current: 1, size: 5 })
	wsConnected.value = isWsConnected()
	uni.hideLoading()
	uni.showToast({ title: '已刷新', icon: 'success' })
}

function formatTime(time) {
	if (!time) return ''
	const date = new Date(time)
	const now = new Date()
	const diff = now - date
	if (diff < 60000) return '刚刚'
	if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
	if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
	if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
	const m = date.getMonth() + 1
	const d = date.getDate()
	return `${m}月${d}日`
}
</script>

<style scoped>
.home-page {
	min-height: 100vh;
	background: #f5f6fa;
	padding-bottom: 30rpx;
}
.home-header {
	background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
	padding: 60rpx 36rpx 40rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
}
.header-left { display: flex; flex-direction: column; }
.greeting { font-size: 26rpx; color: rgba(255,255,255,0.8); }
.user-name { font-size: 36rpx; color: #fff; font-weight: bold; margin-top: 8rpx; }
.notice-badge { position: relative; padding: 10rpx; }
.badge-icon { font-size: 40rpx; }
.badge-count {
	position: absolute;
	top: 2rpx; right: -4rpx;
	min-width: 32rpx; height: 32rpx;
	background: #ff4d4f;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 8rpx;
}
.badge-text { font-size: 18rpx; color: #fff; }

.quick-actions {
	margin: -20rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx 10rpx 16rpx;
	box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
	position: relative;
	z-index: 2;
}
.quick-actions .section-header {
	padding: 0 14rpx;
	margin-bottom: 0;
}
.qa-grid {
	display: flex;
	flex-wrap: wrap;
	margin-top: 24rpx;
}
.action-item {
	width: 25%;
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 24rpx;
}
.action-icon {
	width: 84rpx; height: 84rpx;
	border-radius: 20rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10rpx;
}
.icon-text { font-size: 38rpx; }
.action-label {
	font-size: 22rpx;
	color: #666;
	max-width: 150rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.todo-list { display: flex; flex-direction: column; }
.todo-item {
	display: flex;
	align-items: center;
	padding: 14rpx 0;
	border-bottom: 1rpx solid #f5f5f5;
}
.todo-item:last-child { border-bottom: none; }
.todo-dot {
	width: 12rpx; height: 12rpx;
	border-radius: 50%;
	margin-right: 16rpx;
	flex-shrink: 0;
}
.todo-dot.high { background: #f5222d; }
.todo-dot.mid { background: #fa8c16; }
.todo-dot.low { background: #52c41a; }
.todo-title { flex: 1; font-size: 26rpx; color: #333; }
.todo-count { font-size: 24rpx; color: #999; }

.section {
	margin: 24rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 28rpx 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.section-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20rpx;
}
.section-title { font-size: 30rpx; color: #333; font-weight: 600; }
.section-more { font-size: 24rpx; color: #1890ff; }

.notice-list { display: flex; flex-direction: column; }
.notice-item {
	display: flex;
	align-items: center;
	padding: 14rpx 0;
	border-bottom: 1rpx solid #f5f5f5;
}
.notice-item:last-child { border-bottom: none; }
.notice-dot {
	width: 10rpx; height: 10rpx;
	border-radius: 50%;
	background: #d9d9d9;
	margin-right: 14rpx;
	flex-shrink: 0;
}
.notice-dot.unread { background: #1890ff; }
.notice-content { flex: 1; display: flex; flex-direction: column; }
.notice-title {
	font-size: 25rpx;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.notice-time { font-size: 20rpx; color: #999; margin-top: 4rpx; }
</style>
