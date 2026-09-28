export interface NoticeModel {
  id: string;// 主键
  title: string;
  content: string;
  noticeStatusId:string;
  status?: number;//状态(1发布2撤销)
  createBy?: string;//创建人
  createName?: string;//创建人姓名
  createTime?: Date | undefined;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}

export interface NoticeStatusModel {
  id: string;// 主键
  noticeId: string;
  userId: string;
  readStatus: number;//是否已读(0未读1已读)
  readTime: Date;
}

/**
 * 通知发布/撤销/恢复等写操作入参。
 * 字段全部可选，索引签名兼容后端 SysNotice 上的其余字段。
 */
export interface NoticeSaveParams {
  id?: string;
  title?: string;
  content?: string;
  listUser?: string[];
  status?: number;
  noticeStatusId?: string;
  [key: string]: unknown;
}