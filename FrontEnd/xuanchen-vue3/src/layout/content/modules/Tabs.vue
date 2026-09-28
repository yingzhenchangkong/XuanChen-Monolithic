<template>
  <a-tabs v-model:activeKey="activeKey" hide-add type="editable-card" @tabClick="tabClick" @edit="onEdit" class="atabs">
    <a-tab-pane v-for="tabPane in tabsList" :key="tabPane.name" :closable="tabPane.title !== '首页'">
      <template #tab>
        <a-dropdown :trigger="['contextmenu']">
          <span>{{ tabPane.title }}</span>
          <template #overlay>
            <a-menu @click="({ key }: { key: string }) => handleContextMenu(key, tabPane)">
              <a-menu-item key="closeLeft">关闭左侧</a-menu-item>
              <a-menu-item key="closeRight">关闭右侧</a-menu-item>
              <a-menu-item key="closeOther">关闭其他</a-menu-item>
              <a-menu-item key="closeAll">关闭全部</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
    </a-tab-pane>
    <template #rightExtra>
      <a @click="refresh">
        <ReloadOutlined />
      </a>
      <a-divider type="vertical" />
      <a @click="closeAll" style="padding-right: 10px;">
        <CloseOutlined />
      </a>
    </template>
  </a-tabs>
</template>

<script lang="ts" setup>
import { useTabsStore, useMenuStore } from '@/stores';
import { storeToRefs } from 'pinia';
import { watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { TabItem } from '@/types/api';

const emit = defineEmits(['updateValue']);
const tabStore = useTabsStore();
const route = useRoute();
const router = useRouter();
// 页签列表与激活项直接引用 store 中的响应式状态，组件内不再保存镜像副本
const { tabsList, activeKey } = storeToRefs(tabStore);
const state = useMenuStore().state;

const setActiveTab = () => {
  activeKey.value = route.name as string;
  state.selectedKeys = [route.name as string];
  state.openKeys = route.matched && route.matched[1] ? [route.matched[1].name as string] : [];
};
const pushTabList = () => {
  const { name, meta } = route;
  tabStore.pushTabsList({
    name: name as string,
    title: meta.title as string,
  });
};
const tabClick = (key: string) => {
  router.push({ name: key });
  activeKey.value = key;
};
const onEdit = (targetKey: string) => {
  remove(targetKey);
};
const remove = (targetKey: string) => {
  let lastIndex = 0;
  tabsList.value.forEach((pane, i) => {
    if (pane.name === targetKey) {
      lastIndex = i - 1;
    }
  });
  tabStore.setTabsList(tabsList.value.filter(pane => pane.name !== targetKey));
  if (tabsList.value.length && activeKey.value === targetKey) {
    if (lastIndex >= 0) {
      activeKey.value = tabsList.value[lastIndex].name;
    } else {
      activeKey.value = tabsList.value[0].name;
    }
  }
  router.push({ name: activeKey.value });
};
const refresh = () => {
  emit('updateValue', new Date().getTime())
};
const closeAll = () => {
  tabStore.clearTabsList();
  activeKey.value = 'home';
  router.push({ name: 'home' });
};

const handleContextMenu = (key: string, tabPane: TabItem) => {
  const currentIndex = tabsList.value.findIndex(pane => pane.name === tabPane.name);
  const homeIndex = tabsList.value.findIndex(pane => pane.name === 'home');
  switch (key) {
    case 'closeLeft':
      if (currentIndex > 0) {
        // 保留首页 + 当前标签页及其右侧所有标签。
        // 用名称集合过滤原数组而不是拼接切片：首页若本就在右侧切片内，
        // 简单 prepend 会产生重复标签
        const keepNames = new Set<string>(['home']);
        tabsList.value.slice(currentIndex).forEach(pane => keepNames.add(pane.name));
        tabStore.setTabsList(tabsList.value.filter(pane => keepNames.has(pane.name)));
      }
      break;
    case 'closeRight':
      if (currentIndex < tabsList.value.length - 1) {
        // 保留从首页到当前标签页；首页若位于右侧被关闭区间，则一并保留并固定在最前。
        // 注意：切片后数组变短，不能再用旧下标 homeIndex 去新数组取值，
        // 否则 push 进去的是 undefined（标签栏出现空白/死项）
        const homeTab = tabsList.value[homeIndex];
        const kept = tabsList.value.slice(0, currentIndex + 1);
        tabStore.setTabsList(homeIndex > currentIndex
          ? [homeTab, ...kept.filter(pane => pane.name !== 'home')]
          : kept);
      }
      break;
    case 'closeOther':
      // 保留首页和当前标签页
      tabStore.setTabsList(tabPane.name === 'home'
        ? [tabsList.value[homeIndex]]
        : [tabsList.value[homeIndex], tabsList.value[currentIndex]]);
      break;
    case 'closeAll':
      closeAll();
      return;
  }
  // 确保当前标签页保持激活状态（closeAll 已自行跳转首页，不再覆盖）
  activeKey.value = tabPane.name;
};

onMounted(() => {
  // 只做标签状态初始化：当前路由已由路由守卫（含动态路由注入后的重导航）正确解析，
  // 绝不能再按缓存的 activeKey 主动 push——否则 F5 刷新任何内页都会被劫持到首页/上次标签
  setActiveTab();
  pushTabList();
});
watch(() => route.name, () => {
  setActiveTab();
  pushTabList();
});
</script>

<style scoped>
.atabs :deep(.ant-tabs-nav) {
  height: 38px;
  margin: 0;
}

.atabs :deep(.ant-tabs-nav .ant-tabs-tab) {
  background-color: transparent;
  padding: 2px 4px;
  border: none;
  margin: 0px 15px;
}

.atabs :deep(.ant-tabs-nav .ant-tabs-tab-active) {
  background-color: transparent;
  padding: 2px 4px;
  border: none;
  border-bottom: 2px solid #1890ff;
  margin: 0px 15px;
}
</style>
