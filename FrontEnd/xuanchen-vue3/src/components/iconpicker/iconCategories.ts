/**
 * 图标语义分类表 —— 与 antd 官网「图标列表」的六大分类保持一致。
 *
 * 数据来源：https://ant.design/components/icon-cn/ 的 SSR HTML（结构为 `<h3>分类名</h3><ul><li class="风格">…<span>图标名</span></li>`），
 * 由脚本解析其分类标题与成员得到，未手工增删。
 *
 * 两个关键设计：
 * 1. 只记录**基名**（不含 Outlined / Filled / TwoTone 后缀）。官网同一图标在三种风格下共享分类，
 *    去后缀后 Outlined / Filled / TwoTone 三个 tab 自动复用同一份分类，无需维护三份。
 * 2. 数组顺序即官网展示顺序（StepBackward → StepForward → …），比字母序更符合直觉，因此界面按此顺序渲染。
 *
 * 注意：本表是官网分类的完整快照，包含少量本项目当前图标包里还没有的名字（如官网 6.x 新增的 Anthropic、
 * HarmonyOS 等）。这样后续升级 @ant-design/icons-vue 时，新图标能自动落到正确分类，不必回头改这个文件。
 */

export const ICON_CATEGORY_ORDER = [
  '方向性图标',
  '提示建议性图标',
  '编辑类图标',
  '数据类图标',
  '品牌和标识',
  '网站通用图标',
] as const

export type IconCategory = (typeof ICON_CATEGORY_ORDER)[number]

