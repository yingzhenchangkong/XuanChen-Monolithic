export interface DictModel {
  id: string;// 主键
  dictCode: string;
  dictName: string;
  orderNo: number | undefined;//排序码
  status: number;//状态(1启用0停用)
  delFlag?: number;//删除状态(0正常1已删除)
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
  status: number;//状态(1启用0停用)
  delFlag?: number;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}

/** 字典新增/编辑提交参数（id 缺省为新增） */
export interface DictSaveParams {
  id?: string;// 主键
  dictCode?: string;// 字典编码
  dictName?: string;// 字典名称
  orderNo?: number;//排序码
  status?: number;//状态(1启用0停用)
  delFlag?: number;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
  [key: string]: unknown;
}

/** 字典项新增/编辑提交参数（id 缺省为新增） */
export interface DictItemSaveParams {
  id?: string;// 主键
  dictCode?: string;// 所属字典编码
  dictItemText?: string;// 名称
  dictItemValue?: string;// 数据值
  orderNo?: number;//排序码
  status?: number;//状态(1启用0停用)
  delFlag?: number;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
  [key: string]: unknown;
}