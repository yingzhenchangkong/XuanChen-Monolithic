export interface OnlineFormRecord {
  id?: string;
  formName?: string;
  remark?: string;
  /** 设计 JSON（列表/options 接口不返回，详情接口返回） */
  schemaJson?: string;
  createBy?: string;
  createName?: string;
  createTime?: string;
  updateBy?: string;
  updateName?: string;
  updateTime?: string;
  [key: string]: unknown;
}
