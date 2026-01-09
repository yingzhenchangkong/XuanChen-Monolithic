<template>
  <a-dropdown :trigger="['click']">
    <a-avatar class="avatar" :src="getImageView(avatarPath)">
      <template #icon>
        <UserOutlined />
      </template>
    </a-avatar>
    <template #overlay>
      <a-menu>
        <a-menu-item>
          <router-link :to="{ name: 'userCenter' }">用户中心</router-link>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="changePassword">修改密码</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="clearCache">清除缓存</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="logout">退出登录</a>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
  <ChangePassword ref="refChangePassword" />
</template>

<script setup lang="ts">
import { getImageView } from '@/utils/ImageUtil';
import router from '@/router';
import { useMenuStore, useAuthStore } from '@/stores';
import { getAction } from '@/utils/httpAction';
import ChangePassword from './ChangePassword.vue';
import { ref } from 'vue';

const avatarPath = useAuthStore().getUserInfo().avatar;

const url = {
  logout: '/logout',
}

const refChangePassword = ref();
const changePassword = () => {
  refChangePassword.value.show();
};

const clearCache = () => {

};

const logout = async () => {
  await getAction(url.logout, {});
  useMenuStore().setMenuList([]);
  window.sessionStorage.clear();
  router.push('/login');
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