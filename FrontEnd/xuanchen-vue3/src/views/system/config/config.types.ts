export interface ConfigModel {
  id: string;// 主键
  configName: string;//参数名称
  configDescription: string;//参数描述
  configKey: string;//参数键名
  configValue: string;//参数键值
  configType: number | undefined;//参数类型(1系统2业务)
  orderNo: number | undefined;//排序码
  status: number;//状态(1启用0停用)
  delFlag?: number;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}