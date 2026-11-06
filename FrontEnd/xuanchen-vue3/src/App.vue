<template>
  <a-config-provider :locale="zhCN" :theme="rootTheme">
    <RouterView />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { theme } from 'ant-design-vue';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

/**
 * 全站兜底：携带文件的拖拽一律阻止浏览器默认行为。
 * 不拦截时，用户误把图片/文件拖进窗口（尤其在各类编辑器画布里），
 * 整个标签页会被浏览器导航到该文件——应用 UI 全部消失，看起来像"页面变成了上传的那张图，
 * 没有退出/保存按钮"，只能靠浏览器后退找回。只 preventDefault，不阻断传播，
 * 各页面自己的 drop 处理（如流程图编辑器的拖图入画布）不受影响。
 */
const preventFileNavigation = (e: DragEvent) => {
  if (Array.from(e.dataTransfer?.types ?? []).includes('Files')) {
    e.preventDefault();
  }
};

onMounted(() => {
  window.addEventListener('dragover', preventFileNavigation);
  window.addEventListener('drop', preventFileNavigation);
});

onBeforeUnmount(() => {
  window.removeEventListener('dragover', preventFileNavigation);
  window.removeEventListener('drop', preventFileNavigation);
});

/**
 * 全站唯一的主题 Provider，按路由动态切换算法（官方支持的动态主题模式）。
 * 🔴 不要在业务页面里再嵌套带不同 algorithm 的 ConfigProvider：
 * antd-vue 4.x 局部主题 Provider 卸载后会污染全局主题上下文，
 * 导致之后挂载的页面整套按暗色 token 渲染（大屏设计器返回列表页变黑的根因）。
 * 需要暗色页面的路由在此登记即可。
 */
const DARK_THEME_ROUTES = ['/visualization/designer', '/visualization/viewer'];

const route = useRoute();
const rootTheme = computed(() => {
  const path = route.path;
  const isDark = DARK_THEME_ROUTES.some((p) => path === p || path.startsWith(`${p}/`));
  return isDark
    ? { algorithm: theme.darkAlgorithm, token: { colorPrimary: '#40a9ff', borderRadius: 4 } }
    : {};
});
</script>
