<template>
  <a-tabs v-model:activeKey="activeKey" hide-add type="editable-card" @tabClick="tabClick" @edit="onEdit" class="atabs">
    <a-tab-pane v-for="tabPane in listTabPane" :key="tabPane.name" :closable="tabPane.title !== '首页'">
      <template #tab>
        <a-dropdown :trigger="['contextmenu']">
          <span>{{ tabPane.title }}</span>
          <template #overlay>
            <a-menu @click="({ key }: { key: string }) => handleContextMenu(key, tabPane)">
              <a-menu-item key="closeLeft">关闭左侧</a-menu-item>
              <a-menu-item key="closeRight">关闭右侧</a-menu-item>
              <a-menu-item key="closeOther">关闭其他</a-menu-item>
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
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const emit = defineEmits(['updateValue']);
const tabStore = useTabsStore();
const route = useRoute();
const router = useRouter();
const activeKey = ref('');
const state = useMenuStore().state;

const setActiveTab = () => {
  activeKey.value = route.name as string;
  state.selectedKeys = [route.name as string];
  state.openKeys = [route.matched[1].name as string];
};
const listTabPane = ref();
const pushTabList = () => {
  const { name, meta } = route;
  const tab: any = {
    name: name,
    title: meta.title as string,
  }
  tabStore.pushTabsList(tab);
  listTabPane.value = tabStore.getTabsList();
};
const tabClick = (key: string) => {
  router.push({ name: key });
  tabStore.setActiveKey(key);
};
const onEdit = (targetKey: string) => {
  remove(targetKey);
};
const remove = (targetKey: string) => {
  let lastIndex = 0;
  listTabPane.value.forEach((pane: any, i: number) => {
    if (pane.name === targetKey) {
      lastIndex = i - 1;
    }
  });
  listTabPane.value = listTabPane.value.filter((pane: any) => pane.name !== targetKey);
  tabStore.setTabsList(listTabPane.value);
  if (listTabPane.value.length && activeKey.value === targetKey) {
    if (lastIndex >= 0) {
      activeKey.value = listTabPane.value[lastIndex].name;
    } else {
      activeKey.value = listTabPane.value[0].name;
    }
  }
  tabStore.setActiveKey(activeKey.value);
  router.push({ name: activeKey.value });
};
const refresh = () => {
  emit('updateValue', new Date().getTime())
};
const closeAll = () => {
  tabStore.clearTabsList();
  listTabPane.value = tabStore.getTabsList();
  tabStore.setActiveKey('home');
  router.push({ name: 'home' });
};

const handleContextMenu = (key: string, tabPane: any) => {
  const currentIndex = listTabPane.value.findIndex((pane: any) => pane.name === tabPane.name);
  const homeIndex = listTabPane.value.findIndex((pane: any) => pane.name === 'home');
  switch (key) {
    case 'closeLeft':
      if (currentIndex > 0) {
        // 获取首页和当前标签页及其之后的所有标签
        const homeTab = listTabPane.value[homeIndex];
        const rightTabs = listTabPane.value.slice(currentIndex);
        // 将首页放在第一位，后面跟上当前标签页及其之后的所有标签
        listTabPane.value = [homeTab, ...rightTabs];
        tabStore.setTabsList(listTabPane.value);
      }
      break;
    case 'closeRight':
      if (currentIndex < listTabPane.value.length - 1) {
        // 保留从开始到当前标签页的所有标签
        listTabPane.value = listTabPane.value.slice(0, currentIndex + 1);
        // 如果首页在右侧，需要保留首页
        if (homeIndex > currentIndex) {
          listTabPane.value.push(listTabPane.value[homeIndex]);
        }
        tabStore.setTabsList(listTabPane.value);
      }
      break;
    case 'closeOther':
      // 保留首页和当前标签页
      listTabPane.value = tabPane.name === 'home'
        ? [listTabPane.value[homeIndex]]
        : [listTabPane.value[homeIndex], listTabPane.value[currentIndex]];
      tabStore.setTabsList(listTabPane.value);
      break;
  }
  // 确保当前标签页保持激活状态
  activeKey.value = tabPane.name;
  tabStore.setActiveKey(activeKey.value);
};

onMounted(() => {
  setActiveTab();
  pushTabList();
  router.push({ name: tabStore.getActiveKey() || 'home' });
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