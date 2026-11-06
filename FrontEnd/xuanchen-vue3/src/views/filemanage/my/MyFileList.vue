<template>
  <div class="my-file-page">
    <!-- 左侧：文件夹树（三期） -->
    <a-card size="small" class="folder-panel" title="文件夹">
      <template #extra>
        <a-tooltip title="新建文件夹">
          <a @click="openCreateFolder(0)"><PlusOutlined /></a>
        </a-tooltip>
      </template>
      <a-spin :spinning="folderLoading">
        <a-tree v-if="folderTreeData.length" :tree-data="folderTreeData" block-node
          :selected-keys="selectedFolderKeys" :expanded-keys="expandedKeys"
          @select="onSelectFolder" @expand="onExpandTree">
          <template #title="{ key, title }">
            <span class="folder-title">
              <span class="folder-name" :title="String(title)">{{ title }}</span>
              <span v-if="key !== '0'" class="folder-ops" @click.stop>
                <a-tooltip title="重命名">
                  <EditOutlined @click="openRenameFolder(String(key), String(title))" />
                </a-tooltip>
                <a-popconfirm title="删除文件夹及其下文件？文件可在回收站恢复" placement="top"
                  @confirm="() => handleDeleteFolder(String(key))">
                  <a-tooltip title="删除">
                    <DeleteOutlined class="danger-icon" />
                  </a-tooltip>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </a-tree>
      </a-spin>
    </a-card>

    <!-- 右侧：文件列表 -->
    <div class="list-panel">
      <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
        :query-params="queryParams" :selection="false" export-title="我的文件">
        <!-- 工具栏：通用文件上传按钮（分片上传：init → 分片 → complete，秒传/断点续传内含） -->
        <template #toolbar-prepend>
          <a-upload :show-upload-list="false" :before-upload="handleBeforeUpload" :custom-request="handleUpload"
            :disabled="uploading">
            <a-button type="primary" :loading="uploading">
              <template #icon>
                <UploadOutlined />
              </template>{{ uploading ? '上传中…' : '上传文件' }}
            </a-button>
          </a-upload>
        </template>

        <!-- 类型/大小/存储列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'ext'">
            <a-tag color="blue">{{ String(record.ext).toUpperCase() }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'fileSize'">
            {{ formatFileSize(record.fileSize as number) }}
          </template>
          <template v-else-if="column.dataIndex === 'storageType'">
            {{ storageTypeText(record.storageType as number) }}
          </template>
        </template>

        <!-- 操作列：预览 / 协同编辑(仅 Office) / 下载 / 共享 / 删除（进回收站） -->
        <template #operation="{ record }">
          <a @click="handlePreview(record as FileInfoRecord)">
            <EyeOutlined /> 预览
          </a>
          <template v-if="isCoEditable(record.ext)">
            <a-divider type="vertical"></a-divider>
            <a @click="handleCoEdit(record as FileInfoRecord)">
              <EditOutlined /> 协同编辑
            </a>
          </template>
          <a-divider type="vertical"></a-divider>
          <a @click="handleDownload(record as FileInfoRecord)">
            <DownloadOutlined /> 下载
          </a>
          <a-divider type="vertical"></a-divider>
          <a @click="handleOpenShare(record as FileInfoRecord)">
            <ShareAltOutlined /> 共享
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="删除后可在回收站恢复，确定删除吗？" placement="left"
            @confirm="() => handleDelete(record.id)">
            <a>
              <DeleteOutlined /> 删除
            </a>
          </a-popconfirm>
        </template>
      </XCListPage>
    </div>
  </div>

  <!-- 上传进度（大文件分片上传） -->
  <a-modal :open="uploading" :closable="false" :mask-closable="false" :keyboard="false" :footer="null"
    :width="420" title="正在上传">
    <div class="upload-file-name">{{ uploadFileName }}</div>
    <a-progress :percent="uploadPercent" status="active" />
    <div class="upload-hint">支持秒传与断点续传，请保持页面开启</div>
  </a-modal>

  <!-- 预览弹窗：带水印罩层（图片 / PDF / Office 转 PDF / 文本），标题栏内置全屏切换 -->
  <XCModal v-model:open="previewVisible" :title="previewName" :width="960" hide-footer destroy-on-close
    default-fullscreen wrap-class-name="preview-modal">
    <div class="preview-body">
      <a-spin v-if="previewLoading" class="preview-spin" />
      <template v-else>
        <img v-if="previewKind === 'image'" :src="previewSrc" class="preview-image" :alt="previewName" />
        <div v-else-if="previewKind === 'text'" class="preview-text-wrap">
          <div class="preview-text-bar">
            <span class="preview-text-label">编码</span>
            <a-select v-model:value="textCharset" size="small" style="width: 150px" :options="charsetOptions"
              @change="reloadTextPreview" />
            <a-tag v-if="textTruncated" color="orange">文件较大，仅显示前 512KB</a-tag>
            <span v-else class="preview-text-meta">{{ previewText.length }} 字符 · {{ detectedCharset }}</span>
          </div>
          <a-spin v-if="textLoading" class="preview-text-loading" />
          <pre v-else class="preview-text">{{ previewText }}</pre>
        </div>
        <div v-else-if="previewKind === 'markdown'" class="preview-text-wrap">
          <div class="preview-text-bar">
            <a-radio-group v-model:value="mdViewMode" size="small" button-style="solid">
              <a-radio-button value="render">渲染</a-radio-button>
              <a-radio-button value="source">源码</a-radio-button>
            </a-radio-group>
            <span class="preview-text-label">编码</span>
            <a-select v-model:value="textCharset" size="small" style="width: 150px" :options="charsetOptions"
              @change="reloadTextPreview" />
            <a-tag v-if="textTruncated" color="orange">文件较大，仅显示前 512KB</a-tag>
            <span v-else class="preview-text-meta">{{ previewText.length }} 字符 · {{ detectedCharset }}</span>
          </div>
          <a-spin v-if="mdRenderLoading" class="preview-text-loading" />
          <pre v-else-if="mdViewMode === 'source'" class="preview-text">{{ previewText }}</pre>
          <!-- mdHtml 由 markdown-it 生成（html:false 转义内嵌 HTML），无用户可控原始 HTML -->
          <div v-else class="preview-markdown" v-html="mdHtml"></div>
        </div>
        <XCCadViewer v-else-if="previewKind === 'cad'" :url="previewSrc" class="preview-cad" />
        <XCOfficeViewer v-else-if="previewKind === 'office'" :file-id="previewFileId" class="preview-office" />
        <iframe v-else :src="previewSrc" class="preview-frame"></iframe>
        <XCWatermark :text="watermarkText" />
      </template>
    </div>
  </XCModal>

  <!-- 新建/重命名文件夹弹窗 -->
  <a-modal v-model:open="folderModalVisible" :title="folderModalMode === 'create' ? '新建文件夹' : '重命名文件夹'"
    :width="420" @ok="handleFolderModalOk" :confirm-loading="folderSubmitting">
    <a-input v-model:value="folderModalName" placeholder="请输入文件夹名" :maxlength="64" allow-clear
      @press-enter="handleFolderModalOk" />
  </a-modal>

  <!-- 共享弹窗：创建新共享 + 我的共享管理（footer 操作，按约定用 a-modal） -->
  <a-modal v-model:open="shareVisible" title="文件共享" :width="680" :footer="null" destroy-on-close>
    <a-form layout="inline" class="share-form" @submit.prevent>
      <a-form-item label="有效期">
        <a-select v-model:value="shareForm.expireHours" style="width: 130px" :options="expireOptions" />
      </a-form-item>
      <a-form-item label="提取码">
        <a-input v-model:value="shareForm.shareCode" style="width: 140px" placeholder="留空则无需提取码"
          :maxlength="8" allow-clear />
      </a-form-item>
      <a-button type="primary" :loading="shareCreating" :disabled="!currentFile"
        @click="handleCreateShare">创建共享</a-button>
    </a-form>
    <div v-if="currentFile" class="share-file">
      <FileTextOutlined /> {{ currentFile.originalName }}（{{ formatFileSize(currentFile.fileSize) }}）
    </div>

    <a-divider orientation="left" plain>我的共享</a-divider>
    <a-table :data-source="shareList" :columns="shareColumns" row-key="id" size="small"
      :pagination="false" :loading="shareLoading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'fileName'">
          <span :title="record.fileName">{{ record.fileName }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'fileSize'">
          {{ formatFileSize(record.fileSize as number) }}
        </template>
        <template v-else-if="column.dataIndex === 'hasCode'">
          <a-tag :color="record.hasCode ? 'orange' : 'green'">{{ record.hasCode ? '需提取码' : '直接下载' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'visitCount'">
          {{ record.visitCount }} 次
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a @click="handleCopyShareLink(record as ShareRecord)">
            <LinkOutlined /> 复制链接
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="撤销后链接立即失效，确定撤销吗？" @confirm="() => handleRevokeShare(record.id)">
            <a class="danger-link">
              <StopOutlined /> 撤销
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  DeleteOutlined, DownloadOutlined, EditOutlined, EyeOutlined, FileTextOutlined, LinkOutlined,
  PlusOutlined, ShareAltOutlined, StopOutlined, UploadOutlined,
} from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import XCCadViewer from '@/components/xuanchen/XCCadViewer.vue';
import XCOfficeViewer from '@/components/xuanchen/XCOfficeViewer.vue';
import XCWatermark from '@/components/xuanchen/XCWatermark.vue';
import { useAuthStore } from '@/stores';
import { computeFileMd5 } from '@/utils/fileMd5';
import {
  chunkCompleteApi, chunkInitApi, createFolderApi, createShareApi, deleteFileInfoApi, deleteFolderApi,
  getFileTextApi, getFileUrlApi, getFolderTreeApi, getMySharesApi, renameFolderApi,
  revokeShareApi, uploadChunkApi,
} from '../filemanage.api';
import type { FileInfoRecord, FolderRecord, ShareRecord } from '../filemanage.api';
import {
  columns, formatFileSize, isImageExt, isOfficeExt, isTextExt, queryParams, queryFormItems, storageTypeText,
} from '../filemanage.data';

const listRef = ref();
const authStore = useAuthStore();
const router = useRouter();

/** 支持协同编辑的扩展名（与后端 OfficeEditService 保持一致） */
const CO_EDIT_EXTS = ['docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'csv', 'txt'];
const isCoEditable = (ext?: string): boolean => !!ext && CO_EDIT_EXTS.includes(ext);

/** 进入 ONLYOFFICE 协同编辑全屏页 */
const handleCoEdit = (record: FileInfoRecord) => {
  router.push(`/fileedit/${record.id}`);
};

const url = {
  list: '/fileinfo/my',
  delete: '/fileinfo/delete',
};

/** 前端预校验：上限 50MB（与后端 xuanchen.fileinfo.max-size 一致） */
const MAX_SIZE_MB = 50;
const handleBeforeUpload = (file: File) => {
  if (file.size / 1024 / 1024 >= MAX_SIZE_MB) {
    message.error(`文件大小不能超过 ${MAX_SIZE_MB}MB`);
    return false;
  }
  return true;
};

//---------------------------- 分片上传（含秒传/断点续传） ----------------------------
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB，与后端单片上限（10MB）内取整
const uploading = ref(false);
const uploadPercent = ref(0);
const uploadFileName = ref('');

const handleUpload = async (option: { file: File }) => {
  const file = option.file;
  uploading.value = true;
  uploadFileName.value = file.name;
  uploadPercent.value = 0;
  try {
    //1、init：秒传命中直接完成；命中断点续传返回已传分片
    const md5 = await computeFileMd5(file);
    const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
    const initRes = await chunkInitApi({
      fileName: file.name,
      fileSize: file.size,
      md5,
      bizType: 'common',
      folderId: (queryParams.folderId as string) || '0',
      totalChunks,
      chunkSize: CHUNK_SIZE,
    });
    if (initRes.code !== 200 || !initRes.data) {
      message.error(initRes.msg || '上传初始化失败');
      return;
    }
    if (initRes.data.instant && initRes.data.file) {
      message.success(`「${initRes.data.file.name}」秒传成功！`);
      listRef.value?.loadData();
      return;
    }
    const uploadId = initRes.data.uploadId!;
    const uploadedSet = new Set(initRes.data.uploaded || []);
    //2、逐片上传（已传的跳过 = 断点续传）
    for (let i = 0; i < totalChunks; i++) {
      if (uploadedSet.has(i)) {
        uploadPercent.value = Math.round(((i + 1) / totalChunks) * 100);
        continue;
      }
      const start = i * CHUNK_SIZE;
      const blob = file.slice(start, Math.min(start + CHUNK_SIZE, file.size));
      const form = new FormData();
      form.append('uploadId', uploadId);
      form.append('index', String(i));
      form.append('file', blob, `chunk-${i}`);
      const chunkRes = await uploadChunkApi(form);
      if (chunkRes.code !== 200) {
        message.warning(chunkRes.msg || `第 ${i + 1} 片上传失败，重新上传同名文件可续传`);
        return;
      }
      uploadPercent.value = Math.round(((i + 1) / totalChunks) * 100);
    }
    //3、complete：服务端合并 + MD5 复核 + 落存储
    const completeRes = await chunkCompleteApi(uploadId);
    if (completeRes.code === 200 && completeRes.data?.file) {
      message.success(`「${completeRes.data.file.name}」上传成功！`);
      listRef.value?.loadData();
    } else {
      message.error(completeRes.msg || '上传完成处理失败');
    }
  } catch {
    // HTTP 错误已由响应拦截器统一提示
  } finally {
    uploading.value = false;
  }
};

//---------------------------- 文件夹树（三期） ----------------------------
const folderLoading = ref(false);
const folders = ref<FolderRecord[]>([]);
const selectedFolderKeys = ref<string[]>(['0']);
const expandedKeys = ref<string[]>([]);

interface FolderNode {
  title: string;
  key: string;
  children: FolderNode[];
}

const folderTreeData = computed<FolderNode[]>(() => {
  const nodeMap = new Map<string, FolderNode>();
  folders.value.forEach((f) => nodeMap.set(f.id, { title: f.folderName, key: f.id, children: [] }));
  const roots: FolderNode[] = [];
  folders.value.forEach((f) => {
    const node = nodeMap.get(f.id)!;
    const parent = f.parentId && f.parentId !== '0' ? nodeMap.get(f.parentId) : undefined;
    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });
  return [{ title: '我的文件', key: '0', children: roots }];
});

const loadFolders = async () => {
  folderLoading.value = true;
  try {
    const res = await getFolderTreeApi();
    if (res.code === 200) {
      folders.value = res.data || [];
    }
  } finally {
    folderLoading.value = false;
  }
};

const onSelectFolder = (keys: (string | number)[]) => {
  if (!keys.length) {
    return;
  }
  selectedFolderKeys.value = [String(keys[0])];
  //根节点 '0'=根目录文件；子文件夹=该文件夹内文件
  queryParams.folderId = String(keys[0]);
  queryParams.pageNo = 1;
  listRef.value?.loadData();
};

const onExpandTree = (keys: (string | number)[]) => {
  expandedKeys.value = keys.map(String);
};

//新建/重命名弹窗
const folderModalVisible = ref(false);
const folderModalMode = ref<'create' | 'rename'>('create');
const folderModalName = ref('');
const folderSubmitting = ref(false);
const renameTargetId = ref('');

const openCreateFolder = (parentKey: number | string) => {
  //工具栏按钮 parentKey=0：当前选中文件夹为根时挂根目录，否则挂当前文件夹
  const parent = selectedFolderKeys.value[0] || String(parentKey);
  folderModalMode.value = 'create';
  folderModalName.value = '';
  //存到临时变量供提交时使用
  createParentId.value = parent;
  folderModalVisible.value = true;
};
const createParentId = ref('0');

const openRenameFolder = (id: string, name: string) => {
  folderModalMode.value = 'rename';
  folderModalName.value = name;
  renameTargetId.value = id;
  folderModalVisible.value = true;
};

const handleFolderModalOk = async () => {
  const name = folderModalName.value.trim();
  if (!name) {
    message.warning('文件夹名不能为空');
    return;
  }
  folderSubmitting.value = true;
  try {
    const res = folderModalMode.value === 'create'
      ? await createFolderApi(createParentId.value, name)
      : await renameFolderApi(renameTargetId.value, name);
    if (res.code === 200) {
      message.success(folderModalMode.value === 'create' ? '创建成功！' : '重命名成功！');
      folderModalVisible.value = false;
      await loadFolders();
    } else {
      message.error(res.msg || '操作失败');
    }
  } finally {
    folderSubmitting.value = false;
  }
};

const handleDeleteFolder = async (id: string) => {
  const res = await deleteFolderApi(id);
  if (res.code === 200) {
    message.success('删除成功！文件夹下文件已移入回收站');
    //若删除的是当前所在文件夹，回到根目录
    if (selectedFolderKeys.value[0] === id) {
      selectedFolderKeys.value = ['0'];
      queryParams.folderId = '0';
    }
    await loadFolders();
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

//---------------------------- 预览（水印罩层，Office 转 PDF，文本服务端解码） ----------------------------
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewSrc = ref('');
const previewName = ref('');
const previewKind = ref<'image' | 'frame' | 'text' | 'markdown' | 'cad' | 'office'>('frame');

/** 文本预览：内容由后端解码后返回，编码可手动切换（自动识别失败时兜底） */
const previewText = ref('');
const textCharset = ref<string>('');
const detectedCharset = ref('');
const textTruncated = ref(false);
/** 切换编码时的独立 loading：不遮挡编码下拉，避免用户看到工具栏闪掉 */
const textLoading = ref(false);

/** Markdown 渲染版预览：复用文本预览的数据链路（/fileinfo/text），仅多一层渲染 */
const MD_EXTS = ['md', 'markdown'];
const mdViewMode = ref<'render' | 'source'>('render');
const mdRenderLoading = ref(false);
const mdHtml = ref('');

/** markdown-it + highlight.js 体积不小，首次打开 md 预览才动态加载（懒加载单例） */
let mdRendererPromise: Promise<(src: string) => string> | null = null;
const getMdRenderer = () => {
  mdRendererPromise ??= (async () => {
    const [{ default: MarkdownIt }, { default: hljs }] = await Promise.all([
      import('markdown-it'),
      import('highlight.js/lib/common'),
    ]);
    await import('highlight.js/styles/github.css');
    //独立 escape（不引用 md.utils：在 md 自身初始化器里引用 md 会造成循环推断，
    //vue-tsc 会把 md 推断成 any 并连锁报 TS7022/TS7006）
    const escapeHtml = (s: string) =>
      s.replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] as string,
      );
    const md = new MarkdownIt({
      //html:false：内嵌 HTML 一律转义展示（防 XSS），与 AI 助手组件同款安全姿势；
      //linkify 自动把裸 URL 变链接，markdown-it 内置对 javascript: 等危险协议的校验
      html: false,
      linkify: true,
      breaks: false,
      highlight: (str, lang) => {
        if (lang && hljs.getLanguage(lang)) {
          try {
            return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`;
          } catch {
            //语言解析异常时退回纯文本展示
          }
        }
        return `<pre class="hljs"><code>${escapeHtml(str)}</code></pre>`;
      },
    });
    //外链新窗口打开并断开 opener（防 Tab-nabbing）
    const fallbackLinkOpen: import('markdown-it').RendererRule = (tokens, idx, opts, _env, self) =>
      self.renderToken(tokens, idx, opts);
    const defaultLinkOpen = md.renderer.rules.link_open ?? fallbackLinkOpen;
    md.renderer.rules.link_open = (tokens, idx, opts, env, self) => {
      tokens[idx].attrSet('target', '_blank');
      tokens[idx].attrSet('rel', 'noopener noreferrer');
      return defaultLinkOpen(tokens, idx, opts, env, self);
    };
    return (src: string) => md.render(src);
  })();
  return mdRendererPromise;
};

const renderMarkdown = async () => {
  mdRenderLoading.value = true;
  try {
    const render = await getMdRenderer();
    mdHtml.value = render(previewText.value);
  } catch (e) {
    console.error('[MyFileList] Markdown 渲染失败', e);
    mdViewMode.value = 'source';
  } finally {
    mdRenderLoading.value = false;
  }
};
const charsetOptions = [
  { label: '自动识别', value: '' },
  { label: 'UTF-8', value: 'UTF-8' },
  { label: 'GBK / GB18030', value: 'GB18030' },
  { label: 'UTF-16LE', value: 'UTF-16LE' },
  { label: 'UTF-16BE', value: 'UTF-16BE' },
];

/** 当前预览的文件 id（切换编码时重新取内容） */
const previewFileId = ref('');

const watermarkText = computed(() => {
  const user = authStore.getUserInfo()?.userName || '';
  return `${user} ${new Date().toLocaleString('zh-CN')}`;
});

const CAD_EXTS = ['dwg', 'dxf'];
const SVG_EXTS = ['svg'];

const loadTextPreview = async (id: string, switching = false) => {
  if (switching) {
    textLoading.value = true;
  } else {
    previewLoading.value = true;
  }
  try {
    const res = await getFileTextApi(id, textCharset.value || undefined);
    if (res.code === 200 && res.data) {
      previewText.value = res.data.content;
      detectedCharset.value = res.data.charset;
      textTruncated.value = res.data.truncated;
    } else {
      message.error(res.msg || '获取文本内容失败');
    }
  } catch {
    //拦截器已提示
  } finally {
    if (switching) {
      textLoading.value = false;
    } else {
      previewLoading.value = false;
    }
  }
};

/** 切换编码：按新编码重新取内容（旧内容保留到新内容返回） */
const reloadTextPreview = async () => {
  if (!previewFileId.value) return;
  await loadTextPreview(previewFileId.value, true);
  if (previewKind.value === 'markdown') await renderMarkdown();
};

const handlePreview = async (record: FileInfoRecord) => {
  const office = isOfficeExt(record.ext);
  const cad = CAD_EXTS.includes(record.ext);
  const text = isTextExt(record.ext);
  if (!office && !cad && !text && !isImageExt(record.ext) && !SVG_EXTS.includes(record.ext)
    && record.ext !== 'pdf') {
    message.info('该类型不支持在线预览，请下载后查看');
    return;
  }
  previewName.value = record.originalName;
  previewFileId.value = record.id;
  previewVisible.value = true;
  previewLoading.value = true;
  //重置上一次预览的残留状态
  previewText.value = '';
  previewSrc.value = '';
  textCharset.value = '';
  detectedCharset.value = '';
  textTruncated.value = false;
  mdViewMode.value = 'render';
  mdHtml.value = '';
  if (text) {
    if (MD_EXTS.includes(record.ext)) {
      //Markdown：同样走服务端解码文本，取到后前端渲染（可切回源码查看）
      previewKind.value = 'markdown';
      await loadTextPreview(record.id);
      await renderMarkdown();
    } else {
      previewKind.value = 'text';
      await loadTextPreview(record.id);
    }
    return;
  }
  if (cad) {
    //CAD 图纸：前端浏览器直接解析渲染（DWG 走 LibreDWG WASM worker，DXF 内置解析器），
    //只需一个带有效期的预签名 URL；后端 LibreDWG 转链路保留作兜底
    previewKind.value = 'cad';
    try {
      const res = await getFileUrlApi(record.id, true);
      if (res.code === 200 && res.data) {
        previewSrc.value = res.data;
      } else {
        previewVisible.value = false;
        message.error(res.msg || '获取预览地址失败');
      }
    } catch {
      previewVisible.value = false;
    } finally {
      previewLoading.value = false;
    }
    return;
  }
  if (office) {
    //Office 文档：ONLYOFFICE 原格式只读预览（组件内自带失败提示与重试）
    previewKind.value = 'office';
    previewLoading.value = false;
    return;
  }
  previewKind.value = isImageExt(record.ext) || SVG_EXTS.includes(record.ext) ? 'image' : 'frame';
  try {
    const res = await getFileUrlApi(record.id, true);
    if (res.code === 200 && res.data) {
      previewSrc.value = res.data;
    } else {
      previewVisible.value = false;
      message.error(res.msg || '获取预览地址失败');
    }
  } catch {
    previewVisible.value = false;
  } finally {
    previewLoading.value = false;
  }
};

//---------------------------- 下载 / 删除 ----------------------------
const handleDownload = async (record: FileInfoRecord) => {
  const res = await getFileUrlApi(record.id, false);
  if (res.code !== 200 || !res.data) {
    message.error(res.msg || '获取文件地址失败');
    return;
  }
  const link = document.createElement('a');
  link.style.display = 'none';
  link.href = res.data;
  link.setAttribute('download', record.originalName);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

const handleDelete = async (id: string) => {
  const res = await deleteFileInfoApi(id);
  if (res.code === 200) {
    message.success('已移入回收站！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

//---------------------------- 共享 ----------------------------
const shareVisible = ref(false);
const shareCreating = ref(false);
const shareLoading = ref(false);
const currentFile = ref<FileInfoRecord | null>(null);
const shareList = ref<ShareRecord[]>([]);
const shareForm = reactive({ expireHours: 24, shareCode: '' });
const expireOptions = [
  { label: '1 小时', value: 1 },
  { label: '24 小时', value: 24 },
  { label: '3 天', value: 72 },
  { label: '7 天', value: 168 },
  { label: '30 天', value: 720 },
];

const shareColumns = [
  { title: '文件', dataIndex: 'fileName', ellipsis: true, width: 200 },
  { title: '大小', dataIndex: 'fileSize', width: 90, align: 'center' as const },
  { title: '提取码', dataIndex: 'hasCode', width: 90, align: 'center' as const },
  { title: '下载', dataIndex: 'visitCount', width: 80, align: 'center' as const },
  { title: '失效时间', dataIndex: 'expireTime', width: 160, align: 'center' as const },
  { title: '操作', dataIndex: 'operation', width: 140, align: 'center' as const },
];

const handleOpenShare = async (record: FileInfoRecord) => {
  currentFile.value = record;
  shareForm.expireHours = 24;
  shareForm.shareCode = '';
  shareVisible.value = true;
  await loadShares();
};

const loadShares = async () => {
  shareLoading.value = true;
  try {
    const res = await getMySharesApi();
    if (res.code === 200) {
      shareList.value = res.data || [];
    }
  } finally {
    shareLoading.value = false;
  }
};

const buildShareLink = (shareNo: string) => `${window.location.origin}/share/${shareNo}`;

const handleCreateShare = async () => {
  if (!currentFile.value) return;
  const code = shareForm.shareCode.trim();
  if (code && !/^[A-Za-z0-9]{4,8}$/.test(code)) {
    message.error('提取码须为 4~8 位字母或数字');
    return;
  }
  shareCreating.value = true;
  try {
    const res = await createShareApi(currentFile.value.id, shareForm.expireHours, code || undefined);
    if (res.code === 200 && res.data) {
      message.success('共享创建成功，链接已生成');
      await navigator.clipboard.writeText(buildShareLink(res.data.shareNo));
      message.info('链接已复制到剪贴板');
      await loadShares();
    } else {
      message.error(res.msg || '共享创建失败');
    }
  } finally {
    shareCreating.value = false;
  }
};

//局部别名：保持原有 import 列表可读

const handleCopyShareLink = async (record: ShareRecord) => {
  try {
    await navigator.clipboard.writeText(buildShareLink(record.shareNo));
    message.success('链接已复制到剪贴板');
  } catch {
    message.warning(`请手动复制：${buildShareLink(record.shareNo)}`);
  }
};

const handleRevokeShare = async (id: string) => {
  const res = await revokeShareApi(id);
  if (res.code === 200) {
    message.success('已撤销！');
    await loadShares();
  } else {
    message.error(res.msg || '撤销失败');
  }
};

onMounted(() => {
  //queryParams.folderId 默认 '0'（data.ts），与树默认选中"我的文件"根节点一致
  loadFolders();
});
</script>

<style scoped>
.my-file-page {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.folder-panel {
  width: 230px;
  flex-shrink: 0;
  align-self: flex-start;
}

.list-panel {
  flex: 1;
  min-width: 0;
}

.folder-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.folder-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-ops {
  display: none;
  gap: 8px;
}

.folder-title:hover .folder-ops {
  display: inline-flex;
}

.danger-icon {
  color: #ff4d4f;
}

.upload-file-name {
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
  word-break: break-all;
}

.upload-hint {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
  text-align: center;
}

.preview-body {
  position: relative;
  height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.preview-spin {
  margin: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-office {
  width: 100%;
  height: 100%;
}

/* CAD 查看器：撑满预览区，canvas 自适应容器 */
.preview-cad {
  width: 100%;
  height: 100%;
}

/* 文本预览：撑满容器、等宽字体、长行自动换行 */
.preview-text-wrap {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #fff;
}

.preview-text-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-text-label {
  color: #666;
  font-size: 13px;
}

.preview-text-loading {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
}

.preview-text-meta {
  color: #999;
  font-size: 12px;
}

.preview-text {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 12px 16px;
  overflow: auto;
  color: #333;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  tab-size: 4;
}

/* Markdown 渲染区（v-html 注入的节点无 scoped 属性，内部元素必须 :deep 穿透） */
.preview-markdown {
  flex: 1;
  min-height: 0;
  padding: 16px 28px 32px;
  overflow: auto;
  color: #333;
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
}
.preview-markdown :deep(h1),
.preview-markdown :deep(h2),
.preview-markdown :deep(h3),
.preview-markdown :deep(h4) {
  margin: 1.2em 0 0.6em;
  font-weight: 600;
  line-height: 1.4;
}
.preview-markdown :deep(h1) {
  font-size: 1.7em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eee;
}
.preview-markdown :deep(h2) {
  font-size: 1.4em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eee;
}
.preview-markdown :deep(h3) {
  font-size: 1.2em;
}
.preview-markdown :deep(p) {
  margin: 0.6em 0;
}
.preview-markdown :deep(a) {
  color: #1677ff;
  text-decoration: none;
}
.preview-markdown :deep(a:hover) {
  text-decoration: underline;
}
.preview-markdown :deep(ul),
.preview-markdown :deep(ol) {
  margin: 0.6em 0;
  padding-left: 1.6em;
}
.preview-markdown :deep(blockquote) {
  margin: 0.8em 0;
  padding: 4px 16px;
  color: #666;
  border-left: 4px solid #d9d9d9;
  background: #fafafa;
}
.preview-markdown :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #f2f2f3;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 0.9em;
}
.preview-markdown :deep(pre.hljs) {
  margin: 0.8em 0;
  padding: 12px 16px;
  border-radius: 6px;
  background: #f6f8fa;
  overflow: auto;
}
.preview-markdown :deep(pre.hljs code) {
  padding: 0;
  background: none;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
}
.preview-markdown :deep(table) {
  margin: 0.8em 0;
  border-collapse: collapse;
  display: block;
  max-width: 100%;
  overflow: auto;
}
.preview-markdown :deep(th),
.preview-markdown :deep(td) {
  padding: 6px 14px;
  border: 1px solid #e8e8e8;
}
.preview-markdown :deep(th) {
  background: #fafafa;
  font-weight: 600;
}
.preview-markdown :deep(img) {
  max-width: 100%;
}
.preview-markdown :deep(hr) {
  margin: 1.2em 0;
  border: none;
  border-top: 1px solid #eee;
}

.share-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 12px;
}

.share-file {
  margin-bottom: 4px;
  color: #666;
  font-size: 13px;
}

.danger-link {
  color: #ff4d4f;
}
</style>

<style>
/*
 * 全屏预览：内容区必须跟着弹窗撑满视口。
 * 必须放在非 scoped 块：XCModal 把弹窗 teleport 到 body，scoped 的属性选择器命不中包裹层；
 * 且用 `.xc-full-modal.preview-modal` 双类限定，避免影响其它全屏 XCModal（表单类弹窗要保留 body 内边距）。
 */
.xc-full-modal.preview-modal .ant-modal-body {
  display: flex;
  flex-direction: column;
  padding: 0;
}

/* 非全屏时 .preview-body 固定 70vh，全屏时改为吃掉剩余高度（依次高于 scoped 里的 70vh） */
.xc-full-modal.preview-modal .preview-body {
  flex: 1;
  min-height: 0;
  height: auto;
}
</style>
