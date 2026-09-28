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
  status: number;//状态(0停用1启用)
  delFlag?: number;//删除状态(0正常1已删除)
  createBy?: string;//创建人
  createTime?: Date;//创建时间
  updateBy?: string;//更新人
  updateTime?: Date;//更新时间
}

/**
 * 部门新增/编辑提交参数。
 * 字段全部可选，索引签名兼容后端 SysDept 上的其余字段。
 */
export interface DeptSaveParams {
  id?: string;
  deptCode?: string;
  parentDeptCode?: string;
  deptName?: string;
  orderNo?: number;
  status?: number;
  [key: string]: unknown;
}

/**
 * 部门树节点（/system/dept/getDeptTree 直接返回节点数组，未包 Result）。
 * 字段名与后端 SysDeptTreeVO 的 title/key/children 对齐。
 */
export interface DeptRecord {
  title: string;
  key: string;
  children?: DeptRecord[] | null;
  [key: string]: unknown;
}

/**
 * getSelectedDept 直接返回的部门信息对象（历史接口未包 Result）。
 */
export interface DeptSelectedRecord {
  id: string;
  deptCode: string;
  parentDeptCode?: string;
  deptName: string;
  status: number;
  orderNo?: number;
  [key: string]: unknown;
}
