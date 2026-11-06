import config from './config'

let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
	refreshSubscribers.forEach(cb => cb(token))
	refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
	refreshSubscribers.push(cb)
}

function getToken() {
	return uni.getStorageSync(config.tokenKey) || ''
}

function setToken(token) {
	uni.setStorageSync(config.tokenKey, token)
}

function getUserInfo() {
	const info = uni.getStorageSync(config.userInfoKey)
	return info ? (typeof info === 'string' ? JSON.parse(info) : info) : null
}

function setUserInfo(info) {
	uni.setStorageSync(config.userInfoKey, info)
}

function clearAuth() {
	uni.removeStorageSync(config.tokenKey)
	uni.removeStorageSync(config.userInfoKey)
}

function navigateToLogin() {
	uni.reLaunch({ url: '/pages/auth/login' })
}

async function refreshToken() {
	try {
		const res = await request({ url: '/auth/refresh', method: 'POST' })
		if (res.code === 200 && res.data?.token) {
			setToken(res.data.token)
			return res.data.token
		}
		return null
	} catch {
		return null
	}
}

function request(options) {
	return new Promise((resolve, reject) => {
		const { url, method = 'GET', data, header = {}, showLoading = false, showError = true } = options

		const token = getToken()
		const headers = {
			'Content-Type': 'application/json',
			...header
		}
		if (token) {
			headers[config.tokenHeader] = token
		}

		if (showLoading) {
			uni.showLoading({ title: '加载中...', mask: true })
		}

		uni.request({
			url: config.baseUrl + url,
			method,
			data,
			header: headers,
			withCredentials: true,
			success: (res) => {
				if (showLoading) uni.hideLoading()

				const statusCode = res.statusCode
				const body = res.data

				if (statusCode === 200 && body) {
					if (body.code === 200) {
						resolve(body)
					} else if (body.code === 4030) {
						clearAuth()
						uni.showToast({ title: '请修改密码', icon: 'none' })
						uni.reLaunch({ url: '/pages/auth/change-password?forced=1' })
						reject(body)
					} else {
						if (showError) {
							uni.showToast({ title: body.msg || '请求失败', icon: 'none', duration: 2000 })
						}
						reject(body)
					}
				} else if (statusCode === 401) {
					if (!isRefreshing) {
						isRefreshing = true
						refreshToken().then(newToken => {
							isRefreshing = false
							if (newToken) {
								setToken(newToken)
								onRefreshed(newToken)
								headers[config.tokenHeader] = newToken
								uni.request({
									url: config.baseUrl + url,
									method, data,
									header: headers,
									withCredentials: true,
									success: (retryRes) => {
										if (retryRes.statusCode === 200 && retryRes.data?.code === 200) {
											resolve(retryRes.data)
										} else {
											reject(retryRes.data || { code: -1, msg: '请求失败' })
										}
									},
									fail: (err) => {
										uni.showToast({ title: '网络错误', icon: 'none' })
										reject(err)
									}
								})
							} else {
								clearAuth()
								navigateToLogin()
								reject({ code: 401, msg: '登录已过期' })
							}
						})
					} else {
						return new Promise((resolve2) => {
							addRefreshSubscriber((newToken) => {
								headers[config.tokenHeader] = newToken
								resolve2(request({ ...options, header: headers }))
							})
						}).then(resolve).catch(reject)
					}
				} else {
					if (showError) {
						uni.showToast({ title: body?.msg || `请求错误(${statusCode})`, icon: 'none' })
					}
					reject(body || { code: statusCode, msg: '请求失败' })
				}
			},
			fail: (err) => {
				if (showLoading) uni.hideLoading()
				uni.showToast({ title: '网络连接失败', icon: 'none' })
				reject(err)
			}
		})
	})
}

export function getAction(url, params, options = {}) {
	return request({ url, method: 'GET', data: params, ...options })
}

export function postAction(url, data, options = {}) {
	return request({ url, method: 'POST', data, ...options })
}

export function putAction(url, data, options = {}) {
	return request({ url, method: 'PUT', data, ...options })
}

export function deleteAction(url, params, options = {}) {
	return request({ url, method: 'DELETE', data: params, ...options })
}

export function uploadAction(url, formData, options = {}) {
	const token = getToken()
	const header = { ...options.header }
	if (token) {
		header[config.tokenHeader] = token
	}
	return new Promise((resolve, reject) => {
		if (options.showLoading) uni.showLoading({ title: '上传中...', mask: true })
		uni.uploadFile({
			url: config.baseUrl + url,
			filePath: formData.filePath,
			name: formData.name || 'file',
			formData: formData.data || {},
			header: {
				...header
			},
			success: (res) => {
				if (options.showLoading) uni.hideLoading()
				try {
					const data = JSON.parse(res.data)
					if (data.code === 200) {
						resolve(data)
					} else {
						uni.showToast({ title: data.msg || '上传失败', icon: 'none' })
						reject(data)
					}
				} catch (e) {
					reject({ code: -1, msg: '解析响应失败' })
				}
			},
			fail: (err) => {
				if (options.showLoading) uni.hideLoading()
				uni.showToast({ title: '上传失败', icon: 'none' })
				reject(err)
			}
		})
	})
}

export { getToken, setToken, getUserInfo, setUserInfo, clearAuth, navigateToLogin }

export default {
	getAction, postAction, putAction, deleteAction, uploadAction,
	getToken, setToken, getUserInfo, setUserInfo, clearAuth, navigateToLogin
}
