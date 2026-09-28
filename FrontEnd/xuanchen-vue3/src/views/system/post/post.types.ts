/** 岗位新增/编辑提交参数（id 缺省为新增，同时兼容表单模型） */
export interface PostModel {
  id?: string;
  postCode?: string;
  postName?: string;
  postDescription?: string;
  status?: number;
  orderNo?: string;
  [key: string]: unknown;
}

/** 岗位列表行数据（/system/post/list 的 records 项，供 Index 页 useList 行类型使用） */
export interface PostRecord {
  id: string;
  status: number;
  [key: string]: unknown;
}

/** 未分配员工下拉选项（/system/userpost/listUnAssignUser 的 records 项） */
export interface UnAssignUserOption {
  userId: string;
  userName?: string;
  nickName?: string;
  [key: string]: unknown;
}
