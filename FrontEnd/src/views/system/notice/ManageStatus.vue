<template>
  <!--表格区域-->
  <a-table :dataSource="dataSource" :columns="columnsStatus" :pagination="ipagination" :loading="loading" bordered
    rowKey="id" size="small" @change="handleTableChange">
  </a-table>
</template>

<script lang="ts" setup>
import { useList } from '@/hooks/useList'
import { watch } from 'vue';

import { queryParamsStatus, columnsStatus } from './notice.data';

const props = defineProps({
  mainId: String
})

/** url */
const url = {
  list: '/system/notice/listManageStatus',
}

watch(() => props.mainId, (newVal, oldVal) => {
  if (!props.mainId) {
    handleReset();
  } else {
    queryParamsStatus.noticeId = props.mainId;
    loadData();
  }
})

/** 重置 */
const handleReset = () => {
  queryParamsStatus.noticeId = '';
  loadData();
}
const queryParams = queryParamsStatus;
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
loadData()
</script>