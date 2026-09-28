import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
// 组件与静态图标改由 unplugin-vue-components + AntDesignVueResolver 按需自动引入（见 vite.config.ts），
// 不再 app.use(Antd) 全量注册、不再全量注册 700+ 图标，未使用代码可被 tree-shaking
import 'ant-design-vue/dist/reset.css'
import '@/permission'
import '@/assets/basic.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
