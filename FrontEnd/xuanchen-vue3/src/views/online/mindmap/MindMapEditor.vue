<template>
  <div class="mindmap-editor">
    <!-- 顶部工具条 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <a-button size="small" @click="handleBack">
          <template #icon>
            <ArrowLeftOutlined />
          </template>返回
        </a-button>
        <a-input v-model:value="docName" size="small" class="doc-name-input" :maxlength="100" />
      </div>
      <div class="toolbar-center">
        <a-space :size="4">
          <a-tooltip title="插入子节点（Tab）">
            <a-button size="small" @click="exec('INSERT_CHILD_NODE')">子节点</a-button>
          </a-tooltip>
          <a-tooltip title="插入同级节点（Enter）">
            <a-button size="small" @click="exec('INSERT_NODE')">同级</a-button>
          </a-tooltip>
          <a-tooltip title="删除节点（Delete）">
            <a-button size="small" danger @click="exec('REMOVE_NODE')">删除节点</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-tooltip title="扩大字号">
            <a-button size="small" @click="changeFontSize(2)">A+</a-button>
          </a-tooltip>
          <a-tooltip title="缩小字号">
            <a-button size="small" @click="changeFontSize(-2)">A-</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-tooltip title="居中显示">
            <a-button size="small" @click="centerView">定位</a-button>
          </a-tooltip>
          <a-tooltip title="缩放复位">
            <a-button size="small" @click="resetView">1:1</a-button>
          </a-tooltip>
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-space :size="4">
          <a-dropdown>
            <a-button size="small">
              <template #icon>
                <DownloadOutlined />
              </template>导出
            </a-button>
            <template #overlay>
              <a-menu @click="handleExport">
                <a-menu-item key="png">PNG 图片</a-menu-item>
                <a-menu-item key="svg">SVG 矢量</a-menu-item>
                <a-menu-item key="json">JSON 备份</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button size="small" type="primary" :loading="saving" @click="handleSave">
            <template #icon>
              <SaveOutlined />
            </template>保存
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 画布容器 -->
    <div class="canvas-wrap">
      <div ref="canvasRef" class="canvas"></div>
      <a-tag class="hotkey-tip" color="default">
        右键节点=快捷菜单 · Tab=子节点 Enter=同级 Delete=删除 · 滚轮=缩放 · 按住拖拽=平移 · 双击节点编辑文字
      </a-tag>
    </div>

    <!-- 节点右键菜单（fixed 定位，Teleport 到 body 防画布 transform 干扰） -->
    <Teleport to="body">
      <div v-if="ctxMenu.visible" class="ctx-mask" @mousedown="closeCtxMenu" @contextmenu.prevent="closeCtxMenu">
        <div
          class="ctx-menu"
          :style="{ left: `${ctxMenu.x}px`, top: `${ctxMenu.y}px` }"
          @mousedown.stop
          @contextmenu.stop
        >
          <div class="ctx-item" @click="ctxExec('INSERT_CHILD_NODE')">插入子节点</div>
          <template v-if="!ctxMenu.isRoot">
            <div class="ctx-item" @click="ctxExec('INSERT_NODE')">插入同级节点</div>
            <div class="ctx-divider"></div>
            <div class="ctx-item danger" @click="ctxExec('REMOVE_NODE')">删除节点</div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, DownloadOutlined, SaveOutlined } from '@ant-design/icons-vue';
import MindMap from 'simple-mind-map';
import ExportPlugin from 'simple-mind-map/src/plugins/Export.js';
import type { MindMapFullSnapshot, MindMapNode, MindMapNodeData } from 'simple-mind-map';

import { getMindmapDetailApi, saveMindmapApi } from './mindmap.api';

// 精简入口 index.js 默认不注册任何插件。未注册时 export() 内部抛「请注册Export插件！」，
// 且异常被库吞掉只返回 undefined，会下载出一个内容为 "undefined" 的假图片。
MindMap.usePlugin(ExportPlugin);

const route = useRoute();
const router = useRouter();

const docId = route.params.id as string;
const docName = ref('');
const canvasRef = ref<HTMLDivElement>();
const saving = ref(false);

let mindMap: MindMap | null = null;
/** 离开页面前未保存的变更标记（目前仅做提示位，MVP 手动保存） */
let dirty = false;

