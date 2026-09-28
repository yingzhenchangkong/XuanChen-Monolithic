import { defineStore } from "pinia";
import { ref, reactive } from 'vue';
import type { MenuRecord } from '@/types/api';

export const useMenuStore = defineStore('menu', () => {
  /** 未加载时为 undefined；加载后为后端菜单数组（零菜单账号为 []，二者必须区分） */
  const menuList = ref<MenuRecord[] | undefined>();
  const getMenuList = (): MenuRecord[] | undefined => {
    return menuList.value;
  }
  const setMenuList = (menuListIn: MenuRecord[] | undefined) => {
    menuList.value = menuListIn;
  }
  const state = reactive({
    openKeys: [''],
    selectedKeys: [''],
  })
  return { getMenuList, setMenuList, state }
})
