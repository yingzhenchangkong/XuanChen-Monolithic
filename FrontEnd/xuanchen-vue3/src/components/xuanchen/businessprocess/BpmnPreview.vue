<template>
  <div class="bpm-bpmn-preview-wrap">
    <div ref="viewerRef" class="bpm-bpmn-preview"></div>
    <div v-if="error" class="bpm-bpmn-preview-error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import Viewer from 'bpmn-js/lib/Viewer';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

const props = defineProps<{
  bpmnXml?: string;
  /** 高亮节点 key 集合（当前停留） */
  currentKeys?: string[];
  /** 已走过的节点 key 集合 */
  passedKeys?: string[];
}>();

const viewerRef = ref<HTMLDivElement>();
const error = ref('');
let viewer: any = null;

const HIGHLIGHT_CURRENT = 'xc-highlight-current';
const HIGHLIGHT_PASSED = 'xc-highlight-passed';

const render = async () => {
  if (!viewerRef.value) return;
  error.value = '';
  const xml = props.bpmnXml?.trim();
  if (!xml) {
    error.value = '暂无流程图';
    return;
  }
  try {
    if (!viewer) {
      viewer = new Viewer({ container: viewerRef.value });
    }
    await viewer.importXML(xml);
    viewer.get('canvas').zoom('fit-viewport', 'auto');
    const canvas = viewer.get('canvas');
    (props.passedKeys ?? []).forEach((key) => {
      if (key) canvas.addMarker(key, HIGHLIGHT_PASSED);
    });
    (props.currentKeys ?? []).forEach((key) => {
      if (key) canvas.addMarker(key, HIGHLIGHT_CURRENT);
    });
  } catch (e: any) {
    error.value = '流程图渲染失败';
  }
};

onMounted(() => {
  nextTick(render);
});

onBeforeUnmount(() => {
  viewer?.destroy?.();
  viewer = null;
});

watch(() => [props.bpmnXml, props.currentKeys, props.passedKeys], () => {
  nextTick(render);
});
</script>

<style>
.bpm-bpmn-preview-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 220px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.bpm-bpmn-preview {
  width: 100%;
  height: 100%;
}

.bpm-bpmn-preview-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 13px;
}

/* 节点高亮：已走路径（描边变蓝）与当前停留（描边变绿 + 填充） */
.bpm-bpmn-preview .djs-element .xc-highlight-passed:not(.djs-connection) rect,
.bpm-bpmn-preview .djs-element .xc-highlight-passed:not(.djs-connection) circle,
.bpm-bpmn-preview .djs-element .xc-highlight-passed:not(.djs-connection) polygon {
  stroke: #1890ff;
  stroke-width: 2px;
}

.bpm-bpmn-preview .djs-element .xc-highlight-passed.djs-connection > path {
  stroke: #1890ff;
  stroke-width: 2px;
}

.bpm-bpmn-preview .djs-element .xc-highlight-current:not(.djs-connection) rect,
.bpm-bpmn-preview .djs-element .xc-highlight-current:not(.djs-connection) circle,
.bpm-bpmn-preview .djs-element .xc-highlight-current:not(.djs-connection) polygon {
  stroke: #52c41a;
  stroke-width: 3px;
  fill: rgba(82, 196, 26, 0.12);
}
</style>
