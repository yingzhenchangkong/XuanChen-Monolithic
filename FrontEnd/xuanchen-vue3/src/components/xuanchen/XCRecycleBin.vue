<template>
  <a-modal v-model:open="visible" :title="title" :width="width">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>

    <!-- 查询区域：不传 queryFormItems 则不渲染。
         XCQueryForm 就地修改传入对象、不派发 update:modelValue，故用 :modelValue 绑定。 -->
    <XCQueryForm v-if="queryFormItems && queryFormItems.length" :modelValue="queryParams"
      :formItems="queryFormItems" @search="loadData" @reset="handleReset" />

    <!-- 操作按钮区域：选中后出现批量操作 -->
    <div class="btn-style">
      <template v-if="state.selectedRowKeys.length > 0">
        <a-popconfirm title="确定彻底删除吗？" @confirm="handleDeleteBatch">
          <a-button :type="btnType">
            <template #icon>
              <DeleteOutlined />
            </template> 批量彻底删除
          </a-button>
        </a-popconfirm>
        <a-popconfirm title="确定还原吗？" @confirm="handleRevertBatch">
          <a-button :type="btnType">
            <template #icon>
              <RollbackOutlined />
            </template> 批量还原
          </a-button>
        </a-popconfirm>
        <a-button :type="btnType" @click="handleCancelSelect">
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

    <!-- 表格区域 -->
    <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered :rowKey="rowKey"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-popconfirm title="确定还原吗？" @confirm="() => handleRevert(record[rowKey])" placement="left">
            <a>
              <RollbackOutlined /> 还原
            </a>
          </a-popconfirm>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定彻底删除吗？" @confirm="() => handleDelete(record[rowKey])" placement="left">
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
import { computed, reactive, ref } from 'vue'

import XCQueryForm from '@/components/xuanchen/XCQueryForm.vue'
import { useList } from '@/hooks/useList'
import type { UseListUrlConfig } from '@/hooks/useList'

const props = withDefaults(defineProps<{
  /** 回收站接口地址集合（list / delete / deleteBatch / revert / revertBatch） */
  url: UseListUrlConfig
  /** 回收站表格列，含 dataIndex === 'operation' 的操作列 */
  columns: any[]
  /** 查询表单项；不传或空数组则不渲染查询区 */
  queryFormItems?: any[]
  /** 查询条件的**默认值**：组件会在内部复制一份，不会污染列表页的查询参数 */
  queryParams?: Record<string, unknown>
  /** 弹窗标题 */
  title?: string
  /** 弹窗宽度 */
  width?: number
  /** 行主键字段名 */
  rowKey?: string
  /** 批量按钮是否使用 primary 样式（用户管理沿用） */
  primaryButtons?: boolean
}>(), {
  title: '回收站',
  width: 1000,
  rowKey: 'id',
  primaryButtons: false,
})

const emit = defineEmits<{
  (e: 'childOK'): void
}>()

const btnType = computed<'primary' | 'default'>(() => (props.primaryButtons ? 'primary' : 'default'))

/**
 * 查询条件初始值快照。
 * 回收站与列表页的查询字段往往同名，若直接共用同一个 reactive 对象，弹窗内「重置」
 * 会连带清空列表页的查询条件（历史缺陷）。这里一律内部复制独立一份，重置时恢复初始值。
 */
const initialQuery = { ...(props.queryParams ?? {}) }
const queryParams = reactive<Record<string, unknown>>({ ...initialQuery })

const visible = ref(false)
/** 打开弹窗并加载数据 */
const show = () => {
  visible.value = true
  loadData()
}
const handleCancel = () => {
  visible.value = false
  emit('childOK')
}
/** 重置：恢复初始值后重新查询（XCQueryForm 会先把字段置为 undefined） */
const handleReset = () => {
  Object.assign(queryParams, initialQuery)
  loadData()
}

const {
  loadData, handleDelete, handleDeleteBatch, handleRevert, handleRevertBatch,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect,
} = useList({ url: props.url, queryParams })

// 子组件方法默认为私有
defineExpose({
  show
})
</script>
