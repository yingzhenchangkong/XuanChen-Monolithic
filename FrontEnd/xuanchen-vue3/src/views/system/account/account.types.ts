/** 账号新增/编辑提交参数（id 缺省为新增） */
export interface AccountSaveParams {
  id?: string | number;
  userName: string;
  password?: string;
  /** 昵称（系统内展示名） */
  nickName?: string;
  /** 头像相对路径 */
  avatar?: string;
  /** 手机号（登录凭证/短信验证） */
  mobile?: string;
  /** 邮箱（找回密码/通知） */
  email?: string;
  /** 绑定的人员ID（可为空=纯账号） */
  personId?: string;
  /** 字典值在表单中为字符串，后端兼容数字 */
  status?: number | string;
  roleIds?: Array<string | number>;
  [key: string]: unknown;
}

/**
 * 账号列表行 / 编辑回显记录。
 * 含 sys_account 的列 + 只读的 personName（绑定人员姓名，列表展示用，不参与编辑提交）。
 * 昵称/头像/手机号/邮箱属账号（登录凭证 + 展示身份），人员档案的字段请走人员管理。
 */
export interface AccountRecord {
  id?: string | number;
  personId?: string;
  userName?: string;
  /** 昵称（系统内展示名） */
  nickName?: string;
  /** 头像相对路径 */
  avatar?: string;
  /** 手机号（登录凭证/短信验证） */
  mobile?: string;
  /** 邮箱（找回密码/通知） */
  email?: string;
  /** 只读：绑定人员姓名（sys_person.real_name） */
  personName?: string;
  status?: string | number;
  roleIds?: Array<string | number>;
  [key: string]: unknown;
}
