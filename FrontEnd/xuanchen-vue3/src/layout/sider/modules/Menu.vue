<template>
  <a-menu v-model:selectedKeys="state.selectedKeys" :openKeys="state.openKeys" mode="inline" theme="dark"
    @openChange="onOpenChange">
    <a-menu-item key="home" @click="navigateTo('home')">
      <template #icon>
        <HomeOutlined />
      </template>
      首页
    </a-menu-item>
    <template v-for="item in menuList" :key="item.name">
      <template v-if="isGroup(item)">
        <SubMenu :menuInfo="item" />
      </template>
      <template v-else>
        <a-menu-item :key="item.name" @click="navigateTo(item.name)">
          <template #icon>
            <component :is="resolveMenuIcon(item.meta?.icon ?? '')" v-if="item.meta?.icon" />
          </template>
          {{ item.meta?.title }}
        </a-menu-item>
      </template>
    </template>
  </a-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useMenuStore, useTabsStore } from '@/stores';
import { resolveMenuIcon } from '@/utils/menuIcons';
import type { MenuRecord } from '@/types/api';
import SubMenu from './SubMenu.vue';
const router = useRouter();
const menuStore = useMenuStore();
const tabStore = useTabsStore();
const menuList = computed(() => menuStore.getMenuList() ?? []);
const state = menuStore.state;

/**
 * 是否作为可展开的分组渲染。
 * children 为 null 或空数组都按叶子处理：空分组点开后是一片空白（相当于死按钮），
 * 后端目录节点即使子菜单全被权限过滤掉也会返回 []，不能只判 null。
 */
const isGroup = (item: MenuRecord) =>
  Array.isArray(item.children) && item.children.length > 0;

const navigateTo = (name?: string) => {
  if (!name) return;
  tabStore.setActiveKey(name);
  router.push({ name });
};

/**
 * 递归查找某个菜单节点的祖先 name 链（根 → 父）。
 * 多级菜单（系统工具 → 代码生成器 → 代码生成）必须保留祖先 key：
 * 只保留最后一项会把刚展开子级所属的父分组一起收起来，表现为「点了没反应」。
 */
const findAncestors = (
  target: string,
  list: MenuRecord[] = menuList.value,
  chain: string[] = [],
): string[] | null => {
  for (const item of list) {
    if (item.name === target) return chain;
    if (isGroup(item)) {
      const found = findAncestors(target, item.children as MenuRecord[], [
        ...chain,
        item.name as string,
      ]);
      if (found) return found;
    }
  }
  return null;
};

/** 手风琴：同一层级只保留最后展开的分支，但祖先链必须保留，才能支持任意层级菜单 */
const onOpenChange = (openKeys: (string | number)[]) => {
  const latest = openKeys[openKeys.length - 1];
  if (latest === undefined || latest === '') {
    state.openKeys = [];
    return;
  }
  const key = String(latest);
  state.openKeys = [...(findAncestors(key) ?? []), key];
};
</script>
