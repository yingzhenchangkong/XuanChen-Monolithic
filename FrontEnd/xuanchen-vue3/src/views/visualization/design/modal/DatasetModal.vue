<template>
  <!-- 数据集管理：左侧列表 + 右侧编辑（编码/名称/SQL/试执行），供组件按 datasetKey 引用 -->
  <XCModal v-model:open="visible" title="大屏数据集" :width="980" hide-footer :show-fullscreen-btn="false">
    <div class="ds-modal">
      <div class="ds-list">
        <div class="ds-list-head">
          <span>数据集（{{ list.length }}）</span>
          <a-button size="small" type="primary" ghost @click="startAdd">
            <template #icon>
              <PlusOutlined />
            </template>新增
          </a-button>
        </div>
        <div class="ds-list-body">
          <div v-for="item in list" :key="item.id" class="ds-item" :class="{ 'is-active': item.id === current.id }"
            @click="selectItem(item)">
            <div class="ds-item-name">{{ item.datasetName }}</div>
            <div class="ds-item-key">{{ item.datasetKey }}</div>
          </div>
          <a-empty v-if="!list.length" description="暂无数据集" :image="simpleEmpty" />
        </div>
      </div>

      <div class="ds-form">
        <a-form layout="vertical" size="small" :model="current">
          <a-row :gutter="10">
            <a-col :span="12">
              <a-form-item label="数据集编码（组件引用，英文/数字/下划线）" required>
                <a-input v-model:value="current.datasetKey" placeholder="如 orderTrend" :maxlength="64" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="数据集名称" required>
                <a-input v-model:value="current.datasetName" placeholder="如 订单趋势" :maxlength="100" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="数据源">
            <a-select v-model:value="current.dsId" placeholder="默认系统主库" allowClear>
              <a-select-option v-for="d in dsList" :key="d.id" :value="d.id">
                {{ d.dsName }}（{{ d.dsType }}）
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="查询 SQL（只允许单条 SELECT，服务端会做安全校验并限制返回行数）" required>
            <a-textarea v-model:value="current.sqlText" :rows="6" placeholder="SELECT stat_date, SUM(amount) AS amount FROM ... GROUP BY stat_date"
              style="font-family: Consolas, Monaco, monospace;" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="current.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="saving" @click="handleSave">
                <template #icon>
                  <SaveOutlined />
                </template>保存
              </a-button>
              <a-button :loading="previewing" @click="handlePreview">
                <template #icon>
                  <PlayCircleOutlined />
                </template>试执行
              </a-button>
              <a-popconfirm v-if="current.id" title="确定删除该数据集吗？" placement="left" @confirm="handleDelete">
                <a-button danger>
                  <template #icon>
                    <DeleteOutlined />
                  </template>删除
                </a-button>
              </a-popconfirm>
              <a-button v-if="current.id" @click="startAdd">新建数据集</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <div v-if="previewColumns.length" class="ds-preview">
          <div class="ds-preview-title">试执行结果（前 {{ previewRows.length }} 行）</div>
          <div class="ds-preview-table">
            <table>
              <thead>
                <tr>
                  <th v-for="col in previewColumns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in previewRows" :key="index">
                  <td v-for="col in previewColumns" :key="col">{{ row[col] }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  DeleteOutlined, PlayCircleOutlined, PlusOutlined, SaveOutlined,
} from '@ant-design/icons-vue';

import XCModal from '@/components/xuanchen/XCModal.vue';
import {
  deleteScreenDatasetApi, getScreenDatasourceListApi, getScreenDatasetListApi,
  previewScreenDatasetApi, saveOrUpdateScreenDataset,
} from '../../visualization.api';
import type { ScreenDataset, ScreenDatasource } from '../../visualization.types';

const props = defineProps<{
  open: boolean;
  screenId: string;
  datasets: ScreenDataset[];
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'change', datasets: ScreenDataset[]): void;
  (e: 'data-loaded', payload: { datasetKey: string; columns: string[]; rows: Record<string, unknown>[] }): void;
}>();

const visible = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

/** 本地副本：保存后立即刷新列表，不必等父组件回传 */
const list = ref<ScreenDataset[]>([]);
const current = reactive<ScreenDataset>({ screenId: props.screenId, datasetKey: '', datasetName: '', dsId: null, sqlText: '', remark: '' });
const dsList = ref<ScreenDatasource[]>([]);
const datasourceLoaded = ref(false);
const saving = ref(false);
const previewing = ref(false);
const previewColumns = ref<string[]>([]);
const previewRows = ref<Record<string, unknown>[]>([]);
const simpleEmpty = 'https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg';

