<template>
  <div class="office-editor-page">
    <!-- 顶栏：返回 / 文件名 / 刷新 -->
    <div class="editor-topbar">
      <a-button type="text" @click="goBack">
        <template #icon><ArrowLeftOutlined /></template>
        返回
      </a-button>
      <span class="doc-title">{{ docTitle || '协同编辑' }}</span>
      <a-button type="text" :loading="booting" @click="boot">
        <template #icon><ReloadOutlined /></template>
        重新加载
      </a-button>
    </div>

    <!-- 编辑器容器（DS 要求占位元素存在后初始化） -->
    <div class="editor-body">
      <a-spin v-if="booting" class="boot-spin" size="large" tip="正在加载协同编辑器…" />
      <div v-show="!booting && !loadError" id="office-editor-placeholder"></div>
      <div v-if="loadError" class="error-panel">
        <a-result status="warning" title="协同编辑器加载失败" :sub-title="loadError">
          <template #extra>
            <a-button type="primary" @click="boot">重试</a-button>
          </template>
        </a-result>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { getOfficeConfigApi } from './filemanage.api';

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

const route = useRoute();
const router = useRouter();

const docTitle = ref('');
const booting = ref(false);
const loadError = ref('');
let editorInstance: DocsApiEditor | null = null;
let loadedScriptSrc = '';

const fileId = String(route.params.id ?? '');

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
      reject(new Error(`无法连接 Document Server（${src}）。请确认 ONLYOFFICE Docs 服务已启动，且地址配置正确。`));
    document.head.appendChild(script);
  });
}

async function boot() {
  if (!fileId) {
    loadError.value = '缺少文件参数';
    return;
  }
  booting.value = true;
  loadError.value = '';
  try {
    const res = await getOfficeConfigApi(fileId);
    if (res.code !== 200 || !res.data) {
      message.error(res.msg || '获取编辑配置失败');
      loadError.value = res.msg || '获取编辑配置失败';
      return;
    }
    const { dsUrl, config } = res.data;
    docTitle.value = String((config.document as Record<string, unknown> | undefined)?.title ?? '');
    await loadScript(`${dsUrl}/web-apps/apps/api/documents/api.js`);
    if (!window.DocsAPI) {
      throw new Error('api.js 已加载但 DocsAPI 未就绪');
    }
    editorInstance?.destroyEditor();
    editorInstance = new window.DocsAPI.DocEditor('office-editor-placeholder', config);
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '未知错误';
  } finally {
    booting.value = false;
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/filemanage/my');
  }
};

onMounted(boot);

onBeforeUnmount(() => {
  editorInstance?.destroyEditor();
  editorInstance = null;
});
</script>

<style scoped>
.office-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
}

.editor-topbar {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.doc-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-body {
  position: relative;
  flex: 1;
  min-height: 0;
}

.editor-body > div[id='office-editor-placeholder'] {
  width: 100%;
  height: 100%;
}

.boot-spin {
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.error-panel {
  padding-top: 80px;
}
</style>
