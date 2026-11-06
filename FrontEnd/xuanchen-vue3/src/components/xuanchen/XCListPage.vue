<template>
  <a-card :class="cardClass">
    <!-- 查询区域：不传 queryFormItems 或传空数组则不渲染。
         注意：XCQueryForm 是「就地修改传入对象」的实现，不派发 update:modelValue，
         因此用 :modelValue 绑定（而非 v-model）以避免无意义的 const 重赋值。 -->
    <XCQueryForm v-if="queryFormItems && queryFormItems.length" :modelValue="queryParams"
      :formItems="queryFormItems" @search="onSearch" @reset="onReset" />

    <!-- 操作按钮区域 -->
    <div class="btn-style">
      <!-- 页面自定义按钮（位于标准按钮之前），如「全部标记为已读」 -->
      <slot name="toolbar-prepend" :state="state" :load-data="loadData" />

      <a-button v-if="btn.add" type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>

      <a-upload v-if="btn.import" name="file" :customRequest="handleImport" :showUploadList="false">
        <a-button>
          <template #icon>
            <UploadOutlined />
          </template>导入
        </a-button>
      </a-upload>

      <a-button v-if="btn.export" @click="handleExport(exportTitle)">
        <template #icon>
          <DownloadOutlined />
        </template>导出
      </a-button>

      <a-button v-if="btn.recycleBin" @click="emit('recycle-bin')">
        <template #icon>
          <RestOutlined />
        </template>回收站
      </a-button>

      <template v-if="state.selectedRowKeys.length > 0">
        <a-popconfirm v-if="btn.deleteBatch" :title="deleteConfirmText" @confirm="handleDeleteBatch">
          <a-button>
            <template #icon>
              <DeleteOutlined />
            </template>批量删除
          </a-button>
        </a-popconfirm>
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

      <!-- 页面自定义按钮（位于标准按钮之后） -->
      <slot name="toolbar" :state="state" :load-data="loadData" />
    </div>

    <!-- 表格区域 -->
    <a-table :dataSource="dataSource" :columns="columns" :pagination="pagination ? ipagination : false"
      :loading="loading" :row-selection="rowSelection" :rowKey="rowKey" bordered size="small" v-bind="tableBind"
      @change="onTableChange">
      <template #bodyCell="cell">
        <template v-if="cell.column.dataIndex === 'operation'">
          <slot name="operation" v-bind="cell" :data-source="dataSource" :state="state" :handle-edit="handleEdit"
            :handle-delete="handleDelete" :load-data="loadData">
            <!-- 默认操作列：编辑 + 删除 -->
            <a @click="handleEdit(cell.record)">
              <EditOutlined /> 编辑
            </a>
            <a-divider type="vertical"></a-divider>
            <a-popconfirm title="确定删除吗？" placement="left" @confirm="handleDelete(cell.record[rowKey])">
              <a>
                <DeleteOutlined /> 删除
              </a>
            </a-popconfirm>
          </slot>
        </template>
        <template v-else-if="cell.column.dataIndex">
          <!-- 其余列全部透传；页面未提供 #bodyCell 时自动回退为默认文本渲染。
               注意必须限定 dataIndex 非空：key='rowIndex' 这类纯渲染列没有 dataIndex，
               antd 会把「整行 record 对象」当作 text 传入插槽，透传给页面后
               {{ text }} 兜底会把整个 JSON 渲染进单元格；这里不透传，
               让 a-table 回退到列默认渲染（customRender 的行序号） -->
          <slot name="bodyCell" v-bind="cell" :data-source="dataSource" :state="state"
            :handle-status-change="handleStatusChange" />
        </template>
        <template v-else />
      </template>
    </a-table>

    <!-- 表格之后的附加内容（如字典的明细 tab、子表等） -->
    <slot name="footer" :state="state" :load-data="loadData" />

    <!-- 弹窗区域：页面用函数 ref 绑定 Operation，组件内部即可调用 add() / edit() -->
    <slot name="modals" :operation-title="operationTitle" :set-operation-ref="setOperationRef" :state="state"
      :load-data="loadData" />
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import XCQueryForm from '@/components/xuanchen/XCQueryForm.vue'
import { useList } from '@/hooks/useList'
import { useStatusToggle } from '@/hooks/useStatusToggle'
import type { UseListUrlConfig } from '@/hooks/useList'
import type { Result } from '@/types/api'

