import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useWebSocketStore } from '@/stores'
import { handleUnauthorized } from '@/utils/httpRequest'
// 组件与静态图标改由 unplugin-vue-components + AntDesignVueResolver 按需自动引入（见 vite.config.ts），
// 不再 app.use(Antd) 全量注册、不再全量注册 700+ 图标，未使用代码可被 tree-shaking
import 'ant-design-vue/dist/reset.css'
import { prepareRouteTable } from '@/permission'
import '@/assets/basic.css'

const app = createApp(App)

app.use(createPinia())
// Pinia 安装后注册 WS 鉴权过期处理器：WS 握手收到 1008 时复用 HTTP 401 的统一登出流程
useWebSocketStore().setAuthExpiredHandler(handleUnauthorized)

/**
 * 先让动态路由就绪，再安装 router。
 * router 安装时会立刻用当前地址做首次匹配，而菜单路由是异步注入的（见 prepareRouteTable 注释），
 * 晚一步注册就会让深链接刷新先命中"空匹配"并打出 VUE_ROUTER_R0004 告警。
 * prepareRouteTable 内部有超时兜底且吞掉所有错误，不会阻塞挂载，也不会改变守卫的处置逻辑。
 */
const bootstrap = async () => {
  await prepareRouteTable()
  app.use(router)
  app.mount('#app')
}

void bootstrap()

// 开发期：在控制台打印全量图标清单，按 Outlined / Filled / TwoTone 三组分开输出。
// 数据源直接取自 @ant-design/icons-vue 的实际导出，因此永远与已安装的版本一致，
//
// 必须保留 if (import.meta.env.DEV) 守卫：main.ts 属入口 chunk，
// 若在生产构建中引用图标桶模块，全部 789 个图标组件会被钉进首屏包
// 直接抵消 vite.config.ts 里“全量图标只进异步 chunk”的优化。
// 加了守卫后，prod 构建时 import.meta.env.DEV 被静态替换为 false，整段被消除，产物逐字节不变。
if (import.meta.env.DEV) {
  void import('@ant-design/icons-vue').then((mod) => {
    const NON_ICON = new Set([
      'default',
      '__esModule',
      'createFromIconfontCN',
      'setTwoToneColor',
      'getTwoToneColor',
    ])

    const groups = {
      Outlined: [] as string[],
      Filled: [] as string[],
      TwoTone: [] as string[],
    }

    for (const name of Object.keys(mod)) {
      if (NON_ICON.has(name)) continue
      const style = name.match(/(Outlined|Filled|TwoTone)$/)?.[1]
      if (style === 'Outlined' || style === 'Filled' || style === 'TwoTone') {
        groups[style].push(name)
      }
    }

    groups.Outlined.sort()
    groups.Filled.sort()
    groups.TwoTone.sort()

    const total = groups.Outlined.length + groups.Filled.length + groups.TwoTone.length
    console.log(
      `[icons] 全量图标共 ${total} 个 —— Outlined ${groups.Outlined.length} / Filled ${groups.Filled.length} / TwoTone ${groups.TwoTone.length}`
    )
    console.log(`Outlined (${groups.Outlined.length})`, groups.Outlined)
    console.log(`Filled (${groups.Filled.length})`, groups.Filled)
    console.log(`TwoTone (${groups.TwoTone.length})`, groups.TwoTone)
  })
}
