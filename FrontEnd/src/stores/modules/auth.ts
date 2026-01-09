import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', () => {
  const getToken = () => {
    return sessionStorage.getItem('token');
  }
  const setToken = (token: string) => {
    sessionStorage.setItem('token', token);
  }
  const removeToken = () => {
    sessionStorage.removeItem('token');
  }
  const getUserInfo = () => {
    const userInfo = sessionStorage.getItem('userInfo');
    return userInfo ? JSON.parse(userInfo) : null;
  }
  const setUserInfo = (userInfo: any) => {
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
  }
  const removeUserInfo = () => {
    sessionStorage.removeItem('userInfo');
  }
  return { getToken, setToken, removeToken, getUserInfo, setUserInfo, removeUserInfo };
})