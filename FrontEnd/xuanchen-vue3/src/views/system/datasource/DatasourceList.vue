<template>
  <div class="page-container">
    <a-card :bordered="false">
      <a-row :gutter="16" :justify="`space-between`" align="middle" class="mb-sm">
        <a-col>
          <h3 class="mb-0">数据源配置</h3>
          <span class="text-secondary text-xs">统一数据源（sys_datasource），报表/API/大屏/代码生成共用</span>
        </a-col>
        <a-col>
          <a-space>
            <a-input-search v-model:value="keyword" placeholder="搜索名称 / 连接串" allowClear
              style="width: 240px" @search="loadData" @press-enter="loadData" />
            <a-button type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新建数据源
            </a-button>
          </a-space>
        </a-col>
      </a-row>

      <a-table :columns="columns" :data-source="filtered" :pagination="pagination" row-key="id" size="middle"
        :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'volcano'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'password'">
            <a-tag :color="record.hasPassword ? 'blue' : 'default'">
              {{ record.hasPassword ? '已配置' : '未配置' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleTest(record)">测试连接</a>
              <a @click="handleEdit(record)">编辑</a>
              <a-popconfirm :title="`确认删除「${record.dsName}」？删除后其他模块将无法引用此数据源。`"
                @confirm="handleDelete(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <Operation ref="refOperation" @childOK="loadData" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import Operation from './modal/Operation.vue';
import { deleteSysDatasourceApi, getSysDatasourceListApi, testSysDatasourceConnectionApi, type SysDatasourceModel } from './datasource.api';

const refOperation = ref();
const loading = ref(false);
const keyword = ref('');
const list = ref<SysDatasourceModel[]>([]);

const pagination = { pageSize: 10, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` };

const filtered = computed(() => {
  if (!keyword.value) return list.value;
  const kw = keyword.value.toLowerCase();
  return list.value.filter((d) =>
    (d.dsName ?? '').toLowerCase().includes(kw) || (d.jdbcUrl ?? '').toLowerCase().includes(kw),
  );
});

const columns = [
  { title: '数据源名称', dataIndex: 'dsName', width: 180 },
  { title: '类型', dataIndex: 'dsType', width: 100 },
  { title: 'JDBC 连接串', dataIndex: 'jdbcUrl', ellipsis: true, tooltip: true },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '密码', key: 'password', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getSysDatasourceListApi();
    if (res.code === 200) list.value = res.data ?? [];
  } catch { /* 错误统一处理 */ }
  finally { loading.value = false; }
};

const handleAdd = () => refOperation.value?.add();
const handleEdit = (record: SysDatasourceModel) => refOperation.value?.edit(record);
const handleDelete = async (record: SysDatasourceModel) => {
  const res = await deleteSysDatasourceApi(record.id!);
  if (res.code === 200) {
    message.success('删除成功');
    loadData();
  } else {
    message.error(res.msg ?? '删除失败');
  }
};

const handleTest = async (record: SysDatasourceModel) => {
  const res = await testSysDatasourceConnectionApi({ id: record.id, jdbcUrl: record.jdbcUrl, username: record.username, password: '' });
  if (res.code === 200) {
    message.success(res.data ?? '连接成功');
  } else {
    message.error(res.msg ?? '连接失败');
  }
};

onMounted(loadData);
</script>
