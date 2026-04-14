<template>
  <Tabs @updateValue="handleUpdate" />
  <a-layout-content :style="{ padding: '15px', }" :key="receivedValue">
    <RouterView />
  </a-layout-content>
</template>

<script lang="ts" setup>
import Tabs from './modules/Tabs.vue';
import { ref } from 'vue';

const receivedValue = ref('');

const handleUpdate = (value: string) => {
  receivedValue.value = value;
}
</script>