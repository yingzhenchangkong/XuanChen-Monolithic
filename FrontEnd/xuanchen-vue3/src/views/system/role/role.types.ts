/** 角色新增/编辑提交参数（id 缺省为新增，同时兼容表单模型） */
export interface RoleModel {
  id?: string;
  roleCode?: string;
  roleName?: string;
  roleDescription?: string;
  status?: number;
  orderNo?: string;
  [key: string]: unknown;
}

/** 菜单树节点（/system/menu/list 的 records 项，a-tree 经 fieldNames 映射 id/title/children） */
export interface MenuTreeNode {
  id?: string | number;
  title?: string;
  children?: MenuTreeNode[];
  [key: string]: unknown;
}

/** 角色列表行数据（/system/role/list 的 records 项，供 Index 页 useList 行类型使用） */
export interface RoleRecord {
  id: string;
  status: number;
  [key: string]: unknown;
}

/** 未授权用户下拉选项（/system/userrole/listUnAuthUser 的 records 项） */
export interface UnAuthUserOption {
  userId: string;
  userName?: string;
  nickName?: string;
  [key: string]: unknown;
}
