/** API 访问日志列表行 */
export interface LogApiRecord {
  id: string;
  apiPath?: string;
  method?: string;
  username?: string;
  ip?: string;
  duration?: number;
  status?: number;
  createTime?: string;
}
