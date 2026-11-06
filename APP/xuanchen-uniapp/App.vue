<script>
import { useAuthStore } from '@/stores/auth'
import { useNoticeStore } from '@/stores/notice'

export default {
	onLaunch() {
		console.log('App Launch')
		const authStore = useAuthStore()
		const noticeStore = useNoticeStore()

		if (authStore.isLoggedIn) {
			authStore.restoreSession()
			noticeStore.startListening()
			noticeStore.fetchUnreadCount()
		}
	},
	onShow() {
		console.log('App Show')
	},
	onHide() {
		console.log('App Hide')
	}
}
</script>

<style>
page {
	background-color: #f5f6fa;
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

view, text {
	box-sizing: border-box;
}

button {
	margin: 0;
	padding: 0;
}

button::after {
	border: none;
}
</style>
