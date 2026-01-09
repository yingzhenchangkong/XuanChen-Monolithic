import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import * as IconAll from '@ant-design/icons-vue'
import '@/permission'
import '@/assets/basic.css'

const icons:any = IconAll

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

for(const i in icons){
  app.component(i, icons[i])
}
app.config.globalProperties.$antIcons = IconAll
app.mount('#app')
