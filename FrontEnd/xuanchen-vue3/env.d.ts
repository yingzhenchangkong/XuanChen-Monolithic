/// <reference types="vite/client" />

/**
 * 自定义环境变量（见 .env.dev / .env.test / .env.pro）。
 * 浏览器侧变量必须以 VITE_ 或本项目约定的 APP_ 前缀声明，
 * 未在此声明的 import.meta.env.xxx 会在 type-check 时报错。
 */
interface ImportMetaEnv {
  /** axios 统一前缀；同源相对地址（/api）或完整跨域地址 */
  readonly APP_BASE_URL: string;
  /** 文件上传接口的后端相对路径 */
  readonly APP_FILE_UPLOAD_PATH: string;
  /** 文件静态访问的后端相对路径 */
  readonly APP_FILE_VIEW_PATH: string;
  /** dev server 代理目标（仅 Node 侧读取） */
  readonly APP_PROXY_TARGET?: string;
  /** WS 握手时代理改写的 Origin（仅 dev） */
  readonly APP_PROXY_WS_ORIGIN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'virtual:ant-icons-all' {
  import type { Component } from 'vue'
  // 由 vite.config.ts 中的 antIconsVirtual 插件按 icon.json 生成，键为 antd 图标组件名
  export const icons: Record<string, Component>
}
