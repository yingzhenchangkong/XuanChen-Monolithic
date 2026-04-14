import { defineStore } from "pinia";
import { ref, reactive } from 'vue';

export const useMenuStore = defineStore('menu', () => {
  const menuList = ref();
  const getMenuList = () => {
    return menuList.value;
  }
  const setMenuList = (menuListIn: any) => {
    menuList.value = menuListIn;
  }
  const state = reactive({
    openKeys: [''],
    selectedKeys: [''],
  })
  return { getMenuList, setMenuList, state }
})