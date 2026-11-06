<template>
  <!--
    预览水印罩层：CSS 平铺水印（SVG data-uri 生成斜排文字），pointer-events:none 不挡交互。
    父容器需 position:relative。
  -->
  <div class="xc-watermark" :style="bgStyle" aria-hidden="true"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    /** 水印文字（通常为"用户名 + 时间"） */
    text: string;
    /** 透明度（0~1） */
    opacity?: number;
  }>(),
  { opacity: 0.12 },
);

const escapeXml = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const bgStyle = computed(() => {
  const opacity = Math.min(Math.max(props.opacity, 0.02), 0.5);
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="260" height="150">` +
    `<text x="18" y="86" font-size="13" font-family="sans-serif" ` +
    `fill="rgba(0,0,0,${opacity})" transform="rotate(-18 130 75)">${escapeXml(props.text)}</text></svg>`;
  return {
    backgroundImage: `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}")`,
    backgroundRepeat: 'repeat' as const,
  };
});
</script>

<style scoped>
.xc-watermark {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 20;
}
</style>