/** 表格行键 */
type RowKey = string | number

/** 标准工具栏按钮开关 */
export interface ListPageButtons {
  add?: boolean
  import?: boolean
  export?: boolean
  recycleBin?: boolean
  deleteBatch?: boolean
}

/** 操作弹窗对外暴露的方法约定 */
export interface OperationExposed {
  add: () => void
  edit: (record: any) => void
}

const props = withDefaults(defineProps<{
  /** 列表相关接口地址集合（list / delete / deleteBatch / exportExcel / importExcel） */
  url: UseListUrlConfig
  /** 表格列定义，含 dataIndex === 'operation' 的操作列 */
  columns: any[]
  /** 查询表单项；不传或空数组则不渲染查询区 */
  queryFormItems?: any[]
  /** 查询参数：与页面共用同一份 reactive 对象，便于页面自定义 search 逻辑 */
  queryParams?: Record<string, unknown>
  /** 行主键字段名 */
  rowKey?: string
  /** 选择列：checkbox / radio / false（不显示） */
  selection?: boolean | 'checkbox' | 'radio'
  /** 是否分页 */
  pagination?: boolean
  /** 初始每页条数 */
  defaultPageSize?: number
  /** 标准工具栏按钮开关 */
  buttons?: ListPageButtons
  /** 导出文件名（不含后缀） */
  exportTitle?: string
  /** 批量删除二次确认文案 */
  deleteConfirmText?: string
  /** 状态切换接口；配置后可通过插槽拿到 handleStatusChange */
  statusApi?: (id: string, status: number) => Promise<Result>
  /** 状态值换算，默认 1 ↔ 0（用户管理为 1 ↔ 2） */
  statusCompute?: (current: number) => number
  /** 透传给 a-table 的额外属性（scroll / expandedRowKeys / expandedRowsChange 等） */
  tableProps?: Record<string, unknown>
  /** a-card 上额外的 class */
  cardClass?: string
}>(), {
  rowKey: 'id',
  selection: 'checkbox',
  pagination: true,
  defaultPageSize: 10,
  buttons: () => ({}),
  exportTitle: '数据',
  deleteConfirmText: '确定删除吗？',
  tableProps: () => ({}),
  cardClass: '',
})

const emit = defineEmits<{
  (e: 'search'): void
  (e: 'reset'): void
  (e: 'add'): void
  (e: 'edit', record: Record<string, unknown>): void
  (e: 'selection-change', keys: RowKey[], rows: Record<string, unknown>[]): void
  (e: 'table-change', pagination: { current: number; pageSize: number }): void
  (e: 'recycle-bin'): void
  (e: 'status-change', payload: { record: Record<string, unknown>; index: number; status: number }): void
}>()

/** 标准按钮：默认全部关闭，由页面按需开启 */
const btn = computed<ListPageButtons>(() => ({
  add: false, import: false, export: false, recycleBin: false, deleteBatch: false,
  ...props.buttons,
}))

/** 查询参数：优先复用页面传入的 reactive 对象 */
const queryParams = props.queryParams ?? reactive<Record<string, unknown>>({})
/**
 * 查询字段初始值快照。
 * XCQueryForm 重置时会先把字段统一置为 undefined，而各页面原先的重置逻辑是把字段还原为 ''，
 * 这里按初始值恢复，保持与原实现一致的语义（避免 undefined 与 '' 带来的后端取值差异）。
 */
const initialQuery = { ...queryParams }

const {
  loadData, handleDelete, handleDeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect,
} = useList({ url: props.url, queryParams })

/** 操作弹窗：由页面通过 #modals 插槽用函数 ref 绑定 */
const operationTitle = ref('')
const refOperation = ref<OperationExposed | null>(null)
const setOperationRef = (el: unknown) => {
  refOperation.value = el as OperationExposed | null
}

