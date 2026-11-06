<template>
	<view class="notice-page">
		<view class="notice-header">
			<text class="header-title">通知公告</text>
			<text class="header-action" v-if="noticeStore.unreadCount > 0" @click="handleReadAll">
				全部已读
			</text>
		</view>

		<view class="notice-tabs">
			<view :class="['tab-item', { active: currentTab === 'all' }]" @click="switchTab('all')">
				<text class="tab-text">全部</text>
			</view>
			<view :class="['tab-item', { active: currentTab === 'unread' }]" @click="switchTab('unread')">
				<text class="tab-text">未读</text>
				<view class="tab-badge" v-if="noticeStore.unreadCount > 0">
					<text class="tab-badge-text">{{ noticeStore.unreadCount }}</text>
				</view>
			</view>
			<view :class="['tab-item', { active: currentTab === 'read' }]" @click="switchTab('read')">
				<text class="tab-text">已读</text>
			</view>
		</view>

		<scroll-view
			class="notice-scroll"
			scroll-y
			:refresher-enabled="true"
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
			@scrolltolower="loadMore"
		>
			<view class="notice-list" v-if="filteredList.length > 0">
				<view
					class="notice-item"
					v-for="item in filteredList"
					:key="item.id"
					@click="goDetail(item)"
				>
					<view :class="['item-left', { unread: item.readStatus !== 1 }]">
						<view class="dot" />
					</view>
					<view class="item-center">
						<text class="item-title">{{ item.title }}</text>
						<text class="item-summary">{{ item.content || '暂无内容' }}</text>
						<text class="item-time">{{ formatTime(item.createTime) }}</text>
					</view>
					<view class="item-right">
						<text class="item-type">{{ item.noticeType === '1' ? '通知' : '公告' }}</text>
					</view>
				</view>
			</view>
			<view class="loading-more" v-if="noticeStore.loading">
				<text class="loading-text">加载中...</text>
			</view>
			<view class="no-more" v-if="noMore && filteredList.length > 0">
				<text class="no-more-text">— 没有更多了 —</text>
			</view>
			<empty-state v-if="!noticeStore.loading && filteredList.length === 0" text="暂无通知" icon="📭" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNoticeStore } from '@/stores/notice'
import EmptyState from '@/components/empty-state.vue'

const noticeStore = useNoticeStore()

const currentTab = ref('all')
const isRefreshing = ref(false)
const currentPage = ref(1)
const pageSize = 10
const noMore = ref(false)
const allList = ref([])

const filteredList = computed(() => {
	if (currentTab.value === 'unread') return allList.value.filter(n => n.readStatus !== 1)
	if (currentTab.value === 'read') return allList.value.filter(n => n.readStatus === 1)
	return allList.value
})

onMounted(async () => {
	await loadData(1)
})

async function loadData(page) {
	currentPage.value = page
	const res = await noticeStore.fetchNoticeList({ current: page, size: pageSize })
	if (res && res.records) {
		if (page === 1) {
			allList.value = res.records
		} else {
			allList.value = [...allList.value, ...res.records]
		}
		noMore.value = allList.value.length >= (res.total || 0)
	}
}

function switchTab(tab) {
	currentTab.value = tab
}

async function onRefresh() {
	isRefreshing.value = true
	await noticeStore.fetchUnreadCount()
	await loadData(1)
	isRefreshing.value = false
}

function loadMore() {
	if (!noMore.value && !noticeStore.loading) {
		loadData(currentPage.value + 1)
	}
}

async function handleReadAll() {
	uni.showModal({
		title: '提示',
		content: '确定将所有通知标记为已读？',
		success: async (res) => {
			if (res.confirm) {
				await noticeStore.markAllAsRead()
				await loadData(1)
			}
		}
	})
}

function goDetail(item) {
	if (item.readStatus !== 1) {
		noticeStore.markAsRead(item.id)
	}
	uni.navigateTo({ url: `/pages/notice/detail?id=${item.id}` })
}

function formatTime(time) {
	if (!time) return ''
	const date = new Date(time)
	const now = new Date()
	const diff = now - date
	if (diff < 60000) return '刚刚'
	if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
	if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
	const m = date.getMonth() + 1
	const d = date.getDate()
	return `${m}月${d}日`
}
</script>

<style scoped>
.notice-page {
	min-height: 100vh;
	background: #f5f6fa;
	display: flex;
	flex-direction: column;
}
.notice-header {
	background: #fff;
	padding: 30rpx 32rpx 20rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	border-bottom: 1rpx solid #f0f0f0;
}
.header-title { font-size: 34rpx; color: #333; font-weight: bold; }
.header-action { font-size: 26rpx; color: #1890ff; }

.notice-tabs {
	background: #fff;
	display: flex;
	padding: 0 32rpx;
	border-bottom: 1rpx solid #f0f0f0;
}
.tab-item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 20rpx 0;
	position: relative;
}
.tab-item.active .tab-text { color: #1890ff; font-weight: 600; }
.tab-item.active::after {
	content: '';
	position: absolute;
	bottom: 0; left: 30%; right: 30%;
	height: 4rpx;
	background: #1890ff;
	border-radius: 2rpx;
}
.tab-text { font-size: 28rpx; color: #666; }
.tab-badge {
	min-width: 32rpx; height: 32rpx;
	background: #ff4d4f;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-left: 8rpx;
	padding: 0 6rpx;
}
.tab-badge-text { font-size: 18rpx; color: #fff; }

.notice-scroll {
	flex: 1;
	height: 0;
}
.notice-list {
	padding: 16rpx 24rpx;
}
.notice-item {
	background: #fff;
	border-radius: 12rpx;
	padding: 24rpx;
	margin-bottom: 16rpx;
	display: flex;
	align-items: flex-start;
	box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.item-left {
	padding-top: 8rpx;
	margin-right: 16rpx;
}
.dot {
	width: 14rpx; height: 14rpx;
	border-radius: 50%;
	background: #d9d9d9;
}
.dot.unread, .item-left.unread .dot { background: #1890ff; }
.item-center {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.item-title {
	font-size: 28rpx;
	color: #333;
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.item-summary {
	font-size: 24rpx;
	color: #999;
	margin-top: 8rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.item-time {
	font-size: 22rpx;
	color: #bbb;
	margin-top: 8rpx;
}
.item-right {
	margin-left: 16rpx;
	flex-shrink: 0;
}
.item-type {
	font-size: 20rpx;
	color: #1890ff;
	background: #e6f7ff;
	padding: 4rpx 12rpx;
	border-radius: 6rpx;
}

.loading-more, .no-more {
	padding: 30rpx 0;
	text-align: center;
}
.loading-text { font-size: 24rpx; color: #999; }
.no-more-text { font-size: 24rpx; color: #d9d9d9; }
</style>
