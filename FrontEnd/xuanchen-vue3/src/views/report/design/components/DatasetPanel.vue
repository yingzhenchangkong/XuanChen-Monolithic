<template>
  <a-card size="small" title="数据集" class="dataset-panel">
    <template #extra>
      <a @click="openModal()">
        <PlusOutlined /> 新建
      </a>
    </template>

    <a-empty v-if="!datasets.length" description="暂无数据集，请新建" />
    <div v-for="ds in datasets" :key="ds.id" class="dataset-item">
      <div class="dataset-head">
        <span class="ds-name" :title="ds.datasetName">{{ ds.datasetName }}</span>
        <span class="ds-code">{{ ds.datasetCode }}</span>
      </div>
      <div class="ds-meta">
        {{ ds.dsId ? `数据源：${ds.dsName ?? ds.dsId}` : '数据源：系统主库' }}
      </div>
      <div class="ds-actions">
        <a @click="openModal(ds)">
          <EditOutlined /> 编辑
        </a>
        <a @click="handleExecute(ds)">
          <ThunderboltOutlined /> 试执行
        </a>
        <a-popconfirm title="确定删除该数据集吗？" @confirm="() => handleDelete(ds.id!)">
          <a class="danger">
            <DeleteOutlined /> 删除
          </a>
        </a-popconfirm>
      </div>
    </div>

    <!-- 新建/编辑数据集 -->
    <XCModal v-model:open="modalVisible" :title="editingId ? '编辑数据集' : '新建数据集'" :width="720"
      :show-fullscreen-btn="false" ok-text="保存" @ok="handleSave">
      <a-form layout="vertical" :model="form">
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="数据集编码" required>
              <a-input v-model:value="form.datasetCode" placeholder="如 dsOrder" :maxlength="64" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="数据集名称" required>
              <a-input v-model:value="form.datasetName" placeholder="如 订单明细" :maxlength="100" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="数据源">
              <a-select v-model:value="form.dsId" :options="dsOptions" allow-clear placeholder="默认系统主库" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="查询 SQL（仅 SELECT，参数用 #{param} 占位）" required>
          <a-textarea v-model:value="form.sqlText" :rows="8" placeholder="SELECT order_no, cust_name, amount&#10;FROM biz_order&#10;WHERE create_time >= #{startDate}" />
        </a-form-item>
        <a-form-item label="参数定义 JSON（可选，数组）">
          <a-textarea v-model:value="form.paramsJson" :rows="4"
            placeholder='[{"code":"startDate","label":"开始日期","type":"date"}]' />
        </a-form-item>
      </a-form>
    </XCModal>

    <!-- 试执行结果 -->
    <a-modal v-model:open="resultVisible" :title="`试执行结果：${resultCode}`" :width="860" :footer="null"
      destroy-on-close>
      <a-alert v-if="resultError" type="error" show-icon :message="resultError" />
      <template v-else>
        <p style="margin: 4px 0">共 {{ resultData?.total ?? 0 }} 行，预览前 50 行。字段：
          <a-tag v-for="c in resultData?.columns ?? []" :key="c" style="margin: 2px">{{ c }}</a-tag>
        </p>
        <a-table size="small" :columns="resultColumns" :data-source="resultRows" :pagination="false"
          :scroll="{ x: 'max-content', y: 360 }" row-key="__row__" bordered />
      </template>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { EditOutlined, DeleteOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons-vue';

import XCModal from '@/components/xuanchen/XCModal.vue';
import {
  getDatasetListApi,
  saveOrUpdateDataset,
  deleteDatasetApi,
  executeDatasetApi,
  getDatasourceListApi,
} from '../../report.api';
import type { DatasetRecord, DatasourceRecord, DatasetResult } from '../../report.types';

const props = defineProps<{
  /** 所属模板ID（数据集挂在模板上） */
  templateId: string;
  datasets: DatasetRecord[];
}>();

const emit = defineEmits<{
  (e: 'datasetsChange', list: DatasetRecord[]): void;
  (e: 'dataExecuted', payload: { datasetCode: string; result: DatasetResult }): void;
}>();

const modalVisible = ref(false);
const editingId = ref('');
const form = reactive({
  datasetCode: '',
  datasetName: '',
  dsId: undefined as string | undefined,
  sqlText: '',
  paramsJson: '',
});

const dsList = ref<DatasourceRecord[]>([]);
const dsOptions = computed(() =>
  dsList.value.map((d) => ({ label: `${d.dsName}（${d.dsType}）`, value: String(d.id) }))
);

const resultVisible = ref(false);
const resultCode = ref('');
const resultError = ref('');
const resultData = ref<DatasetResult | null>(null);
const resultColumns = computed(() =>
  (resultData.value?.columns ?? []).map((c) => ({ title: c, dataIndex: c, key: c, width: 140 }))
);
const resultRows = computed(() =>
  (resultData.value?.rows ?? []).slice(0, 50).map((r, i) => ({ ...r, __row__: i }))
);

const openModal = async (ds?: DatasetRecord) => {
  modalVisible.value = true;
  editingId.value = ds?.id ?? '';
  form.datasetCode = ds?.datasetCode ?? '';
  form.datasetName = ds?.datasetName ?? '';
  form.dsId = ds?.dsId ? String(ds.dsId) : undefined;
  form.sqlText = ds?.sqlText ?? '';
  form.paramsJson = ds?.paramsJson ?? '';
  if (!dsList.value.length) {
    try {
      dsList.value = await getDatasourceListApi();
    } catch {
      // 数据源加载失败不阻塞，仍可用系统主库
    }
  }
};

const handleSave = async () => {
  if (!form.datasetCode.trim() || !form.datasetName.trim() || !form.sqlText.trim()) {
    message.warning('编码/名称/SQL 均不能为空！');
    return Promise.reject();
  }
  const res = await saveOrUpdateDataset({
    id: editingId.value || undefined,
    templateId: props.templateId,
    datasetCode: form.datasetCode.trim(),
    datasetName: form.datasetName.trim(),
    dsId: form.dsId || null,
    sqlText: form.sqlText,
    paramsJson: form.paramsJson.trim() || undefined,
  });
  if (res.code !== 200) {
    message.error(res.msg || '保存失败');
    return Promise.reject();
  }
  message.success('保存成功！');
  modalVisible.value = false;
  await reload();
};

const handleDelete = async (id: string) => {
  const res = await deleteDatasetApi(id);
  if (res.code !== 200) {
    message.error(res.msg || '删除失败');
    return;
  }
  message.success('删除成功！');
  await reload();
};

const handleExecute = async (ds: DatasetRecord) => {
  if (!ds.id) return;
  resultVisible.value = true;
  resultCode.value = `${ds.datasetName}（${ds.datasetCode}）`;
  resultError.value = '';
  resultData.value = null;
  const res = await executeDatasetApi(ds.id, {});
  if (res.code !== 200 || !res.data) {
    resultError.value = res.msg || '执行失败';
    return;
  }
  resultData.value = res.data;
  emit('dataExecuted', { datasetCode: String(ds.datasetCode), result: res.data });
};

const reload = async () => {
  const list = await getDatasetListApi(props.templateId);
  emit('datasetsChange', list);
};

onMounted(() => {
  // 数据集挂在模板上，进入设计器即加载本模板的数据集
  reload().catch(() => undefined);
});

defineExpose({ reload });
</script>

<style scoped>
.dataset-item {
  padding: 6px 0;
  border-bottom: 1px dashed rgba(128, 128, 128, 0.25);
}

.dataset-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ds-name {
  font-weight: 500;
}

.ds-code {
  font-size: 12px;
  opacity: 0.6;
}

.ds-meta {
  font-size: 12px;
  opacity: 0.7;
  margin: 2px 0;
}

.ds-actions {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.danger {
  color: #a32d2d;
}
</style>
