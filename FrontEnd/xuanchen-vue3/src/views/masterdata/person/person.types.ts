/** 人员新增/编辑提交参数（id 缺省为新增） */
export interface PersonSaveParams {
  id?: string;
  realName?: string;
  idCard?: string;
  /** yyyy-MM-dd */
  birthday?: string;
  nation?: string;
  address?: string;
  nativePlace?: string;
  /** 1男2女 */
  gender?: number | string;
  /** 1未婚2已婚3离异4丧偶 */
  maritalStatus?: number | string;
  /** 1小学2初中3高中4大专5本科6研究生 */
  education?: number | string;
  personCode?: string;
  /** 档案照片相对路径（sys_person.photo） */
  photo?: string;
  mobile?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  email?: string;
  /** yyyy-MM-dd */
  entryDate?: string;
  /** yyyy-MM-dd */
  leaveDate?: string;
  /** 1正式员工2外协3临时人员 */
  personType?: number | string;
  orderNo?: number;
  /** 1在职2试用3离职4退休 */
  status?: number | string;
  /** 部门IDS（sys_person_dept，随人员保存；空数组=清空） */
  deptIds?: Array<string | number>;
  /** 岗位IDS（sys_person_post，随人员保存；空数组=清空） */
  postIds?: Array<string | number>;
  [key: string]: unknown;
}

/** 人员列表行 / 编辑回显记录 */
export interface PersonRecord {
  id?: string;
  realName?: string;
  idCard?: string;
  birthday?: string;
  nation?: string;
  address?: string;
  nativePlace?: string;
  gender?: string | number;
  maritalStatus?: string | number;
  education?: string | number;
  personCode?: string;
  /** 档案照片相对路径（sys_person.photo） */
  photo?: string;
  mobile?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  email?: string;
  entryDate?: string;
  leaveDate?: string;
  personType?: string | number;
  orderNo?: number;
  status?: string | number;
  /** 部门IDS（关系表 sys_person_dept，人员管理内维护） */
  deptIds?: Array<string | number>;
  /** 岗位IDS（关系表 sys_person_post，人员管理内维护） */
  postIds?: Array<string | number>;
  /** 部门名称（后端按 deptIds 翻译，只读展示） */
  deptNames?: string;
  /** 岗位名称（后端按 postIds 翻译，只读展示） */
  postNames?: string;
  [key: string]: unknown;
}
