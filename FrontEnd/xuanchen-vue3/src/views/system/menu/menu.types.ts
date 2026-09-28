export interface MenuModel {
  id: string;
  parentId: string | undefined;
  name: string;
  title: string;
  path: string;
  component: string;
  perms: string;
  icon: string;
  menuType: string | undefined;
  status: number;
  orderNo: string;
}

/**
 * 菜单新增/编辑提交参数。
 * 字段全部可选（新增/编辑形态不同），索引签名兼容后端实体上的其余字段。
 */
export interface MenuSaveParams {
  id?: string;
  parentId?: string;
  name?: string;
  title?: string;
  path?: string;
  component?: string | null;
  perms?: string;
  icon?: string;
  menuType?: string;
  status?: number;
  orderNo?: string | number;
  [key: string]: unknown;
}