/**
 * 调用操作弹窗暴露的方法。
 * 弹窗组件必须 defineExpose({ add, edit })；未暴露时只打印告警而不是抛 TypeError，
 * 避免用户看到一个无从定位的「xxx is not a function」。
 */
const invokeOperation = (name: keyof OperationExposed, ...args: any[]) => {
  const operation = refOperation.value as Record<string, any> | null
  const fn = operation?.[name]
  if (typeof fn !== 'function') {
    console.warn(
      `[XCListPage] 操作弹窗未暴露 ${name}()，请在 #modals 插槽中的组件里 defineExpose({ add, edit })`,
      operation,
    )
    return
  }
  fn.apply(operation, args)
}

/** 新增：置标题后调用弹窗的 add() */
const handleAdd = () => {
  operationTitle.value = '新增'
  invokeOperation('add')
  emit('add')
}
/** 编辑：置标题后调用弹窗的 edit() */
const handleEdit = (record: any) => {
  operationTitle.value = '编辑'
  invokeOperation('edit', record)
  emit('edit', record)
}

/** 查询：先让页面处理（如 timeRange 拆解），再请求 */
const onSearch = () => {
  emit('search')
  loadData()
}
/** 重置：把查询字段还原为初始值（XCQueryForm 已先把字段置为 undefined），再重新请求 */
const onReset = () => {
  Object.assign(queryParams, initialQuery)
  emit('reset')
  loadData()
}

/**
 * 表格横向滚动：页面未显式传 scroll 时，按列宽合计作为表格宽度，
 * 让横向滚动条出现在表格内部（底部），而不是把整个页面撑出横向滚动。
 */
const tableScroll = computed(() => {
  if (props.tableProps?.scroll) {
    return props.tableProps.scroll
  }
  const totalWidth = props.columns.reduce((sum: number, col: any) => sum + (Number(col?.width) || 0), 0)
  return totalWidth > 0 ? { x: totalWidth } : undefined
})

/** a-table 实际绑定属性：tableProps 透传，scroll 兜底 */
const tableBind = computed(() => ({ ...props.tableProps, scroll: tableScroll.value }))

/** 选择列配置 */
const rowSelection = computed(() => {
  if (props.selection === false) return undefined
  return {
    selectedRowKeys: state.selectedRowKeys,
    onChange: (keys: RowKey[], rows: Record<string, unknown>[]) => {
      onSelectChange(keys, rows)
      emit('selection-change', keys, rows)
    },
    ...(props.selection === 'radio' ? { type: 'radio' as const } : {}),
  }
})

/** 分页 / 排序变化 */
const onTableChange = (pagination: { current: number; pageSize: number }) => {
  handleTableChange(pagination)
  emit('table-change', pagination)
}

/**
 * 状态列切换：复用 useStatusToggle（乐观更新 + 失败回滚）。
 * 统一了原本在 6 个页面里逐字重复的实现。
 */
const { handleStatusChange: toggleStatus } = useStatusToggle({
  dataSource,
  changeApi: (id, status) => props.statusApi
    ? props.statusApi(id, status)
    : Promise.reject(new Error('未配置 statusApi')),
  rowKey: props.rowKey,
  compute: props.statusCompute,
  onSuccess: ({ record, index, status }) => emit('status-change', { record, index, status }),
})

const handleStatusChange = async (record: any, index: number) => {
  if (!props.statusApi) {
    message.warning('未配置 statusApi，无法切换状态')
    return
  }
  await toggleStatus(record, index)
}

onMounted(() => {
  // 先对齐每页条数再首次请求，避免「首屏按 10 条请求、之后才改成 5 条」的错位
  if (props.defaultPageSize !== 10) {
    ipagination.pageSize = props.defaultPageSize
    queryParams.pageSize = props.defaultPageSize
  }
  loadData()
})

defineExpose({
  // 行为
  loadData, handleAdd, handleEdit, handleDelete, handleDeleteBatch, handleImport, handleExport,
  handleStatusChange, handleCancelSelect, onSelectChange,
  // 状态
  dataSource, loading, ipagination, state, queryParams, operationTitle, refOperation,
})
</script>
