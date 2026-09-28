/** 用户新增/编辑提交参数（id 缺省为新增） */
export interface UserSaveParams {
  id?: string | number;
  userName: string;
  nickName?: string;
  password?: string;
  mobile?: string;
  email?: string;
  avatar?: string;
  /** 字典值在表单中为字符串，后端兼容数字 */
  status?: number | string;
  deptId?: string | number;
  roleIds?: Array<string | number>;
  deptIds?: Array<string | number>;
  postIds?: Array<string | number>;
  [key: string]: unknown;
}

/** 用户列表行 / 编辑回显记录 */
export interface UserRecord {
  id?: string | number;
  userName?: string;
  nickName?: string;
  mobile?: string;
  email?: string;
  avatar?: string;
  status?: string | number;
  roleIds?: Array<string | number>;
  deptIds?: Array<string | number>;
  postIds?: Array<string | number>;
  [key: string]: unknown;
}
