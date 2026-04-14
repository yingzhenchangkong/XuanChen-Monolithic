<template>
  <a-menu v-model:selectedKeys="state.selectedKeys" :openKeys="state.openKeys" mode="inline" theme="dark"
    @openChange="onOpenChange">
    <a-menu-item key="home">
      <template #icon>
        <HomeOutlined />
      </template>
      <router-link :to="{ name: 'home' }">首页</router-link>
    </a-menu-item>
    <template v-for="item in menuList" :key="item.name">
      <template v-if="item.children === null">
        <a-menu-item :key="item.name">
          <template #icon>
            <component :is="item.meta.icon" v-if="item.meta.icon" />
          </template>
          <router-link :to="{ name: item.name }">{{ item.meta.title }}</router-link>
        </a-menu-item>
      </template>
      <template v-else>
        <SubMenu :menuInfo="item" />
      </template>
    </template>
  </a-menu>
</template>

<script setup lang="ts">
import { useMenuStore } from '@/stores';
import SubMenu from './SubMenu.vue';
const menuStore = useMenuStore();
const menuList = menuStore.getMenuList();
const state = menuStore.state;

const onOpenChange = (openKeys: string[]) => {
  if (openKeys.length !== 0) {
    state.openKeys = [openKeys[1]];
  } else {
    state.openKeys = [''];
  }
}
</script>