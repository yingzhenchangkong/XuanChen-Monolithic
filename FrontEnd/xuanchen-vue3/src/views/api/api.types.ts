/** 数据接口配置 */
export interface ApiRecord {
  id: string;
  apiName: string;
  apiPath: string;
  method?: string;
  description?: string;
  status: number;
  createTime?: string;
}
