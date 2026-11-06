<template>
  <div class="btn-style">
    <span class="table-tip">一行一个字段；字段的页面配置点行内齿轮图标展开后维护（默认收起）</span>
  </div>
  <div ref="wrapRef">
    <!-- table-layout fixed：输入框的固有最小宽度不再撑破容器，横向滚动条消失 -->
    <!-- row-key 按对象身份分配唯一键：新增行（无 id）也能被 Vue 稳定追踪，拖拽重排才不错位 -->
    <a-table :columns="columnsFieldConfig" :data-source="dataSource" :pagination="false" table-layout="fixed"
      :row-key="getRowKey" size="small" bordered :show-expand-column="false" v-model:expandedRowKeys="expandedRowKeys">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'dragSort'">
          <HolderOutlined class="drag-handle" title="拖动调整字段顺序" />
        </template>
        <template v-else-if="column.dataIndex === 'columnName'">
          <a-input v-model:value="record.columnName" placeholder="请输入字段名称" />
        </template>
        <template v-else-if="column.dataIndex === 'columnType'">
          <XCSelect v-model="record.columnType" apiUrl="/system/dict/select"
            :params="{ dictCode: 'db_table_column_type' }" style="width: 100%"
            :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择字段类型" />
        </template>
        <template v-else-if="column.dataIndex === 'columnComment'">
          <a-input v-model:value="record.columnComment" placeholder="请输入字段注释" />
        </template>
        <template v-else-if="column.dataIndex === 'columnLength'">
          <a-input v-model:value="record.columnLength" placeholder="长度" />
        </template>
        <template v-else-if="column.dataIndex === 'columnScale'">
          <a-input v-model:value="record.columnScale" placeholder="精度" />
        </template>
        <template v-else-if="column.dataIndex === 'columnDefault'">
          <a-input v-model:value="record.columnDefault" placeholder="默认值" />
        </template>
        <template v-else-if="column.dataIndex === 'columnIsPk'">
          <Checkbox v-model:checked="record.columnIsPk" />
        </template>
        <template v-else-if="column.dataIndex === 'columnIsNullable'">
          <Checkbox v-model:checked="record.columnIsNullable" />
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space :size="4">
            <!-- 页面配置默认隐藏，点这里在行下方展开/收起 -->
            <a-tooltip :title="isExpanded(record) ? '收起页面配置' : '页面配置'">
              <a-button type="text" size="small" :class="['row-op', { 'row-op-active': isExpanded(record) }]"
                @click="toggleExpand(record)">
                <template #icon>
                  <SettingOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <!-- 行内新增：在该行之后插入一行空数据 -->
            <a-tooltip title="在下方插入一行">
              <a-button type="text" size="small" class="row-op" @click="handleInsert(index)">
                <template #icon>
                  <PlusOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-popconfirm title="确定删除该字段吗？" placement="left" @confirm="handleDelete(index)">
              <a-tooltip title="删除">
                <a-button type="text" size="small" danger class="row-op">
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                </a-button>
              </a-tooltip>
            </a-popconfirm>
          </a-space>
        </template>
      </template>

      <!-- 行下方的页面配置：每条字段独立一块，默认收起 -->
      <template #expandedRowRender="{ record }">
        <div class="ui-config">
          <div class="ui-config-title">
            页面配置
            <span class="ui-config-sub">{{ record.columnName || '未命名字段' }}</span>
          </div>
          <a-form layout="inline" autocomplete="off" class="ui-config-form">
            <a-form-item label="查询">
              <a-checkbox v-model:checked="record.uiIsQuery" />
            </a-form-item>
            <a-form-item label="查询方式">
              <XCSelect :disabled="!record.uiIsQuery" v-model="record.uiQueryType" apiUrl="/system/dict/select"
                :params="{ dictCode: 'ui_control_type' }" style="width: 130px"
                :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="查询方式" />
            </a-form-item>
            <a-form-item label="编辑">
              <a-checkbox v-model:checked="record.uiIsEdit" />
            </a-form-item>
            <a-form-item label="必填">
              <a-checkbox v-model:checked="record.uiIsRequired" />
            </a-form-item>
            <a-form-item label="编辑方式">
              <XCSelect :disabled="!record.uiIsEdit" v-model="record.uiEditType" apiUrl="/system/dict/select"
                :params="{ dictCode: 'ui_control_type' }" style="width: 130px"
                :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="编辑方式" />
            </a-form-item>
            <a-form-item label="字典编码">
              <a-input v-model:value="record.uiDictCode" style="width: 160px" placeholder="字典编码" allowClear />
            </a-form-item>
            <a-form-item label="列表">
              <a-checkbox v-model:checked="record.uiIsList" />
            </a-form-item>
          </a-form>
        </div>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { Checkbox } from 'ant-design-vue';
import { columnsFieldConfig } from '../gentable.data';
import type { GenTableColumn } from '../gentable.types';
import XCSelect from '@/components/xuanchen/XCSelect.vue';
import { createRowKeyGetter, useDragSort } from '@/hooks/useDragSort';

