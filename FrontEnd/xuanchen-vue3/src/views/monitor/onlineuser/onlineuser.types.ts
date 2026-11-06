/** 在线用户列表行 */
export interface OnlineUserRecord {
  token: string;
  username?: string;
  realName?: string;
  loginIp?: string;
  loginLocation?: string;
  browser?: string;
  os?: string;
  loginTime?: string;
  lastAccessTime?: string;
}
