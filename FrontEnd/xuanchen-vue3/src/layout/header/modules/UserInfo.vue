<template>
  <a-dropdown :trigger="['click']">
    <a-avatar class="avatar" :src="getImageView(avatarPath)">
      <template #icon>
        <UserOutlined />
      </template>
    </a-avatar>
    <template #overlay>
      <a-menu>
        <a-menu-item key="userCenter">
          <router-link :to="{ name: 'userCenter' }">用户中心</router-link>
        </a-menu-item>
        <a-menu-item key="changePassword" @click="changePassword">
          修改密码
        </a-menu-item>
        <a-menu-item key="clearCache" @click="clearCache">
          清除缓存
        </a-menu-item>
        <a-menu-item key="logout" @click="logout">
          退出登录
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
  <ChangePassword ref="refChangePassword" />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import router from '@/router';
import { message } from 'ant-design-vue';
import { postAction } from '@/utils/httpAction';
import { useMenuStore, useAuthStore, useWebSocketStore, useTabsStore } from '@/stores';
import { getImageView } from '@/utils/ImageUtil';
import ChangePassword from './ChangePassword.vue';

// 响应式取值：用户中心更新头像（setUserInfo 整体替换）后这里自动刷新，不能 setup 取一次快照
const authStore = useAuthStore();
const tabsStore = useTabsStore();
const avatarPath = computed(() => authStore.getUserInfo()?.avatar);

const url = {
  logout: '/logout',
}

const refChangePassword = ref();
const changePassword = () => {
  refChangePassword.value.show();
};

const clearCache = () => {
  // 只清本地 UI 缓存、不退出登录：token/userInfo 保留，WebSocket 不动。
  // 清空已打开页签与激活标记、菜单内存状态后置为“未加载”，随后整页重载——
  // 路由守卫会重新拉取授权菜单，各页面组件重建并重新请求数据
  tabsStore.clearTabsList();
  tabsStore.setActiveKey('');
  useMenuStore().setMenuList(undefined);
  message.success('清除缓存成功，页面即将刷新');
  setTimeout(() => window.location.reload(), 600);
};

const logout = async () => {
  try {
    const res = await postAction(url.logout);
    if (res.code === 200) {
      message.success('退出登录成功!');
    }
  } catch (error) {
    message.error('退出登录失败:' + error);
  } finally {
    useWebSocketStore().closeConnection();
    // 同步清空响应式会话状态（token/userInfo ref），否则守卫仍会读到内存中的旧凭据
    authStore.clearAuth();
    // 页签 store 的内存状态也要复位，否则 SPA 不刷新直接换账号登录会残留上一用户的页签
    tabsStore.clearTabsList();
    tabsStore.setActiveKey('');
    window.sessionStorage.clear();
    // 先导航到静态登录页，再移除动态路由：在当前页面仍激活时 removeRoute 会让
    // RouterView 瞬间解析到空匹配并抛出 "No match for" 错误
    await router.push('/login');
    // 重置为"未加载"语义（undefined）而非空数组：空数组会被路由守卫误判为"菜单已加载"，
    // 导致下次登录不再拉取授权菜单；移除动态路由可避免浏览器后退访问旧会话页面
    useMenuStore().setMenuList(undefined);
    if (router.hasRoute('parent')) {
      router.removeRoute('parent');
    }
  }
};
</script>

<style scoped>
.avatar {
  float: right;
  margin-right: 20px;
  margin-top: 16px;
  cursor: pointer;
}
</style>