watch(
  () => [props.open, props.datasets],
  async () => {
    if (props.open) {
      list.value = [...(props.datasets ?? [])];
      if (!datasourceLoaded.value) {
        try {
          const res = await getScreenDatasourceListApi();
          if (res.code === 200) dsList.value = res.data ?? [];
        } catch { /* 数据源加载失败不阻塞 */ }
        datasourceLoaded.value = true;
      }
      const first = list.value[0];
      if (first) {
        selectItem(first);
      } else {
        startAdd();
      }
    }
  },
  { immediate: true, deep: true },
);

const selectItem = (item: ScreenDataset) => {
  Object.assign(current, {
    id: item.id,
    screenId: item.screenId ?? props.screenId,
    datasetKey: item.datasetKey ?? '',
    datasetName: item.datasetName ?? '',
    dsId: item.dsId ?? null,
    sqlText: item.sqlText ?? '',
    remark: item.remark ?? '',
  });
  previewColumns.value = [];
  previewRows.value = [];
};

const startAdd = () => {
  Object.assign(current, {
    id: undefined,
    screenId: props.screenId,
    datasetKey: '',
    datasetName: '',
    dsId: null,
    sqlText: '',
    remark: '',
  });
  previewColumns.value = [];
  previewRows.value = [];
};

const reload = async () => {
  emit('change', [...list.value]);
};

const handleSave = async () => {
  const key = current.datasetKey?.trim() ?? '';
  if (!key) {
    message.warning('请输入数据集编码');
    return;
  }
  if (!current.datasetName?.trim()) {
    message.warning('请输入数据集名称');
    return;
  }
  if (!current.sqlText?.trim()) {
    message.warning('请输入查询 SQL');
    return;
  }
  saving.value = true;
  try {
    const res = await saveOrUpdateScreenDataset({
      id: current.id,
      screenId: props.screenId,
      datasetKey: key,
      datasetName: current.datasetName.trim(),
      dsId: current.dsId || null,
      sqlText: current.sqlText.trim(),
      remark: current.remark,
    });
    if (res.code !== 200) {
      message.error(res.msg || '保存失败');
      return;
    }
    message.success('保存成功！');
    // 用保存后的内容回填列表：新增时后端生成 id，重新拉取最稳妥
    list.value = await getScreenDatasetListApi(props.screenId);
    const saved = list.value.find((item) => item.datasetKey === key);
    if (saved) selectItem(saved);
    await reload();
  } catch (e) {
    message.error((e as Error).message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const handlePreview = async () => {
  if (!current.sqlText?.trim()) {
    message.warning('请先填写查询 SQL');
    return;
  }
  previewing.value = true;
  try {
    const res = await previewScreenDatasetApi({
      sql: current.sqlText.trim(),
      dsId: current.dsId || null,
    });
    if (res.code !== 200 || !res.data) {
      message.error(res.msg || '试执行失败');
      return;
    }
    previewColumns.value = res.data.columns ?? [];
    previewRows.value = (res.data.rows ?? []).slice(0, 20);
    message.success(`试执行成功，共 ${res.data.total ?? 0} 行`);
    if (current.datasetKey) {
      emit('data-loaded', {
        datasetKey: current.datasetKey.trim(),
        columns: res.data.columns ?? [],
        rows: res.data.rows ?? [],
      });
    }
  } catch (e) {
    message.error((e as Error).message || '试执行失败');
  } finally {
    previewing.value = false;
  }
};

const handleDelete = async () => {
  if (!current.id) return;
  const res = await deleteScreenDatasetApi(current.id);
  if (res.code !== 200) {
    message.error(res.msg || '删除失败');
    return;
  }
  message.success('删除成功！');
  list.value = list.value.filter((item) => item.id !== current.id);
  if (list.value[0]) selectItem(list.value[0]);
  else startAdd();
  await reload();
};
</script>

<style scoped>
.ds-modal {
  display: flex;
  gap: 12px;
  height: 540px;
}

.ds-list {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.ds-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  font-size: 13px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.ds-list-body {
  flex: 1;
  overflow: auto;
  padding: 6px;
}

.ds-item {
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid transparent;
}

.ds-item:hover {
  background: #f5f8ff;
}

.ds-item.is-active {
  background: #e6f4ff;
  border-color: #91caff;
}

.ds-item-name {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ds-item-key {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.ds-form {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
}

.ds-preview {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.ds-preview-title {
  padding: 6px 10px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.ds-preview-table {
  max-height: 180px;
  overflow: auto;
}

.ds-preview-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.ds-preview-table th,
.ds-preview-table td {
  padding: 4px 8px;
  border-bottom: 1px solid #f5f5f5;
  text-align: left;
  white-space: nowrap;
}

.ds-preview-table th {
  background: #fafafa;
  font-weight: 500;
}
</style>