/** 分类 -> 图标基名，顺序即官网顺序 */
const OFFICIAL: Record<IconCategory, string[]> = {
  '方向性图标': [
    'StepBackward', 'StepForward', 'FastBackward', 'FastForward', 'Shrink', 'ArrowsAlt',
    'Down', 'Up', 'Left', 'Right', 'CaretUp', 'CaretDown',
    'CaretLeft', 'CaretRight', 'UpCircle', 'DownCircle', 'LeftCircle', 'RightCircle',
    'DoubleRight', 'DoubleLeft', 'VerticalLeft', 'VerticalRight', 'VerticalAlignTop', 'VerticalAlignMiddle',
    'VerticalAlignBottom', 'Forward', 'Backward', 'Rollback', 'Enter', 'Retweet',
    'Swap', 'SwapLeft', 'SwapRight', 'ArrowUp', 'ArrowDown', 'ArrowLeft',
    'ArrowRight', 'PlayCircle', 'UpSquare', 'DownSquare', 'LeftSquare', 'RightSquare',
    'Login', 'Logout', 'MenuFold', 'MenuUnfold', 'BorderBottom', 'BorderHorizontal',
    'BorderInner', 'BorderOuter', 'BorderLeft', 'BorderRight', 'BorderTop', 'BorderVerticle',
    'PicCenter', 'PicLeft', 'PicRight', 'RadiusBottomleft', 'RadiusBottomright', 'RadiusUpleft',
    'RadiusUpright', 'Fullscreen', 'FullscreenExit',
  ],
  '提示建议性图标': [
    'Question', 'QuestionCircle', 'Plus', 'PlusCircle', 'Pause', 'PauseCircle',
    'Minus', 'MinusCircle', 'PlusSquare', 'MinusSquare', 'Info', 'InfoCircle',
    'Exclamation', 'ExclamationCircle', 'Close', 'CloseCircle', 'CloseSquare', 'Check',
    'CheckCircle', 'CheckSquare', 'ClockCircle', 'Warning', 'IssuesClose', 'Stop',
  ],
  '编辑类图标': [
    'Edit', 'Form', 'Copy', 'Scissor', 'Delete', 'Snippets',
    'Diff', 'Highlight', 'AlignCenter', 'AlignLeft', 'AlignRight', 'BgColors',
    'Bold', 'Italic', 'Underline', 'Strikethrough', 'Redo', 'Undo',
    'ZoomIn', 'ZoomOut', 'FontColors', 'FontSize', 'LineHeight', 'Dash',
    'SmallDash', 'SortAscending', 'SortDescending', 'Drag', 'OrderedList', 'UnorderedList',
    'RadiusSetting', 'ColumnWidth', 'ColumnHeight',
  ],
  '数据类图标': [
    'AreaChart', 'PieChart', 'BarChart', 'DotChart', 'LineChart', 'RadarChart',
    'HeatMap', 'Fall', 'Rise', 'Stock', 'BoxPlot', 'Fund',
    'Sliders',
  ],
  '品牌和标识': [
    'Android', 'Apple', 'Windows', 'Ie', 'IeCircle', 'IeSquare',
    'Chrome', 'Github', 'Aliwangwang', 'Dingding', 'WeiboSquare', 'WeiboCircle',
    'TaobaoCircle', 'Html5', 'Weibo', 'Twitter', 'TwitterCircle', 'TwitterSquare',
    'Wechat', 'WhatsApp', 'Youtube', 'AlipayCircle', 'Taobao', 'TaobaoSquare',
    'Dingtalk', 'DingtalkCircle', 'DingtalkSquare', 'Skype', 'Qq', 'QqCircle',
    'QqSquare', 'MediumWorkmark', 'Gitlab', 'Medium', 'MediumCircle', 'MediumSquare',
    'Linkedin', 'GooglePlus', 'GooglePlusCircle', 'GooglePlusSquare', 'Dropbox', 'DropboxCircle',
    'DropboxSquare', 'Facebook', 'Codepen', 'CodepenSquare', 'CodeSandbox', 'CodeSandboxSquare',
    'CodeSandboxCircle', 'Amazon', 'AmazonCircle', 'AmazonSquare', 'Google', 'GoogleCircle',
    'GoogleSquare', 'CodepenCircle', 'Alipay', 'AlipaySquare', 'AntDesign', 'AntCloud',
    'Aliyun', 'Zhihu', 'ZhihuCircle', 'ZhihuSquare', 'Slack', 'SlackCircle',
    'SlackSquare', 'Behance', 'BehanceCircle', 'BehanceSquare', 'Dribbble', 'DribbbleCircle',
    'DribbbleSquare', 'Instagram', 'Yuque', 'Alibaba', 'Yahoo', 'Reddit',
    'RedditCircle', 'RedditSquare', 'Sketch', 'SketchCircle', 'SketchSquare', 'WechatWork',
    'OpenAI', 'Anthropic', 'Claude', 'Gemini', 'Mistral', 'DeepSeek',
    'Qwen', 'Perplexity', 'HuggingFace', 'Ollama', 'Replicate', 'ElevenLabs',
    'Telegram', 'Mastodon', 'Threads', 'Snapchat', 'Discord', 'X',
    'Bilibili', 'Pinterest', 'TikTok', 'Spotify', 'Twitch', 'Linux',
    'Java', 'JavaScript', 'Python', 'Ruby', 'DotNet', 'Kubernetes',
    'Docker', 'Baidu', 'HarmonyOS',
  ],
  '网站通用图标': [
    'AccountBook', 'Aim', 'Alert', 'Apartment', 'Api', 'AppstoreAdd',
    'Appstore', 'Audio', 'AudioMuted', 'Audit', 'Bank', 'Barcode',
    'Bars', 'Bell', 'Block', 'Book', 'Border', 'BorderlessTable',
    'Branches', 'Bug', 'Build', 'Bulb', 'Calculator', 'Calendar',
    'Camera', 'Car', 'CarryOut', 'CiCircle', 'Ci', 'Clear',
    'CloudDownload', 'Cloud', 'CloudServer', 'CloudSync', 'CloudUpload', 'Cluster',
    'Code', 'Coffee', 'Comment', 'Compass', 'Compress', 'ConsoleSql',
    'Contacts', 'Container', 'Control', 'Copyright', 'CreditCard', 'Crown',
    'CustomerService', 'Dashboard', 'Database', 'DeleteColumn', 'DeleteRow', 'DeliveredProcedure',
    'DeploymentUnit', 'Desktop', 'Disconnect', 'Dislike', 'Dollar', 'Download',
    'Ellipsis', 'Environment', 'EuroCircle', 'Euro', 'Exception', 'ExpandAlt',
    'Expand', 'Experiment', 'Export', 'Eye', 'EyeInvisible', 'FieldBinary',
    'FieldNumber', 'FieldString', 'FieldTime', 'FileAdd', 'FileDone', 'FileExcel',
    'FileExclamation', 'File', 'FileGif', 'FileImage', 'FileJpg', 'FileMarkdown',
    'FilePdf', 'FilePpt', 'FileProtect', 'FileSearch', 'FileSync', 'FileText',
    'FileUnknown', 'FileWord', 'FileZip', 'Filter', 'Fire', 'Flag',
    'FolderAdd', 'Folder', 'FolderOpen', 'FolderView', 'Fork', 'FormatPainter',
    'Frown', 'Function', 'FundProjectionScreen', 'FundView', 'FunnelPlot', 'Gateway',
    'Gif', 'Gift', 'Global', 'Gold', 'Golden', 'Group',
    'Hdd', 'Heart', 'History', 'Holder', 'Home', 'Hourglass',
    'Idcard', 'Import', 'Inbox', 'InsertRowAbove', 'InsertRowBelow', 'InsertRowLeft',
    'InsertRowRight', 'Insurance', 'Interaction', 'Key', 'Laptop', 'Layout',
    'Like', 'Line', 'Link', 'Loading3Quarters', 'Loading', 'Lock',
    'MacCommand', 'Mail', 'Man', 'MedicineBox', 'Meh', 'Menu',
    'MergeCells', 'Merge', 'Message', 'Meta', 'Mobile', 'MoneyCollect',
    'Monitor', 'Moon', 'More', 'Muted', 'Netflix', 'NodeCollapse',
    'NodeExpand', 'NodeIndex', 'Notification', 'Number', 'OneToOne', 'PaperClip',
    'Partition', 'PayCircle', 'Percentage', 'Phone', 'Picture', 'PlaySquare',
    'PoundCircle', 'Pound', 'Poweroff', 'Printer', 'Product', 'Profile',
    'Project', 'PropertySafety', 'PullRequest', 'Pushpin', 'Qrcode', 'Read',
    'Reconciliation', 'RedEnvelope', 'Reload', 'Rest', 'Robot', 'Rocket',
    'RotateLeft', 'RotateRight', 'SafetyCertificate', 'Safety', 'Save', 'Scan',
    'Schedule', 'Search', 'SecurityScan', 'Select', 'Send', 'Setting',
    'Shake', 'ShareAlt', 'Shop', 'ShoppingCart', 'Shopping', 'Signal',
    'Signature', 'Sisternode', 'Skin', 'Smile', 'Solution', 'Sound',
    'SplitCells', 'Star', 'Subnode', 'Sun', 'Switcher', 'Sync',
    'Table', 'Tablet', 'Tag', 'Tags', 'Team', 'Thunderbolt',
    'ToTop', 'Tool', 'TrademarkCircle', 'Trademark', 'Transaction', 'Translation',
    'Trophy', 'Truck', 'Ungroup', 'Unlock', 'Upload', 'Usb',
    'UserAdd', 'UserDelete', 'User', 'UserSwitch', 'UsergroupAdd', 'UsergroupDelete',
    'Verified', 'VideoCameraAdd', 'VideoCamera', 'Wallet', 'Wifi', 'Woman',
  ],
};

