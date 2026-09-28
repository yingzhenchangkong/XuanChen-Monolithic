import service from "./httpRequest";
import type { AxiosRequestConfig } from 'axios';
import type { Result } from '@/types/api';

/** 允许的 HTTP 方法字面量（httpAction 的 method 参数不再接受任意 string） */
export type HttpMethod = 'get' | 'post' | 'put' | 'delete' | 'patch';
/** 导出接口允许的方法 */
export type ExportMethod = 'get' | 'post';

/**
 * 响应拦截器已把 axios 响应解包为后端 Result（blob 请求除外），
 * 这里统一收口为 Promise<Result<T>>，调用方不再需要 as any
 */
const requestJson = <T>(config: AxiosRequestConfig): Promise<Result<T>> => {
  return service(config) as unknown as Promise<Result<T>>;
}

/** GET */
export function getAction<T = unknown>(url: string, params?: object) {
  return requestJson<T>({
      url,
      method: 'get',
      params,
  })
}
/** POST */
export function postAction<T = unknown>(url: string, data?: object) {
  return requestJson<T>({
      url,
      method: 'post',
      data,
  })
}
/** PUT */
export function putAction<T = unknown>(url: string, data?: object) {
  return requestJson<T>({
      url,
      method: 'put',
      data,
  })
}
/** DELETE（参数走 query string） */
export function deleteAction<T = unknown>(url: string, params?: object) {
  return requestJson<T>({
      url,
      method: 'delete',
      params,
  })
}
/** POST | PUT 等通用方法，method 受 HttpMethod 字面量约束 */
export function httpAction<T = unknown>(url: string, data: object | undefined, method: HttpMethod) {
  return requestJson<T>({
      url,
      method,
      data,
  })
}
/**
 * 文件上传：入参必须是 FormData。
 * 不能手写 Content-Type: multipart/form-data——缺少 boundary 时后端无法解析，
 * axios 的 xhr 适配器在 data 为 FormData 时会自动删除该头，交由浏览器生成带 boundary 的值
 */
export function uploadAction<T = unknown>(url: string, data: FormData) {
  return requestJson<T>({
      url,
      method: 'post',
      data,
  })
}
/** 导出（二进制流，响应为 Blob；业务失败时后端可能返回 JSON 错误体，由调用方解析拦截） */
export function exportAction(url: string, params?: object, method: ExportMethod = 'get') {
  return service({
      url,
      method,
      // GET 参数走 params，POST 走 body
      ...(method === 'get' ? { params } : { data: params }),
      responseType: 'blob',
  }) as unknown as Promise<Blob>
}
