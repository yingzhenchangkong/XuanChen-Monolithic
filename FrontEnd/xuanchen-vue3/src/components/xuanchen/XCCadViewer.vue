<template>
  <div class="xc-cad-viewer">
    <a-spin v-if="loading" class="cad-loading" tip="正在加载 CAD 查看器组件（首次较慢）" />
    <a-result v-else-if="error" status="warning" :title="error" />
    <component :is="viewerComp" v-else-if="viewerComp" :url="url" locale="zh" theme="light"
      :background="VIEWER_BACKGROUND" class="cad-canvas" @create="onViewerCreate" />
  </div>
</template>

<script setup lang="ts">
/**
 * CAD 图纸预览封装（DWG / DXF）。
 *
 * 基于 @mlightcad/cad-viewer（MIT，浏览器端解析渲染，无后端依赖）：
 * - DXF 用内置 MIT 解析器；DWG 走 @mlightcad/libredwg-converter（LibreDWG WASM，
 *   GPL——按官方设计隔离在独立 Web Worker bundle 里，与主应用代码分离）
 * - worker / wasm 静态资源由 scripts/copy-cad-workers.mjs 拷到 public/workers
 *
 * 整个 cad-viewer（含 three.js、element-plus）体积很大，全部走动态 import 懒加载，
 * 只在第一次打开 CAD 预览时才拉取，不进首屏 chunk。Element Plus / vue-i18n 是
 * cad-viewer 的宿主要求，安装为插件后其内部组件才能解析（与 antd 的 a-* 前缀无冲突）。
 *
 * ⚠️ 单例时序（踩过的坑）：AcApDocManager 是单例，MlCadViewer 挂载时会携带真实容器
 * 调 createInstance；若我们先擅自 createInstance（只为传 worker 地址），单例已存在，
 * 容器参数被忽略，视图会绑到默认游离 canvas（300x150、不在文档里）→ 画布 0 尺寸白屏。
 * 解决：包裹 createInstance，把 worker 地址合并进 MlCadViewer 传来的参数（它携带容器）。
 * 另：文档打开过程会重置视图背景色，需在 documentCreated 后重放 background prop。
 */
import { getCurrentInstance, onMounted, ref, shallowRef, type App } from 'vue';
import { message } from 'ant-design-vue';

defineProps<{ url: string }>();

const VIEWER_BACKGROUND = 0xffffff;
const loading = ref(true);
const error = ref('');
const viewerComp = shallowRef<unknown>(null);

const workerBase = `${import.meta.env.BASE_URL}workers/`.replace('//workers', '/workers');
const CAD_WORKER_URLS = {
  dwgParser: `${workerBase}libredwg-parser-worker.js`,
  mtextRender: `${workerBase}mtext-renderer-worker.js`,
};

/** 跨实例只初始化一次（插件安装 / DWG 转换器注册都是全局单例操作） */
let initPromise: Promise<void> | null = null;
/** @create 回调里要用，initCadRuntime 加载后暂存 */
let simpleViewerMod: typeof import('@mlightcad/cad-simple-viewer') | null = null;

/** MlCadViewer 完成挂载（单例已带真实容器创建）后重放背景色（打开过程会重置它） */
function onViewerCreate() {
  const dm = simpleViewerMod?.AcApDocManager.tryGetInstance();
  if (!dm) return;
  const applyBackground = () => {
    if (dm.curView && dm.curView.backgroundColor !== VIEWER_BACKGROUND) {
      dm.curView.backgroundColor = VIEWER_BACKGROUND;
    }
  };
  //背景色在文档打开各阶段会被反复重置（documentCreated 时设置的值会被后续
  //转换/取景覆盖），因此事件 + 多级延时兜底，直到打开完成后的最后一次生效
  dm.events.documentCreated.addEventListener(applyBackground);
  dm.events.documentActivated.addEventListener(applyBackground);
  [200, 800, 2000, 4000].forEach((t) => window.setTimeout(applyBackground, t));
}

async function initCadRuntime(app: App): Promise<void> {
  const [cadViewer, elementPlus, dataModel, libredwg, simpleViewer] = await Promise.all([
    import('@mlightcad/cad-viewer'),
    import('element-plus'),
    import('@mlightcad/data-model'),
    import('@mlightcad/libredwg-converter'),
    import('@mlightcad/cad-simple-viewer'),
  ]);
  simpleViewerMod = simpleViewer;
  //cad-viewer.js 自带 `import './cad-viewer.css'`（其 css 已由 copy 脚本就地修补 v-bind 占位）
  await import('element-plus/dist/index.css');
  //注册 DWG → 数据库 转换器（必须在打开 DWG 前完成；本函数整页只跑一次）
  const { AcDbDatabaseConverterManager, AcDbFileType } = dataModel;
  const { AcDbLibreDwgConverter } = libredwg;
  AcDbDatabaseConverterManager.instance.register(
    AcDbFileType.DWG,
    new AcDbLibreDwgConverter({
      useWorker: true,
      parserWorkerUrl: CAD_WORKER_URLS.dwgParser,
    }),
  );
  //包裹 createInstance：单例必须由 MlCadViewer 携带真实容器创建（见文件头「单例时序」），
  //这里只把 worker 地址合并进它传来的参数，不做任何提前创建
  const docManagerClass = simpleViewer.AcApDocManager as unknown as {
    createInstance: (opts?: Record<string, unknown>) => unknown;
  };
  const origCreate = docManagerClass.createInstance.bind(docManagerClass);
  docManagerClass.createInstance = (opts = {}) =>
    origCreate({ ...opts, webworkerFileUrls: CAD_WORKER_URLS });
  //安装 cad-viewer 依赖的宿主插件（element-plus / vue-i18n），与 antd 的 a-* 前缀无冲突。
  //app 必须由调用方在生命周期钩子同步段抓取后传入——本函数内部全是 await，
  //此时 getCurrentInstance() 已返回 null，插件会被静默跳过（vue-i18n 随即报
  //「Need to install with `app.use` function」）。
  app.use(elementPlus.default ?? elementPlus);
  if (cadViewer.i18n) app.use(cadViewer.i18n);
}

onMounted(async () => {
  //必须在首个 await 之前同步抓取根 app 实例（见 initCadRuntime 注释）
  const app = getCurrentInstance()?.appContext.app;
  try {
    if (!initPromise) {
      if (!app) throw new Error('无法获取应用实例');
      initPromise = initCadRuntime(app);
    }
    await initPromise;
    const { MlCadViewer } = await import('@mlightcad/cad-viewer');
    viewerComp.value = MlCadViewer;
  } catch (e) {
    console.error('[XCCadViewer] CAD 查看器初始化失败', e);
    error.value = 'CAD 查看器组件加载失败，请检查网络后重试';
    message.error(error.value);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.xc-cad-viewer {
  width: 100%;
  height: 100%;
  position: relative;
  background: #fff;
  /* cad-viewer 内部（.ml-cad-viewer-container、状态栏、浮动对话框）全是 position:fixed
     全页设计，嵌在弹窗里会逃逸到视口。CSS 规定 transform 非 none 即成为 fixed
     后代的包含块，一行即可把整个查看器（含浮动对话框）约束在本容器内 */
  transform: translateZ(0);
  overflow: hidden;
}

.cad-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.cad-canvas {
  width: 100%;
  height: 100%;
}
</style>
