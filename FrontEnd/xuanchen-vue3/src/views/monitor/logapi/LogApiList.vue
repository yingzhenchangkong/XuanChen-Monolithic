<template>
  <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
    :selection="false" @search="handleSearch">
    <template #bodyCell="{ column, record }">
      <!-- 接口列：名称 + 编码（接口被删除时只剩编码，名称留空） -->
      <template v-if="column.dataIndex === 'apiName'">
        <div class="api-cell">
          <span class="api-name">{{ record.apiName || '（接口已删除）' }}</span>
          <span class="api-code">{{ record.apiCode }}</span>
        </div>
      </template>
      <template v-else-if="column.dataIndex === 'triggerType'">
        {{ triggerText(record.triggerType) }}
      </template>
      <template v-else-if="column.dataIndex === 'success'">
        <a-tag :color="record.success === 1 ? 'green' : 'red'">
          {{ record.success === 1 ? '成功' : '失败' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'costMs'">
        {{ record.costMs == null ? '-' : `${record.costMs} ms` }}
      </template>
      <template v-else-if="column.dataIndex === 'errorMsg'">
        <span class="log-message" :title="record.errorMsg">{{ record.errorMsg || '-' }}</span>
      </template>
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';

import { queryParams, queryFormItems, columns } from './logapi.data';
import { LogApiApiUrl } from './logapi.api';
import { getDictSelect } from '@/views/system/dict/dict.api';

/** 列表接口（日志只读：无新增/删除/导出） */
const url = reactive({
  list: LogApiApiUrl.INDEX_LIST,
});

/** 触发方式文案（与后端 ApiInvokeServiceImpl 的 TRIGGER_* 常量对应） */
const triggerText = (triggerType?: number) => {
  if (triggerType === 1) return '手动调用';
  if (triggerType === 2) return '定时采集';
  if (triggerType === 3) return '测试运行';
  return '-';
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

const loadStatus = async () => {
  queryFormItems[2]!.options = await getDictSelect('succ_fail');
};
loadStatus();
</script>

<style lang="scss" scoped>
.api-cell {
  display: flex;
  flex-direction: column;
  line-height: 18px;

  .api-name {
    font-weight: 500;
  }

  .api-code {
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
</style>
