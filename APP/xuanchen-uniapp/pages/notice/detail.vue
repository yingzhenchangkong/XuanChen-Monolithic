<template>
	<view class="detail-page">
		<view class="detail-header" v-if="notice">
			<text class="detail-title">{{ notice.title }}</text>
			<view class="detail-meta">
				<view class="meta-tag">
					<text class="tag-text">{{ notice.noticeType === '1' ? '通知' : '公告' }}</text>
				</view>
				<text class="meta-time">{{ notice.createTime }}</text>
			</view>
		</view>

		<view class="detail-content" v-if="notice">
			<rich-text :nodes="notice.content || '暂无内容'" />
		</view>

		<view class="detail-actions" v-if="notice && notice.readStatus !== 1">
			<button class="action-btn" @click="handleMarkRead">标记为已读</button>
		</view>

		<view class="loading-area" v-if="loading">
			<text class="loading-text">加载中...</text>
		</view>

		<empty-state v-if="!loading && !notice" text="通知不存在或已删除" icon="📭" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAction } from '@/utils/http'
import { useNoticeStore } from '@/stores/notice'
import EmptyState from '@/components/empty-state.vue'

const noticeStore = useNoticeStore()

const notice = ref(null)
const loading = ref(true)

onMounted(async () => {
	const pages = getCurrentPages()
	const page = pages[pages.length - 1]
	const id = page.options?.id
	if (!id) {
		loading.value = false
		return
	}
	try {
		const res = await getAction('/system/notice/listUser', { id, current: 1, size: 1 })
		if (res.code === 200 && res.data?.records?.length > 0) {
			notice.value = res.data.records[0]
		} else {
			const cached = noticeStore.noticeList.find(n => n.id === id || n.id == id)
			if (cached) notice.value = cached
		}
	} catch {} finally {
		loading.value = false
	}
})

async function handleMarkRead() {
	if (!notice.value) return
	await noticeStore.markAsRead(notice.value.id)
	notice.value.readStatus = 1
	uni.showToast({ title: '已标记为已读', icon: 'success' })
}
</script>

<style scoped>
.detail-page {
	min-height: 100vh;
	background: #fff;
	padding: 0 32rpx;
}
.detail-header {
	padding: 32rpx 0 20rpx;
	border-bottom: 1rpx solid #f0f0f0;
}
.detail-title {
	font-size: 36rpx;
	color: #333;
	font-weight: bold;
	line-height: 1.5;
}
.detail-meta {
	display: flex;
	align-items: center;
	margin-top: 16rpx;
}
.meta-tag {
	background: #e6f7ff;
	padding: 4rpx 16rpx;
	border-radius: 6rpx;
	margin-right: 16rpx;
}
.tag-text { font-size: 22rpx; color: #1890ff; }
.meta-time { font-size: 24rpx; color: #999; }

.detail-content {
	padding: 32rpx 0;
	font-size: 28rpx;
	color: #333;
	line-height: 1.8;
}

.detail-actions {
	padding: 20rpx 0 40rpx;
}
.action-btn {
	width: 100%;
	height: 80rpx;
	background: #1890ff;
	color: #fff;
	font-size: 28rpx;
	border-radius: 40rpx;
	border: none;
	display: flex;
	align-items: center;
	justify-content: center;
}

.loading-area {
	padding: 100rpx 0;
	text-align: center;
}
.loading-text { font-size: 28rpx; color: #999; }
</style>
