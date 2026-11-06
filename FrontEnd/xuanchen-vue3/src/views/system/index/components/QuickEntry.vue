<template>
  <a-card :bordered="false" title="快捷入口">
    <a-empty v-if="!entries.length" description="暂无可用菜单" :image-style="{ height: '48px' }" />
    <div v-else class="entry-grid">
      <div v-for="entry in entries" :key="entry.path" class="entry-item" @click="handleGo(entry.path)">
        <component :is="entry.icon" v-if="entry.icon" class="entry-icon" />
        <AppstoreOutlined v-else class="entry-icon" />
        <div class="entry-title" :title="entry.title">{{ entry.title }}</div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useMenuStore } from '@/stores/modules/menu';
import { resolveMenuIcon } from '@/utils/menuIcons';
import type { MenuRecord } from '@/types/api';

const MAX_ENTRIES = 8;

const router = useRouter();
const menuStore = useMenuStore();

interface QuickEntry {
  path: string;
  title: string;
  icon: ReturnType<typeof resolveMenuIcon>;
}

/** 展开菜单树，取有组件的叶子节点（即真实可跳转页面） */
const flattenLeaves = (menus: MenuRecord[], out: MenuRecord[]) => {
  for (const menu of menus) {
    if (menu.children?.length) {
      flattenLeaves(menu.children, out);
    } else if (menu.component && menu.path) {
      out.push(menu);
    }
  }
};

const entries = computed<QuickEntry[]>(() => {
  const list = menuStore.getMenuList() ?? [];
  const leaves: MenuRecord[] = [];
  flattenLeaves(list, leaves);
  return leaves.slice(0, MAX_ENTRIES).map((menu) => ({
    path: menu.path,
    title: menu.meta?.title || menu.name || menu.path,
    icon: resolveMenuIcon(menu.meta?.icon),
  }));
});

const handleGo = (path: string) => {
  router.push(path);
};
</script>

<style lang="scss" scoped>
.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(22, 119, 255, 0.06);

    .entry-icon {
      color: #1677ff;
    }
  }
}

.entry-icon {
  font-size: 24px;
  color: rgba(0, 0, 0, 0.65);
  transition: color 0.2s;
}

.entry-title {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  text-align: center;
}
</style>
