export interface DictModel {
  id: string;// 主键
  dictCode: string;
  dictName: string;
  orderNo: number | undefined;//排序码
  status: boolean;//状态(0停用1启用)
  delFlag?: boolean;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}

export interface DictItemModel {
  id: string;// 主键
  dictCode: string;
  dictItemText: string;
  dictItemValue: string;
  orderNo: number | undefined;//排序码
  status: boolean;//状态(0停用1启用)
  delFlag?: boolean;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}