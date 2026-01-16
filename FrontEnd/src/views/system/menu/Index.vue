<template>
  <a-card>
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
      <a-button @click="expandCollapse">展开/折叠</a-button>
      <template v-if="state.selectedRowKeys.length > 0">
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
    <a-table :dataSource="dataSource" :columns="columns" :pagination="false" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange, type: 'radio' }" bordered
      rowKey="id" size="small" :expandedRowKeys="expandedRowKeys" @expandedRowsChange="expandedRowsChange"
      @change="handleTableChange">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a href="javascript:;" @click="handleAddSub(record.id)">
            <PlusSquareOutlined /> 新增
          </a>
          <a-divider type="vertical"></a-divider>
          <a href="javascript:;" @click="handleEdit(record)">
            <EditOutlined /> 编辑
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
            <a href="javascript:;">
              <DeleteOutlined /> 删除
            </a>
          </a-popconfirm>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === true ? 'green' : 'volcano'">
            {{ record.status === true ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'title'">
          {{ record.meta.title }}
        </template>
        <template v-else-if="column.dataIndex === 'icon'">
          <component :is="record.meta.icon" v-if="record.meta.icon" />
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
  </a-card>
</template>

<script lang="ts" setup>
import { useList } from '@/hooks/useList'
import { ref } from 'vue';
import Operation from './modal/Operation.vue';

import { columns } from './menu.data';
import { MenuApiUrl } from './menu.api';

/** url */
const url = {
  list: MenuApiUrl.LIST,
  delete: MenuApiUrl.DELETE,
}
/** 添加子菜单 */
const handleAddSub = (parentId: string) => {
  operationTitle.value = '新增'
  refOperation.value.addSub(parentId)
}

const expandedRowKeys = ref([])
const expandCollapse = () => {
  if (expandedRowKeys.value.length === 0) {
    expandAll()
  } else {
    collapseAll()
  }
}
//展开全部
const expandAll = () => {
  let expandArr: any = [];
  dataSource.value.forEach((item: any) => {
    expandArr.push(item.id);
  })
  expandedRowKeys.value = expandArr; //给expandedRowKeys赋值为id组成的数组
}
//折叠全部
const collapseAll = () => {
  expandedRowKeys.value = [];//清空数组，表格行全部折叠
}
// 表格行操作（展开折叠）
const expandedRowsChange = (expandedRows: any) => {
  expandedRowKeys.value = expandedRows; // 点击树形表格内的展开折叠图标时
};

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete,
  dataSource, loading, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url })
loadData()
</script>