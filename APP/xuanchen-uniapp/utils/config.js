const DEV_BASE_URL = 'http://10.0.2.2:8080'
const PROD_BASE_URL = 'https://api.xuanchen.com'

let baseUrl = DEV_BASE_URL

// #ifdef H5
baseUrl = '/api'
// #endif

export const config = {
	baseUrl,
	appName: '萱晨',
	version: '1.0.0',
	tokenHeader: 'XC-ACCESS-TOKEN',
	wsHeartbeatInterval: 25000,
	wsReconnectDelay: 3000,
	wsMaxReconnectAttempts: 5,
	tokenKey: 'xc_access_token',
	userInfoKey: 'xc_user_info'
}

export default config
