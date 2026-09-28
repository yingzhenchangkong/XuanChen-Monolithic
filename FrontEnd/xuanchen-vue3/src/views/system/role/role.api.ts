import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result, SelectOption, PageResult } from '@/types/api';
import type { RoleModel, MenuTreeNode, UnAuthUserOption } from './role.types';

enum RoleApiUrl {
  INDEX_LIST = '/system/role/list',
  INDEX_DELETE = '/system/role/delete',
  INDEX_DELETE_BATCH = '/system/role/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/role/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/role/importExcel',
  INDEX_CHANGE_STATUS = '/system/role/changeStatus',

  OPERATION_ADD = '/system/role/add',
  OPERATION_EDIT = '/system/role/edit',
  OPERATION_VALIDATE = '/system/role/validate',

  RECBIN_LIST = '/system/role/listRecycleBin',
  RECBIN_DELETE = '/system/role/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/system/role/deleteRecycleBinBatch',
  RECBIN_REVERT = '/system/role/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/system/role/revertRecycleBinBatch',

  ASSIGN_MENU_LIST_ALL_MENU = '/system/menu/list',
  ASSIGN_MENU_LIST_AUTH_MENU = '/system/rolemenu/listAuthMenu',
  ASSIGN_MENU_SAVE_AUTH_MENU = '/system/rolemenu/saveAuthMenu',

  ASSIGN_USER_LIST_AUTH_USER = '/system/userrole/listAuthUser',
  ASSIGN_USER_LIST_UN_AUTH_USER = '/system/userrole/listUnAuthUser',
  ASSIGN_USER_AUTH = '/system/userrole/auth',
  ASSIGN_USER_AUTH_BATCH = '/system/userrole/authBatch',
  ASSIGN_USER_CANCEL_AUTH = '/system/userrole/cancelAuth',
  ASSIGN_USER_CANCEL_AUTH_BATCH = '/system/userrole/cancelAuthBatch',

  SELECT = '/system/role/select',
}

export { RoleApiUrl };

export const getRoleSelect = async (): Promise<SelectOption[]> => {
  const res = await getAction<SelectOption[]>(RoleApiUrl.SELECT, {});
  return res.data;
}

export const validateRoleCodeApi = async (id: string, roleCode: string): Promise<void> => {
  const res = await getAction(RoleApiUrl.OPERATION_VALIDATE, { id, roleCode });
  if (res.code !== 200) {
    return Promise.reject("角色编码已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateRoleNameApi = async (id: string, roleName: string): Promise<void> => {
  const res = await getAction(RoleApiUrl.OPERATION_VALIDATE, { id, roleName });
  if (res.code !== 200) {
    return Promise.reject("角色名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = (data: RoleModel) => {
  const httpUrl = data.id ? RoleApiUrl.OPERATION_EDIT : RoleApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

export const getListAllMenu = async (): Promise<MenuTreeNode[]> => {
  const res = await getAction<PageResult<MenuTreeNode>>(RoleApiUrl.ASSIGN_MENU_LIST_ALL_MENU, {});
  return res.data.records;
};

export const getListAuthMenu = async (roleId: string): Promise<string[]> => {
  const res = await getAction<string[]>(RoleApiUrl.ASSIGN_MENU_LIST_AUTH_MENU, { roleId });
  return res.data;
}

export const saveAuthMenu = (roleId: string, menuIds: string[]): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.ASSIGN_MENU_SAVE_AUTH_MENU, { roleId, menuIds });
}

export const cancelAuthUser = (userId: string, roleId: string): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.ASSIGN_USER_CANCEL_AUTH, { userId, roleId });
};

export const cancleAuthUserBatch = (userIds: string[], roleId: string): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.ASSIGN_USER_CANCEL_AUTH_BATCH, { userIds, roleId });
}

export const authUser = (userId: string, roleId: string): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.ASSIGN_USER_AUTH, { userId, roleId });
};

export const authUserBatch = (userIds: string[], roleId: string): Promise<Result<null>> => {
  return postAction<null>(RoleApiUrl.ASSIGN_USER_AUTH_BATCH, { userIds, roleId });
}

export const getListUnAuthUser = (roleId: string) => {
  return getAction<PageResult<UnAuthUserOption>>(RoleApiUrl.ASSIGN_USER_LIST_UN_AUTH_USER, { roleId });
}
