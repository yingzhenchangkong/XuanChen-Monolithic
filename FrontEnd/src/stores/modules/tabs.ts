import { defineStore } from 'pinia';

export const useTabsStore = defineStore('tabs', () => {
  const getTabsList = () => {
    return JSON.parse(sessionStorage.getItem('tabslist') as string || '[]');
  };
  const pushTabsList = (tab: any) => {
    let tabsList = getTabsList() as any[];
    if (tabsList.some(item => item.name === tab.name)) return;
    sessionStorage.setItem('tabslist', JSON.stringify([...tabsList, tab]));
  };
  const setTabsList = (tabsList: any) => {
    sessionStorage.setItem('tabslist', JSON.stringify(tabsList));
  }
  const clearTabsList = () => {
    sessionStorage.setItem('tabslist', JSON.stringify([{ name: "home", title: "首页" }]));
  };
  const setActiveKey = (activeKey: string) => {
    sessionStorage.setItem('activeKey', activeKey);
  };
  const getActiveKey = () => {
    return sessionStorage.getItem('activeKey') || '';
  };
  return { getTabsList, pushTabsList, setTabsList, clearTabsList, setActiveKey, getActiveKey };
})