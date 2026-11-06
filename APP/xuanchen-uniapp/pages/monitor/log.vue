<template>
	<view class="page">
		<view class="filter-bar">
			<input class="filter-input" v-model="query.userName" placeholder="操作人" confirm-type="search" @confirm="reload" />
			<picker class="filter-picker" :range="statusOptions" range-key="label" @change="onStatusChange">
				<view class="filter-picker-text">{{ statusLabel }}</view>
			</picker>
			<button class="filter-btn" @click="reload">查询</button>
		</view>

		<view class="card" v-for="item in list" :key="item.id">
			<view class="card-top">
				<text class="card-title">{{ item.module || (logType === 'login' ? '登录记录' : '其他') }}</text>
				<text :class="['status-tag', item.status === 1 ? 'on' : 'off']">{{ item.status === 1 ? '成功' : '失败' }}</text>
			</view>
			<view class="card-row" v-if="item.description">
				<text class="value desc">{{ item.description }}</text>
			</view>
			<view class="card-row">
				<text class="label">操作人</text>
				<text class="value">{{ item.userName || '-' }}</text>
			</view>
			<view class="card-row" v-if="logType === 'operation'">
				<text class="label">操作类型</text>
				<text class="value">{{ item.operationType || '-' }}</text>
			</view>
			<view class="card-row">
				<text class="label">IP 地址</text>
				<text class="value">{{ item.ipAddress || '-' }}</text>
			</view>
			<view class="card-row">
				<text class="label">时间</text>
				<text class="value">{{ timeText(item) || '-' }}</text>
			</view>
		</view>

		<empty-state v-if="list.length === 0 && !loading" text="暂无日志" icon="📭" />
		<view class="load-more" v-if="list.length > 0" @click="loadMore">
			<text class="load-more-text">{{ hasMore ? '加载更多' : '没有更多了' }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'

const logType = ref('operation')

onLoad((query) => {
	if (query && query.type === 'login') {
		logType.value = 'login'
		uni.setNavigationBarTitle({ title: '登录日志' })
	}
})

function timeText(item) {
	return logType.value === 'login' ? item.loginTime : item.operationTime
}

const list = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loading = ref(false)
const hasMore = computed(() => list.value.length < total.value)

const statusOptions = [
	{ label: '全部状态', value: '' },
	{ label: '成功', value: 1 },
	{ label: '失败', value: 0 }
]
const query = ref({ userName: '', status: '' })

const statusLabel = computed(() => {
	const found = statusOptions.find(o => o.value === query.value.status)
	return found ? found.label : '全部状态'
})

function onStatusChange(e) {
	query.value.status = statusOptions[e.detail.value].value
	reload()
}

async function loadList() {
	if (loading.value) return
	loading.value = true
	try {
		const params = {
			pageNo: pageNo.value,
			pageSize
		}
		if (query.value.userName) params.userName = query.value.userName
		if (query.value.status !== '') params.status = query.value.status
		const url = logType.value === 'login' ? '/monitor/log/login/list' : '/monitor/log/operation/list'
		const res = await getAction(url, params, { showLoading: true })
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

function reload() {
	pageNo.value = 1
	loadList()
}

function loadMore() {
	if (list.value.length >= total.value) return
	pageNo.value++
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
.filter-bar {
	display: flex;
	margin-bottom: 20rpx;
}
.filter-input {
	flex: 1;
	background: #fff;
	border-radius: 12rpx;
	padding: 16rpx 24rpx;
	font-size: 26rpx;
	margin-right: 16rpx;
}
.filter-picker {
	margin-right: 16rpx;
}
.filter-picker-text {
	background: #fff;
	border-radius: 12rpx;
	padding: 16rpx 24rpx;
	font-size: 26rpx;
	color: #333;
	line-height: 40rpx;
}
.filter-btn {
	background: #1890ff;
	color: #fff;
	font-size: 26rpx;
	line-height: 72rpx;
	padding: 0 30rpx;
	border-radius: 12rpx;
	margin: 0;
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
	margin-bottom: 12rpx;
}
.card-title { font-size: 30rpx; color: #333; font-weight: 600; }
.status-tag {
	font-size: 20rpx;
	padding: 4rpx 16rpx;
	border-radius: 8rpx;
}
.status-tag.on { background: #f6ffed; color: #52c41a; }
.status-tag.off { background: #fff1f0; color: #f5222d; }
.card-row {
	display: flex;
	margin-bottom: 8rpx;
}
.label { font-size: 24rpx; color: #999; width: 140rpx; flex-shrink: 0; }
.value { font-size: 24rpx; color: #555; flex: 1; word-break: break-all; }
.value.desc { color: #333; margin-bottom: 8rpx; }
.load-more {
	display: flex;
	justify-content: center;
	padding: 24rpx;
}
.load-more-text { font-size: 24rpx; color: #999; }
</style>
