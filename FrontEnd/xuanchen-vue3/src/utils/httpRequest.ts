import axios from "axios";
import router from "@/router";
import { message } from "ant-design-vue";
import { useAuthStore } from "@/stores";

//创建一个 axios 实例
const service = axios.create({
  baseURL: import.meta.env.APP_BASE_URL,//所有的请求地址的前缀部分
  timeout: 60000,//请求超时时间(毫秒)
  headers: {//设置后端需要的传参类型
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

//请求拦截器
service.interceptors.request.use(
  (config) => { //在发送请求之前做些什么
    const authStore = useAuthStore();
    const token = authStore.getToken();
    if (token) {
      config.headers['XC-ACCESS-TOKEN'] = token;
    }
    return config;
  },
  (error) => { //对请求错误做些什么
    return Promise.reject(error)
  }
)

//响应拦截器
service.interceptors.response.use(
  (response) => { //对响应数据做点什么
    return response.data;
  },
  (error) => { //对响应错误做点什么
    if (error.response.status === 401) {
      message.error('登录已过期，请重新登录');
      window.sessionStorage.clear();
      router.push('/login');
    }
    return Promise.reject(error);
  }
)

export default service;