import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'

import { defineConfig, loadEnv } from 'vite'
import type { Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

/**
 * 虚拟模块 virtual:ant-icons-all：
 * 仅“菜单动态图标解析”和“图标选择器”需要按名字访问全量 antd 图标。
 * 从桶模块 import * 会把全量 700+ 图标钉死在引用它的初始 chunk 中，
 * 这里改用 788 条图标深度路径（每个图标独立 ES 模块）生成聚合模块，
 * 再让使用方以动态 import() / 懒加载页面方式引用，使全量图标只进异步 chunk
 */
const VIRTUAL_ICONS_ID = 'virtual:ant-icons-all'
const antIconsVirtual = (): Plugin => {
  const resolvedId = '\0' + VIRTUAL_ICONS_ID
  const iconNames: string[] = JSON.parse(
    readFileSync(fileURLToPath(new URL('./src/components/iconpicker/icon.json', import.meta.url)), 'utf-8')
  )
  const code = [
    ...iconNames.map((name) => `import ${name} from '@ant-design/icons-vue/es/icons/${name}.js'`),
    `export const icons = { ${iconNames.join(', ')} }`,
  ].join('\n')
  return {
    name: 'ant-icons-virtual',
    resolveId(id) {
      return id === VIRTUAL_ICONS_ID ? resolvedId : null
    },
    load(id) {
      return id === resolvedId ? code : null
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  // envPrefix 为 APP_，这里用同一前缀读取（proxy target 仅供 Node 侧 dev server 使用）
  const env = loadEnv(mode, fileURLToPath(new URL('.', import.meta.url)), 'APP_')
  // 后端地址只作为 dev server 的代理目标，绝不会出现在浏览器侧，默认本地 8080，
  // 可在 .env.<mode> 或环境变量中通过 APP_PROXY_TARGET 覆盖
  const proxyTarget = env.APP_PROXY_TARGET || 'http://localhost:8080'
  // WebSocket 握手按规范必须携带 Origin，且后端有防 CSWSH 的 Origin 白名单
  //（默认仅 http://localhost:8060）。dev server 端口可能被占用而自动切换，
  // 这里由代理在 WS 升级时统一改写 Origin，使任意 dev 端口都无需改动后端 CORS 配置
  const proxyWsOrigin = env.APP_PROXY_WS_ORIGIN || 'http://localhost:8060'

  return {
    plugins: [
      vue(),
      // 全量图标聚合虚拟模块（仅异步 chunk 使用）
      antIconsVirtual(),
      // ant-design-vue 组件与模板中静态使用的图标按需自动引入（可 tree-shaking），
      // v4 样式由 cssinjs 在组件使用时注入，入口保留 reset.css 即可，无需 resolver 导入样式
      Components({
        resolvers: [
          // 图标走深度路径引入（独立 ES 模块），避免 resolver 从桶模块具名引入时，
          // 被“全量图标命名空间”的共享模块图钉死在初始 chunk
          (componentName: string) => {
            if (/^(?:[A-Za-z][A-Za-z0-9]*)(Outlined|Filled|TwoTone)$/.test(componentName)) {
              return {
                name: 'default',
                as: componentName,
                from: `@ant-design/icons-vue/es/icons/${componentName}.js`,
              }
            }
            return undefined
          },
          AntDesignVueResolver({ importStyle: false }),
        ],
        dts: false,
      }),
      // DevTools 仅本地开发服务启用；任何 build（pro/test）都不打包其运行时
      ...(command === 'serve' ? [vueDevTools()] : []),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    envPrefix: "APP_",
    server: {
      port: 8060,
      proxy: {
        // 浏览器始终同源请求 /api/**，由 dev server 转发到后端并去掉 /api 前缀，
        // 开发期不再直连后端、不依赖后端 CORS 放行。
        // 注意：不能直接代理 /system、/monitor 等路径——它们同时是前端 SPA 路由前缀
        // （如页面地址 /system/user），直接代理会把浏览器的页面导航/history 回退劫持到后端，
        // 因此用独立的 /api 命名空间隔离接口与页面
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true, // /api/ws WebSocket 升级请求同样转发（rewrite 对 ws 生效）
          rewrite: (path) => path.replace(/^\/api/, ''),
          configure: (proxy) => {
            // 浏览器视角这是同源请求（页面与 /api 同在 dev server 端口下），本就不存在 CORS。
            // 但 http-proxy 默认会原样转发浏览器的 Origin 头，后端 CORS 白名单若未包含
            // 当前 dev server 端口（如 8060 被占用自动切到 8061），会直接回 403
            // “Invalid CORS request”——开发期又被后端 CORS 配置绑架。
            // dev server 才是对浏览器负责的同源端点，转发时剥离 HTTP 请求的 Origin，
            // 让后端按普通同源请求处理，不再受后端 CORS 白名单约束
            proxy.on('proxyReq', (req: any) => req.removeHeader('origin'));
            // WS 不能剥离 Origin（后端强制白名单校验），改为统一改写为白名单内的 dev 来源，
            // 这样 dev server 在任意端口都能完成 WS 握手
            proxy.on('proxyReqWs', (req: any) => req.setHeader('origin', proxyWsOrigin));
          },
        },
      },
    },
  }
})
