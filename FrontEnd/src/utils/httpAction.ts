import service from "./httpRequest";

/** GET */
export function getAction(url: string, params: object) {
  return service({
      url: url,
      method: 'get',
      params: params,
  })
}
/** POST */
export function postAction(url: string, params: object) {
  return service({
      url: url,
      method: 'post',
      data: params,
  })
}
/** PUT */
export function putAction(url: string, params: object) {
  return service({
      url: url,
      method: 'put',
      data: params,
  })
}
/** DELETE */
export function deleteAction(url: string, params: object) {
  return service({
      url: url,
      method: 'delete',
      params: params,
  })
}
/** POST | PUT */
export function httpAction(url: string, params: object, method: string) {
  return service({
      url: url,
      method: method,
      data: params,
  })
}
/** 文件上传 */
export function uploadAction(url: string, params: object) {
  return service({
      url: url,
      method: 'post',
      data: params,
      headers: {
          'Content-Type': 'multipart/form-data'
      }
  })
}
/** 导出 */
export function exportAction(url: string, params: object, method = 'get') {
  if (method == 'get') {
      return service({
          url: url,
          method: 'get',
          params: params,
          responseType: 'blob'
      })
  } else {
      return service({
          url: url,
          method: 'post',
          data: params,
          responseType: 'blob'
      })
  }
}