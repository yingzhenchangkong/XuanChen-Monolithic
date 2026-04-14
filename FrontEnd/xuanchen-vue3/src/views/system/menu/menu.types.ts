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
  status: boolean;
  orderNo: string;
}
