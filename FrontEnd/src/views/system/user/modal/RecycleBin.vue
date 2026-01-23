<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="1000">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParams" :formItems="queryFormItemsRcvBin" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <template v-if="state.selectedRowKeys.length > 0">
        <a-popconfirm title="确定彻底删除吗？" @confirm="handledeleteBatch">
          <a-button type="primary">
            <template #icon>
              <DeleteOutlined />
            </template> 批量彻底删除
          </a-button>
        </a-popconfirm>
        <a-popconfirm title="确定还原吗？" @confirm="handleRevertBatch">
          <a-button type="primary">
            <template #icon>
              <RollbackOutlined />
            </template> 批量还原
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleCancelSelect">
          <template #icon>
            <UndoOutlined />
          </template> 取消选择
        </a-button>
        <a-tag color="processing" style="float: right;">
          <template #icon>
            <CheckSquareOutlined />
          </template>
          {{ `已选择 ${state.selectedRowKeys.length} 条` }}
        </a-tag>
      </template>
    </div>
    <a-table :dataSource="dataSource" :columns="columnsRcvBin" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-popconfirm title="确定还原吗？" @confirm="() => handleRevert(record.id)" placement="left">
            <a>
              <RollbackOutlined /> 还原
            </a>
          </a-popconfirm>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定彻底删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
            <a>
              <DeleteOutlined /> 彻底删除
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { useList } from '@/hooks/useList'
import { reactive, ref } from 'vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { queryFormItemsRcvBin, columnsRcvBin } from '../user.data';
import { UserApiUrl } from '../user.api';

defineProps({
  modalTitle: {
    type: String,
    default: '回收站'
  }
})
const emit = defineEmits(['childOK']);
/** url */
const url = {
  list: UserApiUrl.REC_BIN_LIST,
  delete: UserApiUrl.REC_BIN_DELETE,
  deleteBatch: UserApiUrl.REC_BIN_DELETE_BATCH,
  revert: UserApiUrl.REC_BIN_REVERT,
  revertBatch: UserApiUrl.REC_BIN_REVERT_BATCH,
}
/** 查询参数 */
const queryParams = reactive({
  userName: '',
  nickName: '',
})

/** 重置 */
const handleReset = () => {
  queryParams.userName = ''
  queryParams.nickName = ''
  loadData()
}
const visible = ref(false);
const show = () => {
  visible.value = true;
  loadData()
}
const handleCancel = () => {
  visible.value = false;
  emit('childOK');
}
const {
  loadData,
  handleDelete, handledeleteBatch,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect, handleRevert, handleRevertBatch,
} = useList({ url, queryParams })

//子组件方法默认为私有
defineExpose({
  show
})
</script>