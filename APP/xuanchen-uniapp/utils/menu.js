const COLORS = ['#e6f7ff', '#fff7e6', '#f6ffed', '#f9f0ff', '#fff0f6', '#f0f5ff', '#fcffe6', '#fff2e8']
const ICONS = ['📊', '', '📋', '⚙️', '📁', '', '🔧', '']

const ANT_ICON_MAP = {
	'FundProjectionScreenOutlined': '',
	'FundOutlined': '📊',
	'FundFilled': '📊',
	'ToolOutlined': '🔧',
	'ToolFilled': '🔧',
	'ReloadOutlined': '🔄',
	'SettingOutlined': '⚙️',
	'SettingFilled': '⚙️',
	'UserOutlined': '👤',
	'UserAddOutlined': '👤',
	'TeamOutlined': '👥',
	'LockOutlined': '🔒',
	'UnlockOutlined': '🔓',
	'KeyOutlined': '🔑',
	'ShieldOutlined': '🛡️',
	'SafetyCertificateOutlined': '🛡️',
	'HomeOutlined': '🏠',
	'HomeFilled': '🏠',
	'AppstoreOutlined': '📱',
	'AppstoreFilled': '📱',
	'MenuOutlined': '📋',
	'MenuFoldOutlined': '📋',
	'MenuUnfoldOutlined': '📋',
	'DashboardOutlined': '📊',
	'BarChartOutlined': '📊',
	'PieChartOutlined': '📊',
	'LineChartOutlined': '📈',
	'FileTextOutlined': '📄',
	'FileOutlined': '📄',
	'FolderOutlined': '📁',
	'FolderOpenOutlined': '📂',
	'FolderFilled': '📁',
	'NotificationOutlined': '🔔',
	'BellOutlined': '🔔',
	'BellFilled': '🔔',
	'MessageOutlined': '💬',
	'CommentOutlined': '💬',
	'CalendarOutlined': '📅',
	'ClockCircleOutlined': '🕐',
	'TimeCircleOutlined': '🕐',
	'StarOutlined': '⭐',
	'StarFilled': '⭐',
	'HeartOutlined': '❤️',
	'HeartFilled': '❤️',
	'LikeOutlined': '👍',
	'LikeFilled': '👍',
	'SearchOutlined': '🔍',
	'EyeOutlined': '👁️',
	'EyeFilled': '👁️',
	'EditOutlined': '✏️',
	'FormOutlined': '✏️',
	'DeleteOutlined': '🗑️',
	'PlusOutlined': '➕',
	'PlusCircleOutlined': '➕',
	'MinusOutlined': '➖',
	'CheckOutlined': '✅',
	'CheckCircleOutlined': '✅',
	'CloseOutlined': '❌',
	'CloseCircleOutlined': '❌',
	'InfoCircleOutlined': 'ℹ️',
	'WarningOutlined': '⚠️',
	'ExclamationCircleOutlined': '❗',
	'QuestionCircleOutlined': '❓',
	'LoadingOutlined': '⏳',
	'PoweroffOutlined': '⏻',
	'LogoutOutlined': '🚪',
	'LoginOutlined': '🚪',
	'ProfileOutlined': '👤',
	'IdcardOutlined': '🪪',
	'MobileOutlined': '📱',
	'PhoneOutlined': '📞',
	'MailOutlined': '📧',
	'SendOutlined': '📤',
	'InboxOutlined': '📥',
	'PrinterOutlined': '🖨️',
	'SaveOutlined': '💾',
	'CloudOutlined': '☁️',
	'CloudUploadOutlined': '☁️',
	'CloudDownloadOutlined': '☁️',
	'LinkOutlined': '🔗',
	'PaperClipOutlined': '📎',
	'ShopOutlined': '🏪',
	'ShoppingOutlined': '🛒',
	'ShoppingCartOutlined': '🛒',
	'MoneyCollectOutlined': '💰',
	'DollarOutlined': '💲',
	'BankOutlined': '🏦',
	'BuildOutlined': '🏗️',
	'BugOutlined': '🐛',
	'CodeOutlined': '💻',
	'CodeSandboxOutlined': '💻',
	'DatabaseOutlined': '🗄️',
	'ServerOutlined': '🖥️',
	'MonitorOutlined': '🖥️',
	'DesktopOutlined': '🖥️',
	'LaptopOutlined': '💻',
	'GlobalOutlined': '🌐',
	'EnvironmentOutlined': '📍',
	'PushpinOutlined': '📌',
	'FlagOutlined': '🚩',
	'BulbOutlined': '💡',
	'BulbFilled': '💡',
	'RocketOutlined': '🚀',
	'CarOutlined': '🚗',
	'GiftOutlined': '🎁',
	'TrophyOutlined': '🏆',
	'CameraOutlined': '📷',
	'VideoCameraOutlined': '📹',
	'SoundOutlined': '🔊',
	'PlayCircleOutlined': '▶️',
	'PauseCircleOutlined': '⏸️',
	'StopOutlined': '⏹️',
	'TagOutlined': '🏷️',
	'TagsOutlined': '🏷️',
	'BookOutlined': '📖',
	'ReadOutlined': '📖',
	'BookFilled': '📖',
	'ApiOutlined': '🔌',
	'ApiFilled': '🔌',
	'BlockOutlined': '🧱',
	'ClusterOutlined': '🔗',
	'BranchesOutlined': '🌿',
	'ContainerOutlined': '📦',
	'CloudServerOutlined': '☁️',
	'RiseOutlined': '📈',
	'FallOutlined': '📉',
	'StockOutlined': '📈',
	'AlertOutlined': '🚨',
	'AlertFilled': '🚨',
	'FireOutlined': '🔥',
	'FireFilled': '🔥',
	'ThunderboltOutlined': '⚡',
	'ThunderboltFilled': '⚡',
	'CrownOutlined': '👑',
	'RobotOutlined': '🤖',
	'ExperimentOutlined': '🧪',
	'MedicineBoxOutlined': '💊',
	'InsuranceOutlined': '🛡️',
	'ContactsOutlined': '📒',
	'ControlOutlined': '🎛️',
	'GatewayOutlined': '🚪',
	'ScanOutlined': '📡',
	'SecurityScanOutlined': '🔍',
	'SwapOutlined': '🔄',
	'ThumbDownOutlined': '👎',
	'UploadOutlined': '📤',
	'DownloadOutlined': '📥',
	'SortAscendingOutlined': '⬆️',
	'SortDescendingOutlined': '⬇️',
	'DashboardFilled': '📊',
	'RobotFilled': '🤖',
	'RocketFilled': '🚀'
}

