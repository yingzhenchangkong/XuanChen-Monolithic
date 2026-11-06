<template>
  <a-card title="检索测试">
    <template #extra>
      <span class="search-tip">用于调优 minScore / 分块参数：输入问题查看命中的知识库分块与相似度</span>
    </template>

    <a-form layout="inline" class="search-form" @submit.prevent="onSearch">
      <a-form-item label="知识库">
        <a-select v-model:value="selectedKbIds" mode="multiple" placeholder="全部知识库（留空=全库检索）"
          style="min-width: 320px" :options="kbOptions" :loading="loadingKb" allow-clear />
      </a-form-item>
      <a-form-item label="问题">
        <a-input v-model:value="query" placeholder="输入测试问题" style="width: 420px" @press-enter="onSearch" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="searching" :disabled="!query.trim()" @click="onSearch">
          <template #icon>
            <SearchOutlined />
          </template>检索
        </a-button>
      </a-form-item>
    </a-form>

    <a-empty v-if="!hits.length && !searching" description="暂无检索结果" style="margin-top: 48px" />

    <div v-for="(hit, i) in hits" :key="i" class="hit-card">
      <div class="hit-head">
        <a-tag color="blue">[{{ i + 1 }}]</a-tag>
        <span class="hit-file">{{ hit.fileName || '未知文档' }}</span>
        <a-tag :color="hit.score >= 0.8 ? 'green' : hit.score >= 0.6 ? 'orange' : 'red'">
          相似度 {{ (hit.score * 100).toFixed(1) }}%
        </a-tag>
      </div>
      <div class="hit-content">{{ hit.content }}</div>
    </div>
  </a-card>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { SearchOutlined } from '@ant-design/icons-vue';

import { getAction } from '@/utils/httpAction';
import { searchKbApi } from './knowledge.api';
import type { AiSearchHit, AiKbRecord } from './knowledge.types';
import type { PageResult } from '@/types/api';

const query = ref('');
const selectedKbIds = ref<string[]>([]);
const kbOptions = ref<{ label: string; value: string }[]>([]);
const loadingKb = ref(false);
const searching = ref(false);
const hits = ref<AiSearchHit[]>([]);

const kbList = ref<AiKbRecord[]>([]);

onMounted(loadKb);

async function loadKb() {
  loadingKb.value = true;
  try {
    const res = await getAction<PageResult<AiKbRecord>>('/ai/kb/list', { pageNo: 1, pageSize: 100 });
    if (res.code === 200) {
      kbList.value = res.data.records || [];
      kbOptions.value = kbList.value.map((kb) => ({ label: kb.kbName, value: kb.id }));
    }
  } finally {
    loadingKb.value = false;
  }
}

async function onSearch() {
  const q = query.value.trim();
  if (!q || searching.value) return;
  searching.value = true;
  hits.value = [];
  try {
    const res = await searchKbApi(q, selectedKbIds.value);
    if (res.code === 200) {
      hits.value = res.data || [];
      if (!hits.value.length) message.info('没有命中任何分块，可尝试降低 minScore 或补充相关文档');
    } else {
      message.error(res.msg || '检索失败');
    }
  } finally {
    searching.value = false;
  }
}
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}
.search-tip {
  color: #999;
  font-size: 12px;
}
.hit-card {
  border: 1px solid #e8eaee;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafbfc;
}
.hit-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.hit-file {
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hit-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  color: #333;
}
</style>
