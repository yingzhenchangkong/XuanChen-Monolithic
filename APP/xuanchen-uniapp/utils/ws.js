import config from './config'
import { getToken, clearAuth, navigateToLogin } from './http'

let socketTask = null
let heartbeatTimer = null
let reconnectTimer = null
let reconnectAttempts = 0
let isClosed = false

const messageListeners = []

function getWsUrl() {
	const token = getToken()
	let wsBase = config.baseUrl.replace('http://', 'ws://').replace('https://', 'wss://')
	// #ifdef H5
	const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
	const host = window.location.host
	wsBase = `${protocol}//${host}`
	// #endif
	return `${wsBase}/ws?token=${token}`
}

export function connectWebSocket() {
	if (socketTask) {
		closeWebSocket()
	}

	isClosed = false
	reconnectAttempts = 0

	const url = getWsUrl()
	socketTask = uni.connectSocket({
		url,
		success: () => {},
		fail: (err) => {
			console.error('[WS] 连接失败:', err)
			scheduleReconnect()
		}
	})

	socketTask.onOpen(() => {
		console.log('[WS] 连接成功')
		reconnectAttempts = 0
		startHeartbeat()
	})

	socketTask.onMessage((res) => {
		const data = res.data
		if (data === 'pong') return
		try {
			const parsed = JSON.parse(data)
			messageListeners.forEach(cb => cb(parsed))
		} catch {
			messageListeners.forEach(cb => cb(data))
		}
	})

	socketTask.onClose(() => {
		console.log('[WS] 连接关闭')
		stopHeartbeat()
		socketTask = null
		if (!isClosed) {
			scheduleReconnect()
		}
	})

	socketTask.onError((err) => {
		console.error('[WS] 连接错误:', err)
	})
}

export function closeWebSocket() {
	isClosed = true
	stopHeartbeat()
	clearReconnectTimer()
	if (socketTask) {
		socketTask.close()
		socketTask = null
	}
}

function startHeartbeat() {
	stopHeartbeat()
	heartbeatTimer = setInterval(() => {
		if (socketTask) {
			socketTask.send({
				data: 'ping',
				fail: () => {
					console.warn('[WS] 心跳发送失败')
				}
			})
		}
	}, config.wsHeartbeatInterval)
}

function stopHeartbeat() {
	if (heartbeatTimer) {
		clearInterval(heartbeatTimer)
		heartbeatTimer = null
	}
}

function scheduleReconnect() {
	if (isClosed || reconnectAttempts >= config.wsMaxReconnectAttempts) {
		console.log('[WS] 达到最大重连次数，停止重连')
		return
	}
	clearReconnectTimer()
	const delay = config.wsReconnectDelay * Math.pow(1.5, reconnectAttempts)
	reconnectAttempts++
	console.log(`[WS] ${delay}ms 后尝试第 ${reconnectAttempts} 次重连`)
	reconnectTimer = setTimeout(() => {
		connectWebSocket()
	}, delay)
}

function clearReconnectTimer() {
	if (reconnectTimer) {
		clearTimeout(reconnectTimer)
		reconnectTimer = null
	}
}

export function addMessageListener(cb) {
	if (!messageListeners.includes(cb)) {
		messageListeners.push(cb)
	}
}

export function removeMessageListener(cb) {
	const index = messageListeners.indexOf(cb)
	if (index > -1) {
		messageListeners.splice(index, 1)
	}
}

export function sendWsMessage(data) {
	if (socketTask) {
		socketTask.send({
			data: typeof data === 'string' ? data : JSON.stringify(data),
			fail: (err) => console.error('[WS] 发送失败:', err)
		})
	}
}

export function isWsConnected() {
	return socketTask !== null
}

export default {
	connectWebSocket, closeWebSocket,
	addMessageListener, removeMessageListener,
	sendWsMessage, isWsConnected
}
