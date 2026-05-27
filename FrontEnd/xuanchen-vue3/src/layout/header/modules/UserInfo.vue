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
import { ref } from 'vue';
import router from '@/router';
import { message } from 'ant-design-vue';
import { postAction } from '@/utils/httpAction';
import { useMenuStore, useAuthStore, useWebSocketStore } from '@/stores';
import { getImageView } from '@/utils/ImageUtil';
import ChangePassword from './ChangePassword.vue';

const avatarPath = useAuthStore().getUserInfo().avatar;

const url = {
  logout: '/logout',
}

const refChangePassword = ref();
const changePassword = () => {
  refChangePassword.value.show();
};

const clearCache = () => {
  message.success('清除缓存成功');
};

const logout = async () => {
  try {
    const res: any = await postAction(url.logout, {});
    if (res.code === 200) {
      message.success('退出登录成功!');
    }
  } catch (error) {
    message.error('退出登录失败:' + error);
  } finally {
    useWebSocketStore().closeConnection();
    useMenuStore().setMenuList([]);
    window.sessionStorage.clear();
    router.push('/login');
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