<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="接口管理">
    <template #toolbar-prepend>
      <a-button type="primary" @click="openCreateModal">
        <template #icon>
          <PlusOutlined />
        </template>新建接口
      </a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="statusColor((record as ApiRecord).status)">{{ statusText((record as ApiRecord).status) }}</a-tag>
    </template>

    <template #operation="{ record }">
      <a @click="handleEdit(record as ApiRecord)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a v-if="(record as ApiRecord).status !== 1" @click="handlePublish(record as ApiRecord)">
        <CheckCircleOutlined /> 发布
      </a>
      <a v-else @click="handleStop(record as ApiRecord)">
        <PauseCircleOutlined /> 停用
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="openScheduleModal(record as ApiRecord)">
        <ClockCircleOutlined /> 定时
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除后不可恢复，确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>
  </XCListPage>

  <!-- 新建弹窗：编码+命名后落库再进编辑器 -->
  <a-modal v-model:open="createOpen" title="新建接口" :confirm-loading="creating" @ok="handleCreate">
    <a-form layout="vertical">
      <a-form-item label="接口编码" required help="访问路径 /open/api/{接口编码}，字母开头，仅字母数字-_">
        <a-input v-model:value="newCode" placeholder="如 weather_query" :maxlength="64" />
      </a-form-item>
      <a-form-item label="接口名称" required>
        <a-input v-model:value="newName" placeholder="请输入接口名称" :maxlength="100" @press-enter="handleCreate" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 定时采集弹窗 -->
  <a-modal v-model:open="scheduleOpen" title="定时采集配置" :footer="null" width="720px">
    <a-alert v-if="scheduleApi" class="schedule-tip" :message="`接口：${scheduleApi.apiName}（${scheduleApi.apiCode}）`" type="info" show-icon />
    <a-form layout="inline" class="schedule-form">
      <a-form-item label="cron 表达式" required help="Spring 6 位格式，如 0 0/30 * * * *（每30分钟）">
        <a-input v-model:value="schedCron" placeholder="0 0/30 * * * *" style="width: 180px" />
      </a-form-item>
      <a-form-item label="启用">
        <a-switch v-model:checked="schedEnabled" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="schedSaving" @click="handleScheduleSave">保存</a-button>
      </a-form-item>
    </a-form>
    <a-table :data-source="scheduleList" :columns="scheduleColumns" row-key="id" size="small" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-tag :color="(record as ApiScheduleRecord).enabled === 1 ? 'green' : 'default'">
            {{ (record as ApiScheduleRecord).enabled === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-popconfirm title="删除该定时配置？" @confirm="() => handleScheduleDelete((record as ApiScheduleRecord).id)">
            <a>删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PauseCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import {
  deleteApiApi,
  deleteScheduleApi,
  getScheduleListApi,
  getApiListUrl,
  publishApiApi,
  saveApiApi,
  saveScheduleApi,
  stopApiApi,
} from '../designer.api';
import type { ApiRecord, ApiScheduleRecord } from '../designer.api';

const router = useRouter();
const listRef = ref();

const url = {
  list: getApiListUrl,
};

const queryParams = reactive<Record<string, unknown>>({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const queryFormItems = reactive([
  {
    name: 'keyword',
    label: '关键字',
    type: 'input' as const,
    placeholder: '按名称/编码搜索',
  },
]);

const statusText = (s: number) => (s === 1 ? '已发布' : s === 2 ? '已停用' : '草稿');
const statusColor = (s: number) => (s === 1 ? 'green' : s === 2 ? 'red' : 'default');

const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '接口名称', dataIndex: 'apiName', align: 'left', ellipsis: true },
  { title: '接口编码', dataIndex: 'apiCode', align: 'left', width: 180, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', align: 'center', width: 90 },
  { title: '更新人', dataIndex: 'updateBy', align: 'center', width: 120 },
  { title: '更新时间', dataIndex: 'updateTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 250, fixed: 'right' as const },
];

// ---------- 新建 ----------
const createOpen = ref(false);
const newCode = ref('');
const newName = ref('');
const creating = ref(false);

const openCreateModal = () => {
  newCode.value = '';
  newName.value = '';
  createOpen.value = true;
};

const handleCreate = async () => {
  if (!newCode.value.trim() || !newName.value.trim()) {
    message.warning('请填写接口编码与名称');
    return;
  }
  creating.value = true;
  try {
    //新建默认给一个 SQL 查询步骤占位（后端要求至少一个步骤）
    const steps = JSON.stringify([{ type: 'sql', name: 'query', config: { sql: 'SELECT 1', params: {} } }]);
    const res = await saveApiApi(undefined, newCode.value.trim(), newName.value.trim(), '', steps, '');
    if (res.code === 200 && res.data) {
      createOpen.value = false;
      message.success('创建成功！');
      router.push(`/openapi/design/${res.data.id}`);
    } else {
      message.error(res.msg || '创建失败');
    }
  } finally {
    creating.value = false;
  }
};

// ---------- 编辑 / 发布 / 停用 / 删除 ----------
const handleEdit = (record: ApiRecord) => {
  router.push(`/openapi/design/${record.id}`);
};

const handlePublish = async (record: ApiRecord) => {
  const res = await publishApiApi(record.id);
  if (res.code === 200) {
    message.success('发布成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '发布失败');
  }
};

const handleStop = async (record: ApiRecord) => {
  const res = await stopApiApi(record.id);
  if (res.code === 200) {
    message.success('停用成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '停用失败');
  }
};

const handleDelete = async (id: string) => {
  const res = await deleteApiApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

// ---------- 定时采集 ----------
const scheduleOpen = ref(false);
const scheduleApi = ref<ApiRecord | null>(null);
const scheduleList = ref<ApiScheduleRecord[]>([]);
const schedCron = ref('');
const schedEnabled = ref(true);
const schedSaving = ref(false);

const scheduleColumns = [
  { title: 'cron', dataIndex: 'cron', width: 160 },
  { title: '状态', key: 'enabled', width: 80 },
  { title: '上次执行', dataIndex: 'lastRunTime', width: 170 },
  { title: '下次执行', dataIndex: 'nextRunTime', width: 170 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'operation', width: 80 },
];

const openScheduleModal = async (record: ApiRecord) => {
  scheduleApi.value = record;
  schedCron.value = '';
  schedEnabled.value = true;
  scheduleOpen.value = true;
  await loadSchedules(record.apiCode);
};

const loadSchedules = async (apiCode: string) => {
  const res = await getScheduleListApi(apiCode);
  if (res.code === 200 && res.data) {
    scheduleList.value = res.data.records ?? [];
  } else {
    scheduleList.value = [];
  }
};

const handleScheduleSave = async () => {
  if (!scheduleApi.value) return;
  if (!schedCron.value.trim()) {
    message.warning('请填写 cron 表达式');
    return;
  }
  schedSaving.value = true;
  try {
    const res = await saveScheduleApi(scheduleApi.value.apiCode, schedCron.value.trim(), schedEnabled.value, '');
    if (res.code === 200) {
      message.success('保存成功！');
      schedCron.value = '';
      await loadSchedules(scheduleApi.value.apiCode);
    } else {
      message.error(res.msg || '保存失败');
    }
  } finally {
    schedSaving.value = false;
  }
};

const handleScheduleDelete = async (id: string) => {
  const res = await deleteScheduleApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    if (scheduleApi.value) await loadSchedules(scheduleApi.value.apiCode);
  } else {
    message.error(res.msg || '删除失败');
  }
};
</script>

<style scoped>
.schedule-tip {
  margin-bottom: 12px;
}

.schedule-form {
  margin-bottom: 12px;
}
</style>
