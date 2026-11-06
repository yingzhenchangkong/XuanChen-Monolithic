<template>
  <XCModal v-model:open="openModel" :title="title" :width="760" okText="选择当前目录"
    :show-fullscreen-btn="false" @ok="handleOk">
    <div class="dir-picker">
      <div class="dir-picker-bar">
        <a-select v-model:value="rootPath" :options="rootOptions" style="width: 170px" placeholder="根目录"
          @change="handleChangeRoot" />
        <a-button :disabled="!current.parent" @click="goParent">
          <template #icon>
            <ArrowUpOutlined />
          </template>上一级
        </a-button>
        <a-input v-model:value="currentPath" readonly class="dir-picker-path" />
      </div>

      <div class="dir-picker-bar">
        <a-input v-model:value="newDirName" placeholder="新建文件夹名称" allowClear style="width: 220px"
          @press-enter="handleMkdir" />
        <a-button @click="handleMkdir">
          <template #icon>
            <FolderAddOutlined />
          </template>新建文件夹
        </a-button>
        <span v-if="current.path && current.writable === false" class="dir-picker-warn">
          当前目录不可写，请换一个目录
        </span>
        <span v-else-if="current.restricted" class="dir-picker-tip">已限制在服务器白名单目录内</span>
      </div>

      <a-alert v-if="mkdirTip" type="success" show-icon closable class="dir-picker-alert"
        :message="mkdirTip" @close="clearCreated" />

      <a-spin :spinning="loading">
        <div class="dir-picker-list">
          <div v-for="dir in current.dirs" :key="dir.path" class="dir-picker-item"
            :class="{ 'dir-picker-item-new': dir.name === createdName }" :data-dir-name="dir.name"
            @click="enterDir(dir)">
            <FolderOutlined class="dir-picker-icon" />
            <span class="dir-picker-name">{{ dir.name }}</span>
            <span v-if="dir.name === createdName" class="dir-picker-badge">新建</span>
            <span v-if="dir.writable === false" class="dir-picker-lock">只读</span>
          </div>
          <a-empty v-if="!loading && !current.dirs.length" description="该目录下没有子文件夹，可直接选择当前目录" />
        </div>
      </a-spin>

      <div class="dir-picker-footer">
        生成产物会写入：<b>{{ currentPath || '（请选择目录）' }}</b>
      </div>
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { ArrowUpOutlined, FolderAddOutlined, FolderOutlined } from '@ant-design/icons-vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import { createDir, getDirListing } from '../gentable.api';
import type { DirItem, DirListing } from '../gentable.types';

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  /** 已选目录，打开时定位到该目录 */
  value: {
    type: String,
    default: '',
  },
  /** 弹窗标题：输出目录与相对路径共用同一个选择器 */
  title: {
    type: String,
    default: '选择输出目录',
  },
});

const emit = defineEmits(['update:open', 'select']);

const openModel = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val),
});

const loading = ref(false);
const currentPath = ref('');
const rootPath = ref<string | undefined>(undefined);
const newDirName = ref('');
const current = ref<DirListing>({ path: '', roots: [], dirs: [] });
/** 刚创建的文件夹名：在列表中高亮展示 */
const createdName = ref('');
/** 创建成功的文字提示（显示在列表上方，可手动关闭/自动消失） */
const mkdirTip = ref('');
let tipTimer: ReturnType<typeof setTimeout> | undefined;

const clearCreated = () => {
  createdName.value = '';
  mkdirTip.value = '';
  if (tipTimer) {
    clearTimeout(tipTimer);
    tipTimer = undefined;
  }
};

const rootOptions = computed(() =>
  (current.value.roots || []).map((root) => ({ label: root.path, value: root.path })),
);

/** 规范化路径用于比较（Windows 路径大小写/分隔符不敏感） */
const normalize = (path: string) => path.replace(/\\/g, '/').toLowerCase();

