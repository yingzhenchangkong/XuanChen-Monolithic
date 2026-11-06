<template>
	<view class="page">
		<view class="card" v-for="item in list" :key="item.token">
			<view class="card-top">
				<view class="user-wrap">
					<text class="avatar">{{ (item.nickName || item.userName || '?').slice(0, 1) }}</text>
					<view class="user-info">
						<text class="user-name">{{ item.nickName || item.userName }}</text>
						<text class="user-sub">{{ item.userName }}</text>
					</view>
				</view>
				<button class="logout-btn" @click="forceLogout(item)">强制下线</button>
			</view>
			<view class="card-row">
				<text class="label">IP 地址</text>
				<text class="value">{{ item.ip || '-' }}</text>
			</view>
			<view class="card-row">
				<text class="label">设备类型</text>
				<text class="value">{{ item.deviceType || '未知' }}</text>
			</view>
		</view>

		<empty-state v-if="list.length === 0 && !loading" text="暂无在线用户" icon="📭" />
		<view class="load-more" v-if="list.length > 0" @click="loadMore">
			<text class="load-more-text">{{ hasMore ? '加载更多' : '没有更多了' }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAction, postAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const list = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loading = ref(false)
const hasMore = computed(() => list.value.length < total.value)

async function loadList() {
	if (loading.value) return
	loading.value = true
	try {
		const res = await getAction('/monitor/onlineUser/list', {
			pageNo: pageNo.value,
			pageSize
		}, { showLoading: true })
		if (res.code === 200 && res.data) {
			if (pageNo.value === 1) {
				list.value = res.data.records || []
			} else {
				list.value = list.value.concat(res.data.records || [])
			}
			total.value = res.data.total || 0
		}
	} catch {} finally {
		loading.value = false
	}
}

function loadMore() {
	if (list.value.length >= total.value) return
	pageNo.value++
	loadList()
}

function forceLogout(item) {
	uni.showModal({
		title: '确认下线',
		content: `确定将用户"${item.nickName || item.userName}"强制下线吗？`,
		success: async (res) => {
			if (!res.confirm) return
			try {
				await postAction('/monitor/onlineUser/forceLogout', { token: item.token }, { showLoading: true })
				uni.showToast({ title: '已强制下线', icon: 'success' })
				reload()
			} catch {}
		}
	})
}

function reload() {
	pageNo.value = 1
	loadList()
}

onMounted(() => {
	loadList()
})
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f6fa;
	padding: 20rpx 24rpx 60rpx;
}
.card {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
	box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.card-top {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16rpx;
}
.user-wrap { display: flex; align-items: center; }
.avatar {
	width: 72rpx;
	height: 72rpx;
	border-radius: 50%;
	background: #1890ff;
	color: #fff;
	font-size: 30rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 16rpx;
}
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 28rpx; color: #333; font-weight: 600; }
.user-sub { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.logout-btn {
	font-size: 22rpx;
	line-height: 56rpx;
	padding: 0 24rpx;
	border-radius: 10rpx;
	margin: 0;
	background: #fff1f0;
	color: #f5222d;
}
.card-row {
	display: flex;
	margin-bottom: 8rpx;
}
.label { font-size: 24rpx; color: #999; width: 140rpx; flex-shrink: 0; }
.value { font-size: 24rpx; color: #555; flex: 1; }
.load-more {
	display: flex;
	justify-content: center;
	padding: 24rpx;
}
.load-more-text { font-size: 24rpx; color: #999; }
</style>
