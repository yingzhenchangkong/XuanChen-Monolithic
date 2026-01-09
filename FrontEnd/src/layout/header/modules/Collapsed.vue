<template>
  <menu-unfold-outlined v-if="collapsed" class="trigger" @click="collapseClick" />
  <menu-fold-outlined v-else class="trigger" @click="collapseClick" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useCollapsedStore } from '@/stores';

const collapsedStore = useCollapsedStore();
const collapsed = computed(() => collapsedStore.getCollapsed);
const collapseClick = () => {
    collapsedStore.setCollapsed();
};
</script>

<style scoped>
.trigger {
    font-size: 18px;
    line-height: 64px;
    padding: 0 24px;
    cursor: pointer;
    transition: color 0.3s;
    float: left;
    margin-top: 20px;
}

.trigger:hover {
    color: #1890ff;
}
</style>