onMounted(async () => {
  if (!canvasRef.value) return;
  //加载详情
  let snapshotData: MindMapNodeData | undefined;
  let snapshotLayout: string | undefined;
  let snapshotTheme: string | undefined;
  let snapshotThemeConfig: Record<string, unknown> | undefined;
  try {
    const res = await getMindmapDetailApi(docId);
    if (res.code !== 200 || !res.data) {
      message.error(res.msg || '文档加载失败');
      router.replace('/onlinedoc/mindmap');
      return;
    }
    docName.value = res.data.docName;
    if (res.data.snapshot) {
      try {
        const parsed = JSON.parse(res.data.snapshot) as
          | MindMapNodeData
          | MindMapFullSnapshot;
        //兼容两种快照格式：
        //1. 节点树（当前保存格式）：{data:{text}, children:[]}
        //2. 旧版 getData(true) 全量快照：{layout, root, theme:{template,config}, view}
        if (
          parsed &&
          !('data' in parsed) &&
          'root' in parsed &&
          (parsed as MindMapFullSnapshot).root?.data
        ) {
          const full = parsed as MindMapFullSnapshot;
          snapshotData = full.root;
          snapshotLayout = full.layout;
          snapshotTheme = full.theme?.template;
          snapshotThemeConfig = full.theme?.config;
        } else {
          snapshotData = parsed as MindMapNodeData;
        }
      } catch {
        message.warning('快照数据解析失败，已初始化为空导图');
      }
    }
  } catch {
    message.error('文档加载失败');
    router.replace('/onlinedoc/mindmap');
    return;
  }

  mindMap = new MindMap({
    el: canvasRef.value,
    data: snapshotData ?? {
      data: { text: docName.value || '中心主题' },
      children: [],
    },
    layout: snapshotLayout ?? 'logicalStructure',
    theme: snapshotTheme ?? 'default',
    ...(snapshotThemeConfig ? { themeConfig: snapshotThemeConfig } : {}),
    //滚轮缩放（库默认 'move' 只平移画布；按住画布拖拽仍可平移）
    mousewheelAction: 'zoom',
    //初始展开与只读由数据驱动；编辑能力全开
    readonly: false,
  });
  mindMap.on('data_change', () => {
    dirty = true;
  });
  mindMap.on('node_contextmenu', onNodeContextmenu);
});

onBeforeUnmount(() => {
  mindMap?.destroy();
  mindMap = null;
});

/** 节点命令（依赖当前选中节点，未选中时库内部自行处理根节点） */
const exec = (cmd: string) => {
  mindMap?.execCommand(cmd);
};

// ---------- 节点右键菜单 ----------
const CTX_MENU_W = 140;
const CTX_MENU_H = 118;
const ctxMenu = reactive({ visible: false, x: 0, y: 0, isRoot: false });
/** 右键命中的节点实例 */
let ctxNode: MindMapNode | null = null;

const onNodeContextmenu = (e: unknown, node: MindMapNode) => {
  const ev = e as MouseEvent;
  ev.preventDefault();
  ctxNode = node;
  ctxMenu.isRoot = !!node.isRoot;
  //防止菜单溢出视口边缘
  ctxMenu.x = Math.min(ev.clientX, window.innerWidth - CTX_MENU_W - 8);
  ctxMenu.y = Math.min(ev.clientY, window.innerHeight - CTX_MENU_H - 8);
  ctxMenu.visible = true;
};

const closeCtxMenu = () => {
  ctxMenu.visible = false;
  ctxNode = null;
};

/** 右键菜单命令：显式指定目标节点，不依赖选中态（openEdit=true 让新节点直接进入文字编辑） */
const ctxExec = (cmd: 'INSERT_CHILD_NODE' | 'INSERT_NODE' | 'REMOVE_NODE') => {
  if (!mindMap || !ctxNode) return;
  if (cmd === 'REMOVE_NODE') {
    mindMap.execCommand(cmd, [ctxNode]);
  } else {
    mindMap.execCommand(cmd, true, [ctxNode]);
  }
  closeCtxMenu();
};

const changeFontSize = (delta: number) => {
  const active = mindMap?.renderer?.activeNodeList ?? [];
  if (!active.length) {
    message.warning('请先选中节点');
    return;
  }
  active.forEach((node) => {
    const current = node.getData<number>('fontSize') ?? 16;
    //库没有 SET_NODE_FONT_SIZE 命令，统一走 SET_NODE_STYLE
    mindMap?.execCommand('SET_NODE_STYLE', node, 'fontSize', Math.max(10, Math.min(64, current + delta)));
  });
};

/** 居中显示：根节点移到画布中心（保持当前缩放比例） */
const centerView = () => {
  mindMap?.renderer.setRootNodeCenter();
};

