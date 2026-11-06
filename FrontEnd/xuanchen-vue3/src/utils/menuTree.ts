import { getMenuListApi } from '@/views/system/menu/menu.api';
import type { MenuRecord } from '@/types/api';

/** 树形下拉选项（a-tree-select 直接消费） */
export interface MenuTreeOption {
  value: string;
  title: string;
  children?: MenuTreeOption[];
}

/**
 * 递归把菜单树转成下拉选项。
 * 按钮（menuType === 'F'）不能作为上级菜单，直接剪掉其分支。
 */
const toOptions = (nodes: MenuRecord[] | null | undefined): MenuTreeOption[] => {
  const result: MenuTreeOption[] = [];
  (nodes || []).forEach((node) => {
    if (node.menuType === 'F') {
      return;
    }
    const id = typeof node.id === 'string' ? node.id : '';
    if (!id) {
      return;
    }
    const children = toOptions(node.children);
    const option: MenuTreeOption = {
      value: id,
      title: String(node.title ?? id),
    };
    if (children.length) {
      option.children = children;
    }
    result.push(option);
  });
  return result;
};

/**
 * 「上级菜单」树形下拉选项。
 * 顶部固定一条「根菜单（顶级）」对应 parentMenuId = '0'，与生成器里 0 表示顶级菜单约定一致。
 */
export const getMenuTreeOptions = async (): Promise<MenuTreeOption[]> => {
  const root: MenuTreeOption = { value: '0', title: '根菜单（顶级）' };
  try {
    const res = await getMenuListApi();
    if (res.code !== 200) {
      console.warn('【generator】读取菜单树失败:', res.msg);
      return [root];
    }
    const options = [root, ...toOptions(res.data?.records as MenuRecord[] | undefined)];
    if (import.meta.env.DEV) {
      // 下拉为空时先看这条日志：1 表示只拿到根节点（说明菜单树没读到）
      console.info('【generator】上级菜单选项数:', options.length);
    }
    return options;
  } catch (error) {
    // 菜单树拿不到时仍可选择根菜单，不阻断生成流程
    console.warn('【generator】读取菜单树异常:', error);
    return [root];
  }
};
