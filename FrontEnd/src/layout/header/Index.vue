<template>
  <a-layout-header class="header">
    <Collapsed />
    <UserInfo />
    <Notice />
  </a-layout-header>
</template>

<script lang="ts" setup>
import Collapsed from './modules/Collapsed.vue';
import UserInfo from './modules/UserInfo.vue';
import Notice from './modules/Notice.vue';
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0;
}
</style>