/** 缩放复位：先恢复 1:1 与原始偏移，再居中，保证内容回到视野 */
const resetView = () => {
  if (!mindMap) return;
  mindMap.view.reset();
  mindMap.renderer.setRootNodeCenter();
};

const handleSave = async () => {
  if (!mindMap) return;
  saving.value = true;
  try {
    //只存节点树（getData() 不带配置）；带配置的 getData(true) 返回 {layout, root, theme, view} 包装结构，
    //直接落库后加载时会被当成节点解析成"空根节点"
    const snapshot = JSON.stringify(mindMap.getData());
    const res = await saveMindmapApi(docId, docName.value.trim() || '未命名导图', snapshot);
    if (res.code === 200) {
      dirty = false;
      message.success('保存成功！');
    } else {
      message.error(res.msg || '保存失败');
    }
  } finally {
    saving.value = false;
  }
};

/** 导出：PNG/SVG 直接下载；JSON 为数据备份 */
const handleExport = async ({ key }: { key: string }) => {
  if (!mindMap) return;
  const name = docName.value.trim() || '思维导图';
  if (key === 'json') {
    const blob = new Blob([JSON.stringify(mindMap.getData(), null, 2)], {
      type: 'application/json;charset=utf-8',
    });
    triggerDownload(URL.createObjectURL(blob), `${name}.json`);
    return;
  }
  try {
    //第二参是 isDownload：传 true 会让库内部先自动下载一次同名文件（造成重复下载）。
    //png/svg 的返回值是 data:URL 字符串（不是 Blob），不能再包一层 Blob，
    //否则落盘内容变成 "data:image/png;base64,..." 文本，图片预览必然失败。
    const dataUrl = await mindMap.export(key as 'png' | 'svg', false, name);
    if (typeof dataUrl !== 'string' || !dataUrl.startsWith('data:')) {
      //export() 内部异常会被库吞掉（只回调 errorHandler），此处必须显式拦截
      message.error('导出失败：未获取到有效图片数据，请查看控制台日志');
      return;
    }
    triggerDownload(URL.createObjectURL(dataUrlToBlob(dataUrl)), `${name}.${key}`);
  } catch (error) {
    console.error('[mindmap] 导出失败', error);
    message.error('导出失败，请重试');
  }
};

/** data:URL → 带正确 MIME 的 Blob，保证落盘是真正的 PNG/SVG 二进制而非 base64 文本 */
const dataUrlToBlob = (dataUrl: string): Blob => {
  const comma = dataUrl.indexOf(',');
  const meta = dataUrl.slice(0, comma);
  const payload = dataUrl.slice(comma + 1);
  const semicolon = meta.indexOf(';');
  const mime = meta.slice(5, semicolon === -1 ? undefined : semicolon) || 'application/octet-stream';
  if (!/;base64/i.test(meta)) {
    return new Blob([decodeURIComponent(payload)], { type: mime });
  }
  const binary = atob(payload);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new Blob([bytes], { type: mime });
};

const triggerDownload = (url: string, filename: string) => {
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  if (url.startsWith('blob:')) {
    //点击后立即 revoke 在部分浏览器会中断下载，延后释放
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
};

const handleBack = () => {
  router.push('/onlinedoc/mindmap');
};
</script>

<style scoped>
.mindmap-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--ant-color-bg-layout, #f5f5f5);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  background: var(--ant-color-bg-container, #fff);
  border-bottom: 1px solid var(--ant-color-border-secondary, #f0f0f0);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.toolbar-right {
  justify-content: flex-end;
}

.toolbar-center {
  display: flex;
  align-items: center;
}

.doc-name-input {
  width: 260px;
}

.canvas-wrap {
  position: relative;
  flex: 1;
  overflow: hidden;
}

.canvas {
  width: 100%;
  height: 100%;
}

.hotkey-tip {
  position: absolute;
  right: 16px;
  bottom: 12px;
  opacity: 0.75;
  pointer-events: none;
}

/* 节点右键菜单（Teleport 到 body，元素仍带 scoped 属性） */
.ctx-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.ctx-menu {
  position: fixed;
  min-width: 132px;
  padding: 4px;
  background: var(--ant-color-bg-container, #fff);
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.ctx-item {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
  cursor: pointer;
  white-space: nowrap;
}

.ctx-item:hover {
  background: var(--ant-color-bg-text-hover, rgba(0, 0, 0, 0.06));
}

.ctx-item.danger {
  color: var(--ant-color-error, #ff4d4f);
}

.ctx-divider {
  height: 1px;
  margin: 4px 8px;
  background: var(--ant-color-border-secondary, #f0f0f0);
}
</style>
