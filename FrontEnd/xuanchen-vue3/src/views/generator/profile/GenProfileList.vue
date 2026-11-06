<template>
  <!-- 项目预设：数据源 + 输出路径 + 包名/模块名 + 模板 的可复用组合 -->
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true }" export-title="项目预设">
    <!-- 数据源列显示连接名称，模板列显示可读文案 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'databaseId'">
        {{ databaseName(record.databaseId) }}
      </template>
      <template v-else-if="column.dataIndex === 'template'">
        <a-tag :color="record.template === 'single' ? 'purple' : 'green'">
          {{ record.template === 'single' ? '单表' : '一对多' }}
        </a-tag>
      </template>
    </template>

    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Operation from './modal/Operation.vue';

import { GenProfileApiUrl } from './profile.api';
import { getGenDatabaseSelect } from '../gendatabase/gendatabase.api';
import { columnsIndex, queryFormItems, queryParams } from './profile.data';

/** 列表接口（编辑/删除走 XCListPage 默认操作列） */
const url = reactive({
  list: GenProfileApiUrl.INDEX_LIST,
  delete: GenProfileApiUrl.INDEX_DELETE,
});

/** 数据源 id -> 连接名称 */
const databaseMap = ref<Record<string, string>>({});
const databaseName = (id?: string) => {
  if (!id) {
    return '-';
  }
  return databaseMap.value[id] || id;
};

const loadDatabases = async () => {
  const res = await getGenDatabaseSelect();
  const map: Record<string, string> = {};
  (res.data ?? []).forEach((item) => {
    const id = typeof item.id === 'string' ? item.id : '';
    if (id) {
      map[id] = String(item.connName ?? id);
    }
  });
  databaseMap.value = map;
};
loadDatabases();
</script>
