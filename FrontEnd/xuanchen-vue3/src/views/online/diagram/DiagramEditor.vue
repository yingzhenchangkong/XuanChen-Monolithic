<template>
  <div class="diagram-editor">
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
          <a-tooltip title="撤销（Ctrl+Z）">
            <a-button size="small" @click="graph?.undo()">撤销</a-button>
          </a-tooltip>
          <a-tooltip title="重做（Ctrl+Shift+Z）">
            <a-button size="small" @click="graph?.redo()">重做</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-tooltip title="删除选中（Delete）">
            <a-button size="small" danger @click="removeSelected">删除选中</a-button>
          </a-tooltip>
          <a-tooltip title="编辑选中节点/连线文字">
            <a-button size="small" @click="editSelectedLabel">编辑文字</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-tooltip title="放大">
            <a-button size="small" @click="zoom(0.1)">＋</a-button>
          </a-tooltip>
          <a-tooltip title="缩小">
            <a-button size="small" @click="zoom(-0.1)">－</a-button>
          </a-tooltip>
          <a-tooltip title="缩放复位">
            <a-button size="small" @click="zoomReset">1:1</a-button>
          </a-tooltip>
          <a-tooltip title="内容充满画布">
            <a-button size="small" @click="zoomToFit">适应</a-button>
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

    <!-- 主体：左侧图形面板 + 画布 -->
    <div class="body">
      <div ref="stencilRef" class="stencil"></div>
      <div class="canvas-wrap">
        <div ref="canvasRef" class="canvas"></div>
        <a-tag class="hotkey-tip" color="default">
          拖拽左侧图形到画布 · 拖动节点圆点连线 · 滚轮缩放 · 拖动空白平移 · Delete删除 · 双击编辑文字 · 支持拖入/粘贴图片
        </a-tag>
      </div>
    </div>

    <!-- 文字编辑弹窗 -->
    <a-modal v-model:open="labelOpen" title="编辑文字" @ok="confirmLabel">
      <a-textarea v-model:value="labelText" placeholder="输入节点/连线文字" :rows="3" :maxlength="200" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, DownloadOutlined, SaveOutlined } from '@ant-design/icons-vue';
import { Cell, Edge, Graph, Node } from '@antv/x6';
import { useAuthStore } from '@/stores';
import type { Result } from '@/types/api';
import { getImageView } from '@/utils/ImageUtil';
import { Export } from '@antv/x6-plugin-export';
import { History } from '@antv/x6-plugin-history';
import { Keyboard } from '@antv/x6-plugin-keyboard';
import { Selection } from '@antv/x6-plugin-selection';
import { Snapline } from '@antv/x6-plugin-snapline';
import { Stencil } from '@antv/x6-plugin-stencil';

import { getDiagramDetailApi, saveDiagramApi } from './diagram.api';

const route = useRoute();
const router = useRouter();

const docId = route.params.id as string;
const docName = ref('');
const canvasRef = ref<HTMLDivElement>();
const stencilRef = ref<HTMLDivElement>();
const saving = ref(false);

let graph: Graph | null = null;
let stencil: Stencil | null = null;
/** 未保存变更标记（MVP 手动保存） */
let dirty = false;
/** 正在编辑文字的单元格 */
let editingCell: Cell | null = null;

const labelOpen = ref(false);
const labelText = ref('');

// ---------------- 自定义图形注册 ----------------
/** 四向连线端口配置（hover 节点时显示） */
const PORT_CIRCLE = { r: 4, magnet: true, stroke: '#5F95FF', strokeWidth: 1, fill: '#fff', style: { visibility: 'hidden' } };
const PORTS = {
  groups: {
    top: { position: 'top' as const, attrs: { circle: { ...PORT_CIRCLE } } },
    right: { position: 'right' as const, attrs: { circle: { ...PORT_CIRCLE } } },
    bottom: { position: 'bottom' as const, attrs: { circle: { ...PORT_CIRCLE } } },
    left: { position: 'left' as const, attrs: { circle: { ...PORT_CIRCLE } } },
  },
  items: [{ group: 'top' }, { group: 'right' }, { group: 'bottom' }, { group: 'left' }],
};

function registerShapes() {
  // X6 的 shape 注册表是模块级全局单例，重复注册同名 shape 会直接抛错
  // （"Node with name 'flow-start' already registered"），导致同一次登录会话内
  // 第二次进入编辑器时整个初始化中断（画布无网格、左侧面板空白、文档名不回填）。
  // 已注册过就跳过；HMR 场景 X6 内部已豁免，无需特殊处理。
  if (Node.registry.exist('flow-start')) return;
  const common = { ports: PORTS, zIndex: 1 };
  //开始
  Graph.registerNode('flow-start', {
    inherit: 'ellipse',
    width: 70, height: 40,
    attrs: {
      body: { fill: '#EFF8FF', stroke: '#5F95FF', strokeWidth: 1.2 },
      label: { text: '开始', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //结束
  Graph.registerNode('flow-end', {
    inherit: 'ellipse',
    width: 70, height: 40,
    attrs: {
      body: { fill: '#FFF1F0', stroke: '#F5222D', strokeWidth: 1.2 },
      label: { text: '结束', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //流程（圆角矩形）
  Graph.registerNode('flow-process', {
    inherit: 'rect',
    width: 100, height: 44,
    attrs: {
      body: { rx: 8, ry: 8, fill: '#F0F5FF', stroke: '#2F54EB', strokeWidth: 1.2 },
      label: { text: '流程', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //判断（菱形）
  Graph.registerNode('flow-decision', {
    inherit: 'polygon',
    width: 100, height: 60,
    attrs: {
      body: { refPoints: '0,10 10,0 20,10 10,20', fill: '#FFF7E6', stroke: '#FA8C16', strokeWidth: 1.2 },
      label: { text: '判断', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //数据（平行四边形）
  Graph.registerNode('flow-data', {
    inherit: 'polygon',
    width: 100, height: 44,
    attrs: {
      body: { refPoints: '4,0 20,0 16,10 0,10', fill: '#F9F0FF', stroke: '#722ED1', strokeWidth: 1.2 },
      label: { text: '数据', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //准备（六边形）
  Graph.registerNode('flow-prepare', {
    inherit: 'polygon',
    width: 110, height: 50,
    attrs: {
      body: { refPoints: '3,0 17,0 20,5 17,10 3,10 0,5', fill: '#E6FFFB', stroke: '#13C2C2', strokeWidth: 1.2 },
      label: { text: '准备', fontSize: 12, fill: '#333' },
    },
    ...common,
  });
  //通用盒子（架构图形，圆角灰蓝）
  Graph.registerNode('arch-box', {
    inherit: 'rect',
    width: 120, height: 50,
    attrs: {
      body: { rx: 4, ry: 4, fill: '#F5F7FA', stroke: '#4E5969', strokeWidth: 1.2 },
      label: { text: '模块', fontSize: 12, fill: '#1D2129' },
    },
    ...common,
  });
  //文本标注
  Graph.registerNode('flow-text', {
    inherit: 'rect',
    width: 80, height: 30,
    attrs: {
      body: { fill: 'transparent', stroke: 'transparent' },
      label: { text: '文本说明', fontSize: 12, fill: '#666' },
    },
    ports: PORTS,
    zIndex: 1,
  });
  //默认连线
  Graph.registerEdge('flow-edge', {
    inherit: 'edge',
    attrs: {
      line: {
        stroke: '#A2B1C6',
        strokeWidth: 1.5,
        targetMarker: { name: 'block', width: 10, height: 6 },
      },
    },
    label: {
      attrs: {
        labelBody: { fill: '#fff', stroke: '#D8DCE6', strokeWidth: 1, rx: 3, ry: 3 },
        labelText: { fill: '#4E5969', fontSize: 11 },
      },
    },
    zIndex: 0,
  });
}

/** 节点/连线文字读写：节点存 attrs.label/text，连线存 labels[0].attrs.labelText/text */
function getCellText(cell: Cell): string {
  if (cell.isEdge()) {
    const label = (cell as Edge).getLabelAt?.(0) as { attrs?: { labelText?: { text?: string } } } | null;
    return label?.attrs?.labelText?.text ?? '';
  }
  return ((cell as Node).attr('label/text') as string) ?? '';
}

function setCellText(cell: Cell, text: string) {
  if (cell.isEdge()) {
    const edge = cell as Edge;
    if (text) {
      edge.setLabels([
        {
          attrs: {
            labelBody: { fill: '#fff', stroke: '#D8DCE6', strokeWidth: 1, rx: 3, ry: 3 },
            labelText: { fill: '#4E5969', fontSize: 11, text },
          },
        },
      ]);
    } else {
      edge.setLabels([]);
    }
  } else {
    (cell as Node).attr('label/text', text);
  }
}

/** 面板节点（静态实例，供 stencil 加载） */

// ---------------- 生命周期 ----------------
// ---------------- 图片拖入 / 粘贴 ----------------
const IMAGE_MIME_RE = /^image\//;

/** 该拖拽是否携带文件（types 含 Files） */
const dragHasFiles = (e: DragEvent): boolean =>
  Array.from(e.dataTransfer?.types ?? []).includes('Files');

/**
 * 上传图片到文件中心，成功返回可直接访问的完整 URL，失败返回 null。
 * 响应契约与 XCUploadImage 一致：统一 Result，相对路径在 data（历史实现落在 msg，两者都兜住）。
 */
const uploadImage = async (file: File): Promise<string | null> => {
  const formData = new FormData();
  formData.append('file', file, file.name);
  formData.append('customPath', 'diagram');
  try {
    const token = useAuthStore().getToken();
    const resp = await fetch(import.meta.env.APP_BASE_URL + import.meta.env.APP_FILE_UPLOAD_PATH, {
      method: 'POST',
      headers: token ? { 'XC-ACCESS-TOKEN': token } : undefined,
      body: formData,
    });
    const res = (await resp.json()) as Result<string | null>;
    if (res.code === 200) {
      const url = getImageView((res.data ?? res.msg) as string);
      if (url) return url;
    }
    message.error(res.msg || '图片上传失败');
  } catch {
    message.error('图片上传失败，请检查网络或重新登录');
  }
  return null;
};

/** 在画布插入图片节点：保持原始宽高比缩放到 240x180 以内；默认落点为画布中心 */
const insertImageNode = (url: string, clientX?: number, clientY?: number) => {
  if (!graph) return;
  const img = new Image();
  img.onload = () => {
    const naturalW = img.width || 240;
    const naturalH = img.height || 180;
    const scale = Math.min(1, 240 / naturalW, 180 / naturalH);
    const w = Math.max(40, Math.round(naturalW * scale));
    const h = Math.max(30, Math.round(naturalH * scale));
    let cx = clientX;
    let cy = clientY;
    if (cx == null || cy == null) {
      const rect = canvasRef.value?.getBoundingClientRect();
      cx = rect ? rect.left + rect.width / 2 : window.innerWidth / 2;
      cy = rect ? rect.top + rect.height / 2 : window.innerHeight / 2;
    }
    const local = graph!.clientToLocal(cx!, cy!);
    graph!.addNode({
      shape: 'image',
      x: local.x - w / 2,
      y: local.y - h / 2,
      width: w,
      height: h,
      imageUrl: url,
      ports: PORTS,
      zIndex: 1,
    });
    message.success('图片已插入画布，记得保存');
  };
  img.onerror = () => message.error('图片加载失败');
  img.src = url;
};

const onDocumentDragOver = (e: DragEvent) => {
  // 必须阻止默认行为，否则浏览器不会派发 drop 事件；App.vue 的全站兜底已处理导航劫持
  if (dragHasFiles(e)) {
    e.preventDefault();
  }
};

const onDocumentDrop = (e: DragEvent) => {
  if (!dragHasFiles(e)) return;
  // 关键兜底：一旦这里不阻止默认行为，整个标签页会被浏览器导航到被拖入的图片，
  // 表现为"编辑器消失、只显示这张图片、没有退出/保存按钮"，只能靠浏览器后退找回
  e.preventDefault();
  const files = Array.from(e.dataTransfer?.files ?? []);
  const images = files.filter((f) => IMAGE_MIME_RE.test(f.type));
  if (!images.length) {
    message.warning('仅支持拖入图片文件（jpg/png 等）');
    return;
  }
  images.forEach(async (file, index) => {
    const url = await uploadImage(file);
    if (url) {
      // 多张时依次错开，避免完全重叠
      insertImageNode(url, e.clientX + index * 24, e.clientY + index * 24);
    }
  });
};

const onDocumentPaste = (e: ClipboardEvent) => {
  const item = Array.from(e.clipboardData?.items ?? []).find((it) => IMAGE_MIME_RE.test(it.type));
  const file = item?.getAsFile();
  if (!file) return;
  e.preventDefault();
  void (async () => {
    const url = await uploadImage(file);
    if (url) insertImageNode(url);
  })();
};

onMounted(async () => {
  registerShapes();

  //加载详情
  let snapshotData: Record<string, unknown> | undefined;
  try {
    const res = await getDiagramDetailApi(docId);
    if (res.code !== 200 || !res.data) {
      message.error(res.msg || '文档加载失败');
      router.replace('/onlinedoc/diagram');
      return;
    }
    docName.value = res.data.docName;
    if (res.data.snapshot) {
      try {
        snapshotData = JSON.parse(res.data.snapshot) as Record<string, unknown>;
      } catch {
        message.warning('快照数据解析失败，已初始化为空画布');
      }
    }
  } catch {
    message.error('文档加载失败');
    router.replace('/onlinedoc/diagram');
    return;
  }

  if (!canvasRef.value) return;
  graph = new Graph({
    container: canvasRef.value,
    grid: { size: 10, visible: true },
    background: { color: '#fafafa' },
    //滚轮缩放；空白处拖拽平移
    mousewheel: { enabled: true, minScale: 0.4, maxScale: 2.5 },
    panning: { enabled: true, eventTypes: ['leftMouseDown', 'rightMouseDown'] },
    connecting: {
      router: 'orth',
      connector: { name: 'rounded', args: { radius: 8 } },
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      allowLoop: false,
      allowNode: false,
      allowEdge: false,
      snap: { radius: 24 },
      highlight: true,
      createEdge: () => graph!.createEdge({ shape: 'flow-edge', attrs: { line: { stroke: '#A2B1C6', strokeWidth: 1.5, targetMarker: { name: 'block', width: 10, height: 6 } } } }),
    },
    highlighting: {
      magnetAdsorbed: {
        name: 'stroke',
        args: { attrs: { fill: '#5F95FF', stroke: '#5F95FF' } },
      },
    },
  });
  graph
    .use(new Snapline())
    .use(new History({ enabled: true }))
    .use(new Keyboard({ enabled: true }))
    .use(new Selection({ enabled: true, multiple: true, rubberband: false, showEdgeSelectionBox: true }))
    .use(new Export());

  //Delete 删除选中
  graph.bindKey(['delete', 'backspace'], () => {
    removeSelected();
  });
  //Ctrl+Z / Ctrl+Shift+Z
  graph.bindKey('ctrl+z', () => graph?.undo());
  graph.bindKey('ctrl+shift+z', () => graph?.redo());

  //脏标记
  const markDirty = () => { dirty = true; };
  graph.on('cell:added', markDirty);
  graph.on('cell:removed', markDirty);
  graph.on('node:moved', markDirty);
  graph.on('edge:connected', markDirty);
  graph.on('history:change', markDirty);

  //双击编辑文字
  graph.on('cell:dblclick', ({ cell }: { cell: Cell }) => {
    editingCell = cell;
    labelText.value = getCellText(cell);
    labelOpen.value = true;
  });

  //加载快照
  if (snapshotData) {
    graph.fromJSON(snapshotData);
  }

  initStencil();

  //初始充满可见
  setTimeout(() => {
    graph?.centerContent();
  }, 100);

  //图片拖入/粘贴监听（document 级：拖到页面任意位置都生效，且防止浏览器接管页面）
  document.addEventListener('dragover', onDocumentDragOver);
  document.addEventListener('drop', onDocumentDrop);
  document.addEventListener('paste', onDocumentPaste);
});

onBeforeUnmount(() => {
  document.removeEventListener('dragover', onDocumentDragOver);
  document.removeEventListener('drop', onDocumentDrop);
  document.removeEventListener('paste', onDocumentPaste);
  graph?.dispose();
  graph = null;
  stencil?.dispose?.();
  stencil = null;
});

// ---------------- 图形面板 ----------------
function initStencil() {
  if (!stencilRef.value || !graph) return;
  stencil = new Stencil({
    title: '流程图图形',
    target: graph,
    search(cell: Cell, keyword: string) {
      return getCellText(cell).includes(keyword);
    },
    collapsable: true,
    stencilGraphWidth: 200,
    groups: [
      { name: 'basic', title: '流程图基础', graphHeight: 160 },
      { name: 'general', title: '通用/架构图形', graphHeight: 150 },
    ],
    layoutOptions: { columns: 2, columnWidth: 96, rowHeight: 64 },
  });
  stencilRef.value.appendChild(stencil.container);

  // 预览尺寸：stencil 网格按 columnWidth/rowHeight 摆放，节点若按注册的天然尺寸
  // （100~120px 宽）渲染会超出单元格互相叠压，这里统一缩小到 96px 宽以内
  const preview = (shape: string, width = 72, height = 40): Node.Metadata => ({ shape, width, height });
  stencil.load(
    [
      preview('flow-start'),
      preview('flow-end'),
      preview('flow-process', 84, 40),
      preview('flow-decision', 88, 54),
    ],
    'basic',
  );
  stencil.load(
    [
      preview('flow-data', 84, 40),
      preview('flow-prepare', 92, 48),
      preview('arch-box', 88, 44),
      preview('flow-text', 76, 28),
    ],
    'general',
  );
}

// ---------------- 操作 ----------------
const removeSelected = () => {
  if (!graph) return;
  const cells = graph.getSelectedCells();
  if (!cells.length) {
    message.warning('请先选中节点或连线');
    return;
  }
  graph.removeCells(cells);
  dirty = true;
};

const editSelectedLabel = () => {
  if (!graph) return;
  const cells = graph.getSelectedCells();
  if (cells.length !== 1) {
    message.warning('请先选中一个节点或连线');
    return;
  }
  editingCell = cells[0];
  labelText.value = getCellText(editingCell);
  labelOpen.value = true;
};

const confirmLabel = () => {
  if (editingCell && graph) {
    setCellText(editingCell, labelText.value ?? '');
    dirty = true;
  }
  labelOpen.value = false;
};

const zoom = (delta: number) => {
  if (!graph) return;
  const factor = delta > 0 ? 1.1 : 0.9;
  const next = Math.min(2.5, Math.max(0.4, graph.zoom() * factor));
  graph.zoomTo(next);
};

const zoomReset = () => {
  graph?.zoomTo(1);
  graph?.centerContent();
};

const zoomToFit = () => {
  graph?.zoomToFit({ padding: 40, maxScale: 1.5 });
};

const handleSave = async () => {
  if (!graph) return;
  saving.value = true;
  try {
    const snapshot = JSON.stringify(graph.toJSON());
    const res = await saveDiagramApi(docId, docName.value.trim() || '未命名流程图', snapshot);
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
  if (!graph) return;
  const name = docName.value.trim() || '流程图';
  if (key === 'json') {
    const blob = new Blob([JSON.stringify(graph.toJSON(), null, 2)], { type: 'application/json' });
    triggerDownload(URL.createObjectURL(blob), `${name}.json`);
    return;
  }
  try {
    if (key === 'png') {
      await graph.toPNG((dataUri: string) => {
        triggerDownload(dataUri, `${name}.png`);
      }, { padding: 20, backgroundColor: '#ffffff', quality: 1 });
    } else {
      await graph.toSVG((dataUri: string) => {
        triggerDownload(dataUri, `${name}.svg`);
      });
    }
  } catch {
    message.error('导出失败，请重试');
  }
};

const triggerDownload = (url: string, filename: string) => {
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  if (url.startsWith('blob:')) {
    URL.revokeObjectURL(url);
  }
};

const handleBack = () => {
  router.push('/onlinedoc/diagram');
};
</script>

<style scoped>
.diagram-editor {
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

.body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.stencil {
  /* X6 stencil 组件是 absolute 铺满定位父级（top/right/bottom/left 全 0），
     父级必须 position:relative，否则它会以整页为包含块铺满全屏、盖住顶部工具条 */
  position: relative;
  width: 210px;
  height: 100%;
  border-right: 1px solid #e5e6eb;
  background: #fff;
  overflow: auto;
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
</style>

<style>
/* X6 画布与面板全局样式（不能 scoped：内层选择器会被丢掉） */
.x6-graph-scroller,
.x6-graph-svg {
  user-select: none;
}

/* 端口默认隐藏，hover 节点时显示 */
.x6-port-body {
  visibility: hidden;
}

.x6-node:hover .x6-port-body,
.x6-node:hover .x6-port {
  visibility: visible !important;
}

/* stencil 面板样式 */
.x6-widget-stencil {
  background: #fff;
  border: none;
}

.x6-widget-stencil-title {
  font-size: 12px;
  color: #4e5969;
  padding: 6px 0;
}

.x6-widget-stencil-group-title {
  font-size: 12px;
  color: #86909c;
}

.x6-widget-stencil-item {
  border-radius: 4px;
}

.x6-widget-stencil-config-search input {
  font-size: 12px;
}
</style>
