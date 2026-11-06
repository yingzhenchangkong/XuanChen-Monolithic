<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItemsIndex"
    :query-params="queryParamsIndex">
    <!-- 自定义按钮：全部已读 / 批量已读（批量已读仅在有选中时出现） -->
    <template #toolbar-prepend="{ state, loadData }">
      <a-button type="primary" @click="handleReadAll(loadData)">
        <template #icon>
          <ReadOutlined />
        </template>全部标记为已读
      </a-button>
      <a-button v-if="state.selectedRowKeys.length > 0" @click="handleReadBatch(state.selectedRows, loadData)">
        <template #icon>
          <ReadOutlined />
        </template>批量标记为已读
      </a-button>
    </template>

    <!-- 操作列：详情 -->
    <template #operation="{ record }">
      <a @click="handleDetail(record)">
        <FileTextOutlined />详情
      </a>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ loadData }">
      <Detail ref="refDetail" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { message } from 'ant-design-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Detail from './modal/Detail.vue';

import { queryParamsIndex, queryFormItemsIndex, columnsIndex } from './notice.data';
import { getDictSelect } from '@/views/system/dict/dict.api';
import { NoticeApiUrl, setReadBatchApi, setReadAllApi } from './notice.api';

/** 列表接口 */
const url = {
  list: NoticeApiUrl.INDEX_USER_LIST,
};

const refDetail = ref();
/** 查看详情 */
const handleDetail = (record: Record<string, unknown>) => {
  refDetail.value.detail(record);
}

/** 全部标记为已读 */
const handleReadAll = async (loadData: () => void) => {
  const res = await setReadAllApi();
  if (res.code === 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.error(res.msg);
  }
}
/** 批量标记为已读（取选中行上的通知状态主键） */
const handleReadBatch = async (rows: Record<string, unknown>[], loadData: () => void) => {
  if (rows.length <= 0) {
    message.warning('请选择一条记录！')
    return
  }
  const ids = rows.map(item => String(item.noticeStatusId));
  const res = await setReadBatchApi(ids);
  if (res.code == 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.warning(res.msg);
  }
}

/** 获取阅读状态数据(通知列表) */
const loadStatus = async () => {
  queryFormItemsIndex[1]!.options = await getDictSelect("yes_no");
};
loadStatus();
</script>
