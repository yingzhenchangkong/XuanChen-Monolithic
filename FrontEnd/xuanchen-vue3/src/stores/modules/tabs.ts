import { defineStore } from 'pinia';
import { ref, watch } from 'vue';
import type { TabItem } from '@/types/api';

/** 首页固定页签 */
const HOME_TAB: TabItem = { name: "home", title: "首页" };
const TABS_STORAGE_KEY = 'tabslist';
const ACTIVE_KEY_STORAGE_KEY = 'activeKey';

/** 启动时从 sessionStorage 恢复页签；数据损坏时回退为仅首页 */
const loadTabs = (): TabItem[] => {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(TABS_STORAGE_KEY) || '[]') as TabItem[];
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : [HOME_TAB];
  } catch {
    return [HOME_TAB];
  }
};

export const useTabsStore = defineStore('tabs', () => {
  /**
   * 页签数据的唯一真相（single source of truth）：
   * 组件不再维护本地镜像副本，只读取本状态、只通过 action 变更；
   * watch 负责把变更单向持久化到 sessionStorage。
   */
  const tabsList = ref<TabItem[]>(loadTabs());
  const activeKey = ref<string>(sessionStorage.getItem(ACTIVE_KEY_STORAGE_KEY) || '');

  watch(tabsList, (list) => {
    sessionStorage.setItem(TABS_STORAGE_KEY, JSON.stringify(list));
  }, { deep: true });
  watch(activeKey, (key) => {
    sessionStorage.setItem(ACTIVE_KEY_STORAGE_KEY, key);
  });

  const pushTabsList = (tab: TabItem) => {
    if (tabsList.value.some(item => item.name === tab.name)) return;
    tabsList.value.push(tab);
  };
  const setTabsList = (list: TabItem[]) => {
    tabsList.value = list;
  };
  const clearTabsList = () => {
    tabsList.value = [HOME_TAB];
  };
  const setActiveKey = (key: string) => {
    activeKey.value = key;
  };

  return { tabsList, activeKey, pushTabsList, setTabsList, clearTabsList, setActiveKey };
})
