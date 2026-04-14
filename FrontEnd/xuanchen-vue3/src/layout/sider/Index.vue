<template>
  <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible>
    <div class="logo">
      <h2 v-if="show">萱晨管理系统</h2>
      <h2 v-else>萱晨</h2>
    </div>
    <Menu />
  </a-layout-sider>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import Menu from './modules/Menu.vue';
import { useCollapsedStore } from '@/stores';

const collapsedStore = useCollapsedStore();
const collapsed = computed(() => collapsedStore.getCollapsed);
const show = ref(true);

watch(() => collapsed.value, (collapsed: boolean) => {
  if (collapsed) {
    show.value = !collapsed;
  } else {
    setTimeout(() => {
      show.value = !collapsed;
    }, 200);
  }
});
</script>

<style scoped>
.logo {
  height: 32px;
  margin: 16px;
  overflow: hidden;
}

.logo h2 {
  color: #fff;
  text-align: center;
  height: 32px;
  line-height: 32px;
  margin-bottom: 0px;
}
</style>