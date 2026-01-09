<template>
  <a-layout class="layout">
    <Sider />
    <a-layout>
      <Header />
      <Content />
    </a-layout>
  </a-layout>
</template>

<script lang="ts" setup>
import Sider from "./sider/Index.vue";
import Header from "./header/Index.vue";
import Content from "./content/Index.vue";
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
</style>