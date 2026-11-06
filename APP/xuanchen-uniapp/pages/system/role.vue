<template>
	<view class="page">
		<view class="search-bar">
			<input class="search-input" v-model="keyword" placeholder="搜索角色名称" confirm-type="search" @confirm="reload" />
			<button class="search-btn" @click="reload">搜索</button>
		</view>

		<view class="card" v-for="item in list" :key="item.id">
			<view class="card-top">
				<text class="card-title">{{ item.roleName }}</text>
				<text :class="['status-tag', item.status === 1 ? 'on' : 'off']">{{ item.status === 1 ? '启用' : '停用' }}</text>
			</view>
			<view class="card-row">
				<text class="label">角色编码</text>
				<text class="value">{{ item.roleCode }}</text>
			</view>
			<view class="card-row" v-if="item.roleDescription">
				<text class="label">描述</text>
				<text class="value">{{ item.roleDescription }}</text>
			</view>
			<view class="card-actions">
				<button class="act-btn" @click="openEdit(item)">编辑</button>
				<button class="act-btn" @click="toggleStatus(item)">{{ item.status === 1 ? '停用' : '启用' }}</button>
				<button class="act-btn danger" @click="handleDelete(item)">删除</button>
			</view>
		</view>

		<empty-state v-if="list.length === 0 && !loading" text="暂无数据" icon="📭" />
		<view class="load-more" v-if="list.length > 0" @click="loadMore">
			<text class="load-more-text">{{ hasMore ? '加载更多' : '没有更多了' }}</text>
		</view>

		<view class="fab" @click="openAdd">
			<text class="fab-icon">+</text>
		</view>

		<form-modal
			:visible="modalVisible"
			:title="form.id ? '编辑角色' : '新增角色'"
			:fields="fields"
			:form="form"
			@close="modalVisible = false"
			@submit="handleSubmit"
		/>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onMounted } from 'vue'
import { getAction, postAction, putAction, deleteAction } from '@/utils/http'
import EmptyState from '@/components/empty-state.vue'
import FormModal from '@/components/form-modal.vue'

const list = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const keyword = ref('')
const loading = ref(false)
const modalVisible = ref(false)

const fields = [
	{ key: 'roleName', label: '角色名称' },
	{ key: 'roleCode', label: '角色编码' },
	{ key: 'roleDescription', label: '描述', type: 'textarea' },
	{ key: 'orderNo', label: '排序', type: 'number' },
	{ key: 'status', label: '状态', type: 'switch' }
]

const form = ref({})

const hasMore = computed(() => list.value.length < total.value)

async function loadList() {
	if (loading.value) return
	loading.value = true
	try {
		const res = await getAction('/system/role/list', {
			roleName: keyword.value,
			pageNo: pageNo.value,
			pageSize
		}, { showError: true, showLoading: true })
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

function openAdd() {
	form.value = { roleName: '', roleCode: '', roleDescription: '', orderNo: 0, status: 1 }
	modalVisible.value = true
}

function openEdit(item) {
	form.value = { ...item }
	modalVisible.value = true
}

async function handleSubmit(data) {
	try {
		if (data.id) {
			await putAction('/system/role/edit', data, { showLoading: true })
		} else {
			await postAction('/system/role/add', data, { showLoading: true })
		}
		uni.showToast({ title: '保存成功', icon: 'success' })
		modalVisible.value = false
		reload()
	} catch {}
}

async function toggleStatus(item) {
	try {
		await putAction('/system/role/changeStatus', {
			id: item.id,
			status: item.status === 1 ? 0 : 1
		}, { showLoading: true })
		item.status = item.status === 1 ? 0 : 1
		uni.showToast({ title: '操作成功', icon: 'success' })
	} catch {}
}

function handleDelete(item) {
	uni.showModal({
		title: '确认删除',
		content: `确定删除角色"${item.roleName}"吗？`,
		success: async (res) => {
			if (!res.confirm) return
			try {
				await deleteAction(`/system/role/delete?id=${item.id}`, null, { showLoading: true })
				uni.showToast({ title: '删除成功', icon: 'success' })
				reload()
			} catch {}
		}
	})
}

onMounted(() => {
	loadList()
})
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f6fa;
	padding: 20rpx 24rpx 160rpx;
}
.search-bar {
	display: flex;
	margin-bottom: 20rpx;
}
.search-input {
	flex: 1;
	background: #fff;
	border-radius: 12rpx;
	padding: 16rpx 24rpx;
	font-size: 26rpx;
	margin-right: 16rpx;
}
.search-btn {
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
	margin-bottom: 16rpx;
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
.value { font-size: 24rpx; color: #555; flex: 1; }
.card-actions {
	display: flex;
	justify-content: flex-end;
	gap: 16rpx;
	margin-top: 16rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid #f5f5f5;
}
.act-btn {
	font-size: 24rpx;
	line-height: 56rpx;
	padding: 0 28rpx;
	border-radius: 10rpx;
	margin: 0;
	background: #f0f7ff;
	color: #1890ff;
}
.act-btn.danger {
	background: #fff1f0;
	color: #f5222d;
}
.load-more {
	display: flex;
	justify-content: center;
	padding: 24rpx;
}
.load-more-text { font-size: 24rpx; color: #999; }
.fab {
	position: fixed;
	right: 40rpx;
	bottom: 80rpx;
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
	background: #1890ff;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 8rpx 24rpx rgba(24,144,255,0.4);
}
.fab-icon { font-size: 56rpx; color: #fff; }
</style>