/**
 * 官网新版已下线、但本项目图标包里仍然存在的基名。
 * （CopyrightCircle / DollarCircle 共 6 个图标，官网 6.x 图标列表中已查不到）
 * 这两个都是通用类图形，补进「网站通用图标」，避免它们在选择器里消失。
 */
const LEGACY_APP = ['CopyrightCircle', 'DollarCircle']

/**
 * 分类 -> 图标基名（含补录的 LEGACY_APP），**数组顺序即官网展示顺序**，界面按此排序渲染。
 */
export const ICON_BASE_ORDER: Record<IconCategory, string[]> = {
  ...OFFICIAL,
  '网站通用图标': [...OFFICIAL['网站通用图标'], ...LEGACY_APP],
};

/** 基名 -> 分类，由 OFFICIAL + LEGACY_APP 展开得到 */
export const ICON_CATEGORY_OF: Record<string, IconCategory> = (() => {
  const map: Record<string, IconCategory> = {}
  for (const category of ICON_CATEGORY_ORDER) {
    for (const base of OFFICIAL[category]) map[base] = category
  }
  for (const base of LEGACY_APP) map[base] = '网站通用图标'
  return map
})();

/**
 * 取某个图标名（含风格后缀）所属的分类。
 * 理论上不会走到兜底分支（分类表已覆盖全部基名），保留是为了任何遗漏都不至于让图标从界面上消失。
 */
export function categoryOf(iconName: string): IconCategory {
  const base = iconName.replace(/(Outlined|Filled|TwoTone)$/, '')
  return ICON_CATEGORY_OF[base] ?? '网站通用图标'
}
