<template>
  <a-modal v-model:open="visible" title="生成预览" width="92%" :footer="null" :body-style="{ height: '72vh' }">
    <a-alert type="info" show-icon style="margin-bottom: 8px"
      message="以下内容在服务器临时目录生成，未写入输出目录；确认无误后再点「代码生成」落盘。" />
    <a-row :gutter="12" style="height: calc(100% - 46px)">
      <a-col :span="7" style="height: 100%; overflow: auto; border-right: 1px solid #f0f0f0">
        <a-empty v-if="!files.length" description="没有生成任何文件" />
        <div v-for="file in files" :key="file.path" class="preview-item"
          :class="{ 'preview-item-active': file.path === current?.path }" @click="selectFile(file)">
          <FileTextOutlined />
          <span class="preview-path">{{ file.path }}</span>
        </div>
      </a-col>
      <a-col :span="17" style="height: 100%; overflow: auto">
        <template v-if="current">
          <div class="preview-title">
            {{ current.path }}
            <a-tag v-if="current.truncated" color="orange">内容已截断</a-tag>
          </div>
          <pre class="preview-content">{{ current.content }}</pre>
        </template>
        <a-empty v-else description="请选择左侧文件" />
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { GenPreviewFile } from '../gentable.types';

const visible = ref(false);
const files = ref<GenPreviewFile[]>([]);
const currentPath = ref('');

const current = computed(() => files.value.find((item) => item.path === currentPath.value));

/** 选中左侧某个文件 */
const selectFile = (file: GenPreviewFile) => {
  currentPath.value = file.path;
};

/** 打开预览：默认选中 Index.vue（没有则选第一个） */
const show = (data: GenPreviewFile[]) => {
  files.value = data || [];
  const preferred = files.value.find((item) => item.path.endsWith('/Index.vue'))
    || files.value.find((item) => item.path.endsWith('.vue'))
    || files.value[0];
  currentPath.value = preferred?.path || '';
  visible.value = true;
};

defineExpose({ show });
</script>

<style lang="scss" scoped>
.preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;

  &:hover {
    background: #f5f5f5;
  }
}

.preview-item-active {
  background: #e6f4ff;
  color: #1677ff;
}

.preview-path {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-title {
  font-weight: 600;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
  word-break: break-all;
}

.preview-content {
  margin: 0;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #262626;
}
</style>
