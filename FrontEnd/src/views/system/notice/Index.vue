<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParamsIndex" :formItems="queryFormItemsIndex" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleReadAll">
        <template #icon>
          <ReadOutlined />
        </template>全部标记为已读
      </a-button>
      <template v-if="state.selectedRowKeys.length > 0">
        <a-button @click="handleReadBatch">
          <template #icon>
            <ReadOutlined />
          </template>批量标记为已读
        </a-button>
        <a-button @click="handleCancelSelect">
          <template #icon>
            <UndoOutlined />
          </template>取消选择
        </a-button>
        <a-tag color="processing" style="float: right;">
          <template #icon>
            <CheckSquareOutlined />
          </template>
          {{ `已选择 ${state.selectedRowKeys.length} 条` }}
        </a-tag>
      </template>
    </div>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columnsIndex" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleDetail(record)">
            <FileTextOutlined />详情
          </a>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Detail ref="refDetail" @childOK="handleReset" />
  </a-card>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { useList } from '@/hooks/useList';
import { message } from 'ant-design-vue';
import Detail from './modal/Detail.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { queryParamsIndex, queryFormItemsIndex, columnsIndex } from './notice.data';
import { getDictSelect } from '../dict/dict.api';
import { NoticeApiUrl, setReadBatchApi, setReadAllApi } from './notice.api';

/** url */
const url = reactive({
  list: NoticeApiUrl.LIST_USER,
})

const refDetail = ref();
const handleDetail = (record: any) => {
  refDetail.value.detail(record);
}

/** 批量标记为已读 */
const handleReadBatch = async () => {
  if (state.selectedRowKeys.length <= 0) {
    message.warning('请选择一条记录！')
    return
  }
  let ids: string[] = [];
  state.selectedRows.forEach(item => {
    ids.push(item.noticeStatusId);
  })
  const res: any = await setReadBatchApi(ids);
  if (res.code == 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.warning(res.msg);
  }
}
/** 全部标记为已读 */
const handleReadAll = async () => {
  const res: any = await setReadAllApi();
  if (res.code === 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.error(res.msg);
  }
}
/** 重置 */
const handleReset = () => {
  queryParamsIndex.title = '';
  queryParamsIndex.readStatus = undefined;
  loadData();
}
const queryParams = queryParamsIndex;
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })
loadData();

/** 获取阅读状态数据(通知列表) */
const loadStatus = async () => {
  queryFormItemsIndex[1]!.options = await getDictSelect("yes_no");
};
loadStatus();
</script>