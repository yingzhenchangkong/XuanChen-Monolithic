<template>
  <!-- eventKey 必须显式传 name：antd 对只给了 :key 的 a-sub-menu 会内部生成
       `sub_menu_<n>_$$_<key>` 形式的 eventKey，导致 openKeys 里存的是随机前缀串，
       既无法按菜单名计算祖先链，也让 store 里按 name 记录的 openKeys 匹配不上 -->
  <a-sub-menu :key="menuInfo.name" :event-key="menuInfo.name">
    <template #icon>
      <component :is="resolveMenuIcon(menuInfo.meta.icon)" v-if="menuInfo.meta.icon" />
    </template>
    <template #title>{{ menuInfo.meta.title }}</template>
    <template v-for="item in menuInfo.children" :key="item.name">
      <!-- children 为空数组也要按叶子渲染：空分组点开是空白，等于死按钮 -->
      <template v-if="item.children && item.children.length > 0">
        <SubMenu :menuInfo="item" :key="item.name" />
      </template>
      <template v-else>
        <a-menu-item :key="item.name" @click="menuClick(item.name)">
          <template #icon>
            <component :is="resolveMenuIcon(item.meta.icon)" v-if="item.meta.icon" />
          </template>
          {{ item.meta.title }}
        </a-menu-item>
      </template>
    </template>
  </a-sub-menu>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useTabsStore } from '@/stores';
import { resolveMenuIcon } from '@/utils/menuIcons';
const router = useRouter();
const tabStore = useTabsStore();

const props = defineProps({
  menuInfo: {
    type: Object,
    default: {},
  }
})

const menuClick = (activeKey: string) => {
  tabStore.setActiveKey(activeKey);
  router.push({ name: activeKey });
}
</script>
