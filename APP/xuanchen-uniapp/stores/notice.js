import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAction, postAction } from '@/utils/http'
import { addMessageListener, removeMessageListener } from '@/utils/ws'

export const useNoticeStore = defineStore('notice', () => {
	const noticeList = ref([])
	const unreadCount = ref(0)
	const loading = ref(false)
	const loaded = ref(false)

	function onWsMessage(msg) {
		if (msg && msg.type === 'notice') {
			fetchUnreadCount()
		}
	}

	function startListening() {
		addMessageListener(onWsMessage)
	}

	function stopListening() {
		removeMessageListener(onWsMessage)
	}

	async function fetchUnreadCount() {
		try {
			const res = await getAction('/system/notice/getNoticeCount', null, { showError: false })
			if (res.code === 200) {
				unreadCount.value = res.data || 0
			}
		} catch {}
	}

	async function fetchNoticeList(params = {}) {
		loading.value = true
		try {
			const res = await getAction('/system/notice/listUser', params)
			if (res.code === 200 && res.data) {
				const pageData = res.data
				if (params.current === 1) {
					noticeList.value = pageData.records || []
				} else {
					noticeList.value = [...noticeList.value, ...(pageData.records || [])]
				}
				loaded.value = true
				return pageData
			}
		} catch {} finally {
			loading.value = false
		}
	}

	async function markAsRead(id) {
		try {
			const res = await postAction('/system/notice/setRead', { id })
			if (res.code === 200) {
				const item = noticeList.value.find(n => n.id === id)
				if (item) item.readStatus = 1
				if (unreadCount.value > 0) unreadCount.value--
			}
		} catch {}
	}

	async function markAllAsRead() {
		try {
			const res = await postAction('/system/notice/setReadAll', {})
			if (res.code === 200) {
				noticeList.value.forEach(n => n.readStatus = 1)
				unreadCount.value = 0
			}
		} catch {}
	}

	function reset() {
		noticeList.value = []
		unreadCount.value = 0
		loaded.value = false
	}

	return {
		noticeList, unreadCount, loading, loaded,
		fetchUnreadCount, fetchNoticeList, markAsRead, markAllAsRead,
		startListening, stopListening, reset
	}
})
