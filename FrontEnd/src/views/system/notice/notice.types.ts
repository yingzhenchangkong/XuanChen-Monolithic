export interface NoticeModel {
  id: string;// 主键
  title: string;
  content: string;
  noticeStatusId:string;
  status?: boolean;//状态(0停用1启用)
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
  readStatus: boolean;
  readTime: Date;
}