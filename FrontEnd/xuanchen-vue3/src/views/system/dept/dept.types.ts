/**
 * 部门传输数据接口
 * 用于定义部门数据传输时的结构
 */
export interface DeptTranData {
  selectedKey: string;
  ifAddChild: boolean;
}

export interface DeptModel {
  id: string;// 主键
  deptCode: string;
  parentDeptCode: string | undefined;
  deptName: string;
  orderNo: number | undefined;//排序码
  status: boolean;//状态(0停用1启用)
  delFlag?: boolean;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}