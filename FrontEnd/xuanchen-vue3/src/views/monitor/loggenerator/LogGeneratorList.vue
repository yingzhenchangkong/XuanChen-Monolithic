<template>
  <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
    :selection="false" @search="handleSearch">
    <template #bodyCell="{ column, record }">
      <!-- 表名列：表名 + 表注释 -->
      <template v-if="column.dataIndex === 'tableName'">
        <div class="table-cell">
          <span class="table-name">{{ record.tableName || '-' }}</span>
          <span v-if="record.tableComment" class="table-comment">{{ record.tableComment }}</span>
        </div>
      </template>
      <template v-else-if="column.dataIndex === 'generateType'">
        {{ typeText(record.generateType) }}
      </template>
      <template v-else-if="column.dataIndex === 'fileList'">
        <a v-if="record.fileList" @click="showFiles(record)">{{ record.fileCount || 0 }} 个文件</a>
        <span v-else class="text-placeholder">-</span>
      </template>
      <template v-else-if="column.dataIndex === 'success'">
        <a-tag :color="record.success ? 'green' : 'red'">{{ record.success ? '成功' : '失败' }}</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'message'">
        <span class="log-message" :title="record.message">{{ record.message || '-' }}</span>
      </template>
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { h, reactive } from 'vue';
import { Modal } from 'ant-design-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';

import { queryParams, queryFormItems, columns } from './loggenerator.data';
import { LogGeneratorApiUrl } from './loggenerator.api';
import { getDictSelect } from '@/views/system/dict/dict.api';

/** 列表接口（日志只读：无新增/删除/导出） */
const url = reactive({
  list: LogGeneratorApiUrl.INDEX_LIST,
});

/** 生成方式文案（与后端 LogGeneratorServiceImpl#record 的入参一致） */
const typeText = (type?: string) => {
  if (type === 'DISK') return '落盘生成';
  if (type === 'ZIP') return '打包下载';
  if (type === 'PREVIEW') return '预览';
  return type || '-';
};

/**
 * 时间范围拆解为 beginTime / endTime。
 * 请求由组件在 emit('search') 之后立刻发起（同步走到 axios，params 在此刻被快照），
 * 因此这里先临时置空 timeRange，避免原始数组随请求发出；本同步流程结束后再还原。
 */
const handleSearch = () => {
  if (queryParams.timeRange.length > 0) {
    queryParams.beginTime = queryParams.timeRange[0];
    queryParams.endTime = queryParams.timeRange[1];
  }
  const temp = queryParams.timeRange;
  queryParams.timeRange = [];
  queueMicrotask(() => {
    queryParams.timeRange = temp;
  });
};

/** 本次生成写出的文件清单（与代码生成器页面的"生成记录"弹窗同款展示） */
const showFiles = (record: Record<string, any>) => {
  Modal.info({
    title: `本次生成的文件（${record.fileCount || 0} 个）`,
    width: 680,
    content: h('div', { style: 'max-height:340px;overflow:auto;font-size:12px;line-height:20px' },
      String(record.fileList || '').split('\n').map((line: string) => h('div', line))),
  });
};

const loadStatus = async () => {
  queryFormItems[3]!.options = await getDictSelect('succ_fail');
};
loadStatus();
</script>

<style lang="scss" scoped>
.table-cell {
  display: flex;
  flex-direction: column;
  line-height: 18px;

  .table-name {
    font-weight: 500;
  }

  .table-comment {
    color: #8c8c8c;
    font-size: 12px;
  }
}

.log-message {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-placeholder {
  color: #bfbfbf;
}
</style>