const emit = defineEmits(['childData']);
const dataSource = reactive<GenTableColumn[]>([]);
/** 已展开「页面配置」的行（row-key 按对象身份分配，展开状态跟随行） */
const expandedRowKeys = ref<number[]>([]);

/** 行唯一键：按 record 对象身份分配（WeakMap 持有，不污染提交数据） */
const getRowKey = createRowKeyGetter();

const isExpanded = (record: GenTableColumn) => expandedRowKeys.value.includes(getRowKey(record));

const toggleExpand = (record: GenTableColumn) => {
  const key = getRowKey(record);
  const index = expandedRowKeys.value.indexOf(key);
  if (index >= 0) {
    expandedRowKeys.value.splice(index, 1);
  } else {
    expandedRowKeys.value.push(key);
  }
};

/** 行拖拽排序：从手柄列按住拖动，结束后同步数据源（顺序即生成页面的字段顺序） */
const wrapRef = ref<HTMLDivElement>();
const { init: initDragSort } = useDragSort({
  handle: '.drag-handle',
  draggable: 'tr.ant-table-row',
  onEnd(oldIndex, newIndex) {
    const [moved] = dataSource.splice(oldIndex, 1);
    dataSource.splice(newIndex, 0, moved);
    emit('childData', dataSource);
  },
});

/** 一行空字段（建表模式下类型必须有值，给最常用的默认值） */
const createEmptyRow = (): GenTableColumn => ({
  id: '',
  tableId: '',
  columnName: '',
  columnComment: '',
  columnType: 'varchar',
  columnLength: '64',
  columnScale: '',
  columnDefault: '',
  columnIsPk: false,
  columnIsNullable: true,
  // 新字段默认进查询/编辑/列表，生成出来的页面才不至于空着
  uiIsRequired: false,
  uiIsEdit: true,
  uiIsList: true,
  uiIsQuery: true,
  uiQueryType: 'Input',
  uiEditType: 'Input',
  uiDictCode: '',
});

const init = (data: GenTableColumn[]) => {
  expandedRowKeys.value = [];
  dataSource.splice(0, dataSource.length, ...data);
  // 没有字段时默认给一行空数据，直接就能开始填
  if (!dataSource.length) {
    dataSource.push(createEmptyRow());
  }
  // 数据行渲染后再绑定拖拽（首次渲染前 tbody 还不存在）
  nextTick(() => initDragSort(wrapRef.value?.querySelector('.ant-table-tbody')));
};

/**
 * 首次挂载兜底：弹窗第一次打开时表格是自己渲染出来的（父组件的 init 可能还拿不到 ref），
 * 这里直接补一行空数据，保证「新增」弹窗一打开就有一行可填。
 */
onMounted(() => {
  if (dataSource.length) {
    return;
  }
  dataSource.push(createEmptyRow());
  emit('childData', dataSource);
  nextTick(() => initDragSort(wrapRef.value?.querySelector('.ant-table-tbody')));
});

/** 在该行之后插入一行空数据（行内加号） */
const handleInsert = (index: number) => {
  dataSource.splice(index + 1, 0, createEmptyRow());
  emit('childData', dataSource);
};

const handleDelete = (index: number) => {
  dataSource.splice(index, 1);
  // 删空了补一行空数据，保证「新增」入口（行内加号）始终存在
  if (!dataSource.length) {
    dataSource.push(createEmptyRow());
  }
  emit('childData', dataSource);
};

defineExpose({
  init,
});
</script>

<style lang="scss" scoped>
.btn-style {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.table-tip {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.drag-handle {
  cursor: move;
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;

  &:hover {
    color: #1677ff;
  }
}

/* 行内图标操作按钮（页面配置/插入/删除），三个按钮统一样式 */
.row-op {
  color: rgba(0, 0, 0, 0.65);
}

.row-op-active {
  color: #1677ff;
}

/* 取消底部横向滚动条：table-layout fixed 下输入框不会撑破列宽，
   展开的页面配置面板偶发溢出时直接裁剪，不出滚动条 */
:deep(.ant-table-content) {
  overflow-x: hidden;
}

:deep(.drag-sort-ghost) {
  opacity: 0.4;
}

/* 表体单元格紧凑一点，字段配置一行放得下 */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 4px;
  white-space: nowrap;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 4px;
}

/* 展开行：页面配置面板 */
.ui-config {
  padding: 4px 12px 0;
}

.ui-config-title {
  margin-bottom: 8px;
  font-weight: 500;

  .ui-config-sub {
    margin-left: 8px;
    color: rgba(0, 0, 0, 0.45);
    font-weight: normal;
    font-size: 12px;
  }
}

.ui-config-form {
  display: flex;
  flex-wrap: wrap;
  row-gap: 4px;

  :deep(.ant-form-item) {
    margin-right: 20px;
    margin-bottom: 8px;
  }
}
</style>
