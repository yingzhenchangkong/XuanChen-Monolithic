import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { postAction, getAction, setToken, getToken, setUserInfo, getUserInfo, clearAuth, navigateToLogin } from '@/utils/http'
import { connectWebSocket, closeWebSocket } from '@/utils/ws'

export const useAuthStore = defineStore('auth', () => {
	const token = ref(getToken())
	const userInfo = ref(getUserInfo())
	const menuList = ref([])

	const isLoggedIn = computed(() => !!token.value)
	const userName = computed(() => userInfo.value?.userName || '')
	const nickName = computed(() => userInfo.value?.nickName || '')
	const avatar = computed(() => userInfo.value?.avatar || '')

	async function login(loginData) {
		const res = await postAction('/login', loginData, { showLoading: true })
		if (res.code === 200 && res.data) {
			const data = res.data
			setToken(data.token)
			token.value = data.token
			const info = {
				userName: data.userName,
				nickName: data.nickName,
				avatar: data.avatar,
				mobile: data.mobile,
				email: data.email,
				pwdResetRequired: data.pwdResetRequired
			}
			setUserInfo(info)
			userInfo.value = info
			connectWebSocket()
			return data
		}
		throw res
	}

	async function logout() {
		try {
			await postAction('/logout', {})
		} catch {}
		closeWebSocket()
		clearAuth()
		token.value = ''
		userInfo.value = null
		menuList.value = []
		navigateToLogin()
	}

	async function loadMenuList() {
		try {
			const res = await getAction('/system/menu/authList')
			if (res.code === 200 && res.data) {
				menuList.value = res.data
			}
		} catch {}
	}

	function updateUserInfo(info) {
		userInfo.value = { ...userInfo.value, ...info }
		setUserInfo(userInfo.value)
	}

	function restoreSession() {
		const savedToken = getToken()
		const savedInfo = getUserInfo()
		if (savedToken) {
			token.value = savedToken
			userInfo.value = savedInfo
			connectWebSocket()
		}
	}

	return {
		token, userInfo, menuList,
		isLoggedIn, userName, nickName, avatar,
		login, logout, loadMenuList, updateUserInfo, restoreSession
	}
})
