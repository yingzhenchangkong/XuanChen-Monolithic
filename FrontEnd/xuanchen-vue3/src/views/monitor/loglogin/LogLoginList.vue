<template>
  <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ export: true }" export-title="登录日志" @search="handleSearch">
    <!-- 登录状态列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'volcano'">
          {{ record.status === 1 ? '登录成功' : '登录失败' }}
        </a-tag>
      </template>
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';

import { queryParams, queryFormItems, columns } from './loglogin.data';
import { LogLoginApiUrl } from './loglogin.api';
import { getUserSelect } from '@/views/system/account/account.api';
import { getDictSelect } from '@/views/system/dict/dict.api';

/** 列表接口 */
const url = reactive({
  list: LogLoginApiUrl.INDEX_LIST,
  exportExcel: LogLoginApiUrl.INDEX_EXPORT_EXCEL,
});

/**
 * 时间范围拆解为 beginTime / endTime。
 * 请求由组件在 emit('search') 之后立刻发起（同步走到 axios，params 在此刻被快照），
 * 因此这里先临时置空 timeRange，避免原始数组随请求发出；本同步流程结束后再还原，筛选框仍显示已选区间。
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
}

const loadUserSelect = async () => {
  queryFormItems[0]!.options = await getUserSelect();
};
loadUserSelect();
const loadStatus = async () => {
  queryFormItems[2]!.options = await getDictSelect("succ_fail");
};
loadStatus();
</script>
