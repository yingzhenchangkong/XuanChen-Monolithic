<template>
  <a-layout class="layout">
    <Sider />
    <a-layout>
      <Header />
      <Content />
    </a-layout>
  </a-layout>
  <!-- 全局 AI 助手：悬浮球全页面可见（组件由 unplugin-vue-components 自动引入） -->
  <XCAiAssistant />
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