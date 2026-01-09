<template>
  <a-sub-menu :key="menuInfo.name">
    <template #icon>
      <component :is="menuInfo.meta.icon" v-if="menuInfo.meta.icon" />
    </template>
    <template #title>{{ menuInfo.meta.title }}</template>
    <template v-for="item in menuInfo.children" :key="item.name">
      <template v-if="item.children === null">
        <a-menu-item :key="item.name" @click="menuClick(item.name)">
          <template #icon>
            <component :is="item.meta.icon" v-if="item.meta.icon" />
          </template>
          <router-link :to="{ name: item.name }">{{ item.meta.title }}</router-link>
        </a-menu-item>
      </template>
      <template v-else>
        <SubMenu :menuInfo="item" :key="item.name" />
      </template>
    </template>
  </a-sub-menu>
</template>

<script setup lang="ts">
import { useTabsStore } from '@/stores';
const tabStore = useTabsStore();

const props = defineProps({
  menuInfo: {
    type: Object,
    default: {},
  }
})

const menuClick = (activeKey: string) => {
  tabStore.setActiveKey(activeKey);
}
</script>