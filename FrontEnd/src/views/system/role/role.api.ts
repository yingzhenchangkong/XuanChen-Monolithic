import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { RoleModel } from './role.types';

enum RoleApiUrl {
  INDEX_LIST = '/system/role/list',
  INDEX_DELETE = '/system/role/delete',
  INDEX_DELETE_BATCH = '/system/role/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/role/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/role/importExcel',
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

export const getRoleSelect = async () => {
  const res: any = await getAction(RoleApiUrl.SELECT, {});
  return res.data;
}

export const validateRoleCodeApi = async (id: string, roleCode: string) => {
  const res: any = await getAction(RoleApiUrl.OPERATION_VALIDATE, { id, roleCode });
  if (res.code === 500) {
    return Promise.reject("角色编码已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateRoleNameApi = async (id: string, roleName: string) => {
  const res: any = await getAction(RoleApiUrl.OPERATION_VALIDATE, { id, roleName });
  if (res.code === 500) {
    return Promise.reject("角色名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const saveOrUpdate = async (data: RoleModel) => {
  const httpUrl = data.id ? RoleApiUrl.OPERATION_EDIT : RoleApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const getListAllMenu = async () => {
  const res: any = await getAction(RoleApiUrl.ASSIGN_MENU_LIST_ALL_MENU, {});
  return res.data.records;
};

export const getListAuthMenu = async (roleId: string) => {
  const res: any = await getAction(RoleApiUrl.ASSIGN_MENU_LIST_AUTH_MENU, { roleId });
  return res.data;
}

export const saveAuthMenu = async (roleId: string, menuIds: string[]) => {
  return await postAction(RoleApiUrl.ASSIGN_MENU_SAVE_AUTH_MENU, { roleId, menuIds });
}

export const cancelAuthUser = async (userId: string, roleId: string) => {
  return await postAction(RoleApiUrl.ASSIGN_USER_CANCEL_AUTH, { userId, roleId });
};

export const cancleAuthUserBatch = async (userIds: string[], roleId: string) => {
  return await postAction(RoleApiUrl.ASSIGN_USER_CANCEL_AUTH_BATCH, { userIds, roleId });
}

export const authUser = async (userId: string, roleId: string) => {
  return await postAction(RoleApiUrl.ASSIGN_USER_AUTH, { userId, roleId });
};

export const authUserBatch = async (userIds: string[], roleId: string) => {
  return await postAction(RoleApiUrl.ASSIGN_USER_AUTH_BATCH, { userIds, roleId });
}

export const getListUnAuthUser = async (roleId: string) => {
  return await getAction(RoleApiUrl.ASSIGN_USER_LIST_UN_AUTH_USER, { roleId });
}