/** 流程图/架构图记录（与后端 OnlineMindmap 对应，图数据以快照 JSON 存储） */
export interface OnlineDocRecord {
  id: string;
  docName: string;
  /** 快照 JSON（列表接口不返回，详情接口返回） */
  snapshot?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}
