/** 操作日志列表行 */
export interface LogOperationRecord {
  id: string;
  title?: string;
  businessType?: string;
  username?: string;
  operIp?: string;
  status: number;
  operTime?: string;
}