export function resolveIcon(iconName, fallbackIndex) {
	if (!iconName) return ICONS[fallbackIndex % ICONS.length]
	if (ANT_ICON_MAP[iconName]) return ANT_ICON_MAP[iconName]
	if (!/^[A-Za-z]/.test(iconName)) return iconName
	return ICONS[fallbackIndex % ICONS.length]
}

export function resolveColor(index) {
	return COLORS[index % COLORS.length]
}

const PAGE_MAP = {
	'system/account': '/pages/user/account-settings',
	'system/person': '/pages/user/account-settings',
	'system/notice': '/pages/notice/index',
	'system/role': '/pages/system/role',
	'system/menu': '/pages/system/menu',
	'system/dict': '/pages/system/dict',
	'system/config': '/pages/system/config',
	'masterdata/dept': '/pages/system/dept',
	'masterdata/post': '/pages/system/post',
	'monitor/onlineuser': '/pages/monitor/online-user',
	'monitor/server': '/pages/monitor/server',
	'monitor/cache': '/pages/monitor/cache',
	'monitor/logoperation': '/pages/monitor/log',
	'monitor/loglogin': '/pages/monitor/log?type=login'
}

const TAB_PAGES = ['/pages/index/index', '/pages/app/index', '/pages/screen/index', '/pages/user/index']

export function navigateToMenu(menu) {
	const path = menu.path || menu.component
	if (!path) {
		uni.showToast({ title: '暂无对应页面', icon: 'none' })
		return
	}
	const key = path.replace(/^\//, '')
	const pagePath = PAGE_MAP[key]
	if (!pagePath) {
		uni.showToast({ title: '暂无对应页面', icon: 'none' })
		return
	}
	if (TAB_PAGES.includes(pagePath)) {
		uni.switchTab({ url: pagePath })
	} else {
		uni.navigateTo({ url: pagePath })
	}
}
