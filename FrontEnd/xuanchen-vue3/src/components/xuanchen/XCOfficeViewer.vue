<template>
  <div class="xc-office-viewer">
    <a-spin v-if="booting" class="office-spin" size="large" tip="正在加载原格式预览…" />
    <a-result v-else-if="loadError" status="warning" title="原格式预览加载失败" :sub-title="loadError">
      <template #extra>
        <a-button type="primary" @click="boot">重试</a-button>
      </template>
    </a-result>
    <!-- DS 要求占位元素先存在再初始化；编辑器 iframe 自带加载动画，构造完成即隐藏 spin -->
    <div v-show="!booting && !loadError" :id="placeholderId" class="office-placeholder"></div>
  </div>
</template>

<script setup lang="ts">
/**
 * Office 原格式预览封装（ONLYOFFICE Document Server 只读模式）。
 *
 * 与 views/filemanage/OfficeEditor.vue（全页协同编辑）共用后端 GET /office/config，
 * 走 mode=view：config 由后端整签（JWT），前端不能改动任何字段后喂给 DocsAPI。
 * 加载失败在组件内展示错误面板并提供重试，不再回退 PDF 转换（该链路已下线）。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { getOfficeConfigApi } from '@/views/filemanage/filemanage.api';

const props = defineProps<{ fileId: string }>();

/** ONLYOFFICE DocsAPI 最小类型声明（api.js 运行时挂 window.DocsAPI） */
interface DocsApiEditor {
  destroyEditor: () => void;
}
interface DocsApiNamespace {
  DocEditor: new (placeholderId: string, config: Record<string, unknown>) => DocsApiEditor;
}
declare global {
  interface Window {
    DocsAPI?: DocsApiNamespace;
  }
}

const booting = ref(true);
const loadError = ref('');
//占位元素 id 带随机后缀，避免与全页编辑器或多个实例撞 id
const placeholderId = `xc-office-viewer-${Math.random().toString(36).slice(2)}`;

let editorInstance: DocsApiEditor | null = null;
let loadedScriptSrc = '';

/** 动态加载 DS 的 api.js（同一地址只加载一次） */
function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.DocsAPI && loadedScriptSrc === src) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = src;
    script.onload = () => {
      loadedScriptSrc = src;
      resolve();
    };
    script.onerror = () =>
      reject(new Error(`无法连接 Document Server（${src}），请确认 ONLYOFFICE 服务已启动`));
    document.head.appendChild(script);
  });
}

/** 销毁旧实例后重新拉配置并挂载（重试按钮入口） */
async function boot() {
  booting.value = true;
  loadError.value = '';
  editorInstance?.destroyEditor();
  editorInstance = null;
  try {
    const res = await getOfficeConfigApi(props.fileId, 'view');
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取预览配置失败');
    }
    const { dsUrl, config } = res.data;
    await loadScript(`${dsUrl}/web-apps/apps/api/documents/api.js`);
    if (!window.DocsAPI) {
      throw new Error('api.js 已加载但 DocsAPI 未就绪');
    }
    editorInstance = new window.DocsAPI.DocEditor(placeholderId, config);
    booting.value = false;
  } catch (e) {
    console.error('[XCOfficeViewer] 原格式预览加载失败', e);
    booting.value = false;
    loadError.value = e instanceof Error ? e.message : '未知错误';
  }
}

onMounted(boot);

onBeforeUnmount(() => {
  editorInstance?.destroyEditor();
  editorInstance = null;
});
</script>

<style scoped>
.xc-office-viewer {
  width: 100%;
  height: 100%;
  position: relative;
  background: #fff;
}

.office-placeholder {
  width: 100%;
  height: 100%;
}

.office-spin {
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
