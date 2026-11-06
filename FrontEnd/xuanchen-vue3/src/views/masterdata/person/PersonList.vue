<template>
  <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, import: true, export: true, recycleBin: true, deleteBatch: true }" export-title="人员管理"
    @recycle-bin="handleRecycleBin">
    <!-- 操作列：编辑 / 详情 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleDetail(record)">
        <ProfileOutlined /> 详情
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 照片列（点击图片可预览）/ 性别列 / 部门列 / 岗位列 / 人员类型列 / 人员状态列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'photo'">
        <a-image v-if="record.photo" :src="getImageView(record.photo as string) ?? undefined" :width="40" :height="40"
          :style="{ objectFit: 'cover', borderRadius: '4px' }" />
        <span v-else>-</span>
      </template>
      <template v-else-if="column.dataIndex === 'gender'">
        {{ genderMap[Number(record.gender)] ?? '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'deptNames'">
        {{ record.deptNames || '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'postNames'">
        {{ record.postNames || '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'personType'">
        <a-tag v-if="personTypeMap[record.personType]"
          :color="personTypeMap[record.personType].color">
          {{ personTypeMap[record.personType].text }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag v-if="personStatusMap[record.status]"
          :color="personStatusMap[record.status].color">
          {{ personStatusMap[record.status].text }}
        </a-tag>
      </template>
    </template>

    <!-- 弹窗区域：新增/编辑（含部门/岗位）、详情、回收站 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <Detail ref="refDetail" />
      <RecycleBin ref="refRecycleBin" :url="recycleUrl" :columns="columnsRcvBin"
        :query-form-items="queryFormItemsRcvBin" :query-params="recycleQueryParams" primary-buttons
        @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import Detail from './modal/Detail.vue';
import { getImageView } from '@/utils/ImageUtil';

import { PersonApiUrl } from './person.api';
import {
  queryParams,
  queryFormItems,
  columns,
  genderMap,
  personTypeMap,
  personStatusMap,
  queryFormItemsRcvBin,
  columnsRcvBin,
} from './person.data';

/** 列表接口 */
const url = reactive({
  list: PersonApiUrl.INDEX_LIST,
  delete: PersonApiUrl.INDEX_DELETE,
  deleteBatch: PersonApiUrl.INDEX_DELETE_BATCH,
  exportExcel: PersonApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: PersonApiUrl.INDEX_IMPORT_EXCEL,
});

/** 回收站接口 */
const recycleUrl = {
  list: PersonApiUrl.REC_BIN_LIST,
  delete: PersonApiUrl.REC_BIN_DELETE,
  deleteBatch: PersonApiUrl.REC_BIN_DELETE_BATCH,
  revert: PersonApiUrl.REC_BIN_REVERT,
  revertBatch: PersonApiUrl.REC_BIN_REVERT_BATCH,
};

/** 回收站查询条件默认值 */
const recycleQueryParams = { realName: '', mobile: '' };

/** 回收站 */
const refRecycleBin = ref();
const handleRecycleBin = () => {
  refRecycleBin.value.show();
};

/** 详情弹窗 */
const refDetail = ref();
const handleDetail = (record: Record<string, unknown>) => {
  refDetail.value.show(record);
};
</script>
