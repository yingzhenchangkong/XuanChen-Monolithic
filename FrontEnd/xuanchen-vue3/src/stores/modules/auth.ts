import { computed, shallowRef } from 'vue';
import { defineStore } from 'pinia';
import type { UserInfo } from '@/types/api';
import { useTabsStore } from './tabs';

/** 从 sessionStorage 读取用户信息，损坏数据按未登录处理 */
const readStoredUserInfo = (): UserInfo | null => {
  const raw = sessionStorage.getItem('userInfo');
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserInfo;
  } catch {
    // 本地存储被外部写坏时不能让 JSON.parse 异常冒泡白屏，按未登录处理
    return null;
  }
};

export const useAuthStore = defineStore('auth', () => {
  // 响应式会话状态：初始值取自 sessionStorage，之后由各 action 同步维护。
  // shallowRef：整体替换即可触发依赖更新，无需对用户对象做深层代理
  const token = shallowRef<string | null>(sessionStorage.getItem('token'));
  const userInfo = shallowRef<UserInfo | null>(readStoredUserInfo());

  const getToken = (): string | null => token.value;
  const setToken = (newToken: string) => {
    token.value = newToken;
    sessionStorage.setItem('token', newToken);
  };
  const removeToken = () => {
    token.value = null;
    sessionStorage.removeItem('token');
  };

  const getUserInfo = (): UserInfo | null => userInfo.value;
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info;
    sessionStorage.setItem('userInfo', JSON.stringify(info));
  };
  const removeUserInfo = () => {
    userInfo.value = null;
    sessionStorage.removeItem('userInfo');
  };

  /** 是否已登录（响应式，可在模板/computed/watch 中直接感知变化） */
  const isLoggedIn = computed(() => !!token.value);

  /**
   * 清除会话凭据（token + userInfo）的响应式状态与持久化。
   * 同时清除 tabs 等 UI 缓存，确保登出/会话过期后不残留旧账号数据。
   */
  const clearAuth = () => {
    removeToken();
    removeUserInfo();
    sessionStorage.removeItem('tabslist');
    sessionStorage.removeItem('activeKey');
    // 同步复位页签 store 内存状态：仅清 sessionStorage 不够，
    // SPA 不刷新时 Pinia 仍持有旧列表，下次登录会直接沿用
    const tabsStore = useTabsStore();
    tabsStore.clearTabsList();
    tabsStore.setActiveKey('');
  };

  return {
    token,
    userInfo,
    isLoggedIn,
    getToken,
    setToken,
    removeToken,
    getUserInfo,
    setUserInfo,
    removeUserInfo,
    clearAuth,
  };
});
