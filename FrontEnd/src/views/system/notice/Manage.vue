<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParamsManage" :formItems="queryFormItemsManage" @search="handleSearch"
      @reset="handleReset" />
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columnsManage" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange, type: 'radio' }" bordered
      rowKey="id" size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleDetail(record)">
            <FileTextOutlined /> 详细
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定撤销吗？" @confirm="() => handleCancel(record.id)" placement="left">
            <a>
              <RollbackOutlined /> 撤销
            </a>
          </a-popconfirm>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定恢复吗？" @confirm="() => handleRecover(record.id)" placement="left">
            <a>
              <RollbackOutlined /> 恢复
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
    <!-- 子项 -->
    <a-tabs defaultActiveKey="1">
      <a-tab-pane tab="明细" key="1">
        <ManageStatus :mainId="mainId" />
      </a-tab-pane>
    </a-tabs>
    <!--弹窗区域-->
    <DetailManage ref="refDetail" />
  </a-card>
</template>

<script lang="ts" setup>
import { useList } from '@/hooks/useList'
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import DetailManage from './modal/DetailManage.vue';
import ManageStatus from './ManageStatus.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { queryParamsManage, queryFormItemsManage, columnsManage } from './notice.data';
import { cancelApi, recoverApi, NoticeApiUrl } from './notice.api';
const mainId = ref('');

const url = {
  list: NoticeApiUrl.LIST_MANAGE,
}
/** 搜索 */
const handleSearch = () => {
  loadData();
  mainId.value = '';
}
/** 重置 */
const handleReset = () => {
  queryParamsManage.title = '';
  mainId.value = '';
  loadData();
}

const refDetail = ref();
/** 详细 */
const handleDetail = (record: any) => {
  refDetail.value.detail(record);
}
/** 撤销 */
const handleCancel = async (id: string) => {
  const res: any = await cancelApi(id);
  if (res.code == 200) {
    message.success(res.msg);
    handleReset();
  } else {
    message.warning(res.msg);
  }
}

/** 恢复 */
const handleRecover = async (id: string) => {
  const res: any = await recoverApi(id);
  if (res.code == 200) {
    message.success(res.msg);
    handleReset();
  } else {
    message.warning(res.msg);
  }
}

/** 选择行改变时 改变已选择行的数组 */
const onSelectChange = (selectedRowKeys: any, selectionRows: any) => {
  state.selectedRowKeys = selectedRowKeys;
  mainId.value = selectionRows[0]['id'];
};
const queryParams = queryParamsManage;
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state
} = useList({ url, queryParams })
loadData();
onMounted(() => {
  ipagination.pageSize = 5;
});
</script>