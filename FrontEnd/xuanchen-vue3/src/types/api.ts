/**
 * 前后端统一契约类型。
 * Result 对应后端 com.xuanchen.common.entity.Result；
 * PageResult 对应 MyBatis-Plus 分页对象。
 */

/** 后端统一响应体 */
export interface Result<T = unknown> {
  code: number;
  msg: string;
  data: T;
}

/** MyBatis-Plus 分页返回结构 */
export interface PageResult<T = unknown> {
  records: T[];
  total: number;
  size?: number;
  current?: number;
  pages?: number;
}

/** 列表查询公共分页参数（各业务查询表单可在其上扩展具体字段） */
export interface PageQuery {
  pageNo?: number;
  pageSize?: number;
  [key: string]: unknown;
}

/** 会话中保存的登录用户信息（登录响应去掉 token 后的主体） */
export interface UserInfo {
  id?: string | number;
  userName: string;
  nickName?: string;
  avatar?: string;
  mobile?: string;
  email?: string;
  /** 1 = 必须先修改密码（对应后端 pwdResetRequired） */
  pwdResetRequired?: number;
}

/** 登录 / 改密成功响应 data：会话用户信息 + 新 token */
export interface LoginResult extends UserInfo {
  token: string;
}

/** 后端菜单数据（同时用于动态路由生成与侧边栏渲染） */
export interface MenuMeta {
  title?: string;
  icon?: string;
  keepAlive?: boolean;
  [key: string]: unknown;
}

export interface MenuRecord {
  path: string;
  name?: string;
  /** 后端存组件相对路径字符串，permission.ts 会映射为懒加载组件；目录节点为 null/空串 */
  component?: string | null;
  redirect?: string;
  meta?: MenuMeta;
  children?: MenuRecord[] | null;
  [key: string]: unknown;
}

/** 页签栏存储项（sessionStorage tabslist） */
export interface TabItem {
  name: string;
  title: string;
  [key: string]: unknown;
}

/** 通用下拉选项（各 /select 接口） */
export interface SelectOption {
  label: string;
  value: string | number;
  [key: string]: unknown;
}
