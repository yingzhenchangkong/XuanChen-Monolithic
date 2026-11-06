/** 登录日志列表行 */
export interface LogLoginRecord {
  id: string;
  username: string;
  loginIp?: string;
  loginLocation?: string;
  browser?: string;
  os?: string;
  status: number;
  msg?: string;
  loginTime?: string;
}