/** 找出当前路径属于哪个根目录，用于回填根目录下拉 */
const matchRoot = (path: string) => {
  const target = normalize(path);
  return (current.value.roots || [])
    .map((root) => root.path)
    .filter((root) => target === normalize(root) || target.startsWith(normalize(root).replace(/\/$/, '') + '/'))
    .sort((a, b) => b.length - a.length)[0];
};

const load = async (path?: string) => {
  loading.value = true;
  try {
    const res = await getDirListing(path);
    if (!res) {
      return;
    }
    current.value = res;
    currentPath.value = res.path;
    rootPath.value = res.path ? matchRoot(res.path) : undefined;
    newDirName.value = '';
  } finally {
    loading.value = false;
  }
};

const init = async () => {
  clearCreated();
  // 已选过目录就直接定位过去，否则先取根列表并进入第一个根
  if (props.value) {
    await load(props.value);
    if (current.value.path) {
      return;
    }
  }
  await load();
  // 优先进入服务端建议目录（未限制时为当前用户主目录，避免直接落到盘符根）
  const startPath = current.value.defaultPath || current.value.roots?.[0]?.path;
  if (startPath) {
    await load(startPath);
  }
};

watch(
  () => props.open,
  (open) => {
    if (open) {
      init();
    }
  },
);

const enterDir = (dir: DirItem) => {
  clearCreated();
  load(dir.path);
};
const goParent = () => {
  if (current.value.parent) {
    clearCreated();
    load(current.value.parent);
  }
};
const handleChangeRoot = (path: string) => {
  clearCreated();
  load(path);
};

const handleMkdir = async () => {
  const name = newDirName.value.trim();
  if (!name) {
    message.warning('请输入文件夹名称');
    return;
  }
  if (!currentPath.value) {
    message.warning('请先选择父目录');
    return;
  }
  const created = await createDir(currentPath.value, name);
  if (created) {
    message.success('创建成功');
    // 留在当前目录刷新列表，让新文件夹实时显示在下方并高亮
    createdName.value = name;
    mkdirTip.value = `已成功创建文件夹「${name}」，已在下方列表中显示`;
    if (tipTimer) {
      clearTimeout(tipTimer);
    }
    tipTimer = setTimeout(() => {
      mkdirTip.value = '';
    }, 6000);
    await load(currentPath.value);
    await nextTick();
    const el = document.querySelector(`[data-dir-name="${CSS.escape(name)}"]`);
    el?.scrollIntoView({ block: 'nearest' });
  }
};

const handleOk = () => {
  if (!currentPath.value) {
    message.warning('请选择输出目录');
    return;
  }
  emit('select', currentPath.value);
  openModel.value = false;
};

defineExpose({ init });
</script>

<style lang="scss" scoped>
.dir-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dir-picker-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dir-picker-path {
  flex: 1;
  min-width: 0;
  font-family: Consolas, Monaco, monospace;
}

.dir-picker-warn {
  color: #d4380d;
  font-size: 12px;
}

.dir-picker-tip {
  color: #8c8c8c;
  font-size: 12px;
}

.dir-picker-alert {
  margin: 0;
  padding: 4px 8px;
  font-size: 12px;
}

.dir-picker-list {
  height: 320px;
  overflow: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 4px;
}

.dir-picker-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: #f5f5f5;
  }

  &.dir-picker-item-new {
    background: #f6ffed;

    .dir-picker-name {
      color: #389e0d;
      font-weight: 600;
    }
  }
}

.dir-picker-icon {
  color: #faad14;
}

.dir-picker-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dir-picker-lock {
  color: #bfbfbf;
  font-size: 12px;
}

.dir-picker-badge {
  color: #389e0d;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 2px;
  font-size: 12px;
  line-height: 18px;
  padding: 0 6px;
}

.dir-picker-footer {
  color: #595959;
  font-size: 12px;
  word-break: break-all;
}
</style>
