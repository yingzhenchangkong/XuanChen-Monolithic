<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="在线表单">
    <template #toolbar-prepend>
      <a-button type="primary" @click="openCreateModal">
        <template #icon>
          <PlusOutlined />
        </template>新建表单
      </a-button>
    </template>

    <template #operation="{ record }">
      <a @click="handleDesign(record as OnlineFormRecord)">
        <EditOutlined /> 设计
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除后流程绑定将失效，确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>
  </XCListPage>

  <!-- 新建弹窗：命名后先落库再进设计器 -->
  <a-modal v-model:open="createOpen" title="新建在线表单" :confirm-loading="creating" @ok="handleCreate">
    <a-form layout="vertical">
      <a-form-item label="表单名称" required>
        <a-input v-model:value="newName" placeholder="请输入表单名称" :maxlength="100" @press-enter="handleCreate" />
      </a-form-item>
      <a-form-item label="备注">
        <a-input v-model:value="newRemark" placeholder="选填" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import { deleteFormApi, saveFormApi } from './form.api';
import type { OnlineFormRecord } from './form.types';

const router = useRouter();
const listRef = ref();

const url = {
  list: '/online/form/list',
  delete: '/online/form/delete',
};

const queryParams = reactive<Record<string, unknown>>({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const queryFormItems = reactive([
  {
    name: 'keyword',
    label: '名称',
    type: 'input' as const,
    placeholder: '按表单名称搜索',
  },
]);

const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '表单名称', dataIndex: 'formName', align: 'left', ellipsis: true },
  { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
  { title: '创建人', dataIndex: 'createBy', align: 'center', width: 140 },
  { title: '更新时间', dataIndex: 'updateTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 140, fixed: 'right' as const },
];

// ---------- 新建 ----------
const createOpen = ref(false);
const newName = ref('');
const newRemark = ref('');
const creating = ref(false);

const openCreateModal = () => {
  newName.value = '';
  newRemark.value = '';
  createOpen.value = true;
};

const handleCreate = async () => {
  if (!newName.value.trim()) {
    message.warning('请输入表单名称');
    return;
  }
  creating.value = true;
  try {
    const res = await saveFormApi(undefined, newName.value.trim(), newRemark.value.trim(), '');
    if (res.code === 200 && res.data) {
      createOpen.value = false;
      message.success('创建成功！');
      router.push(`/businessprocess/form/design/${res.data.id}`);
    } else {
      message.error(res.msg || '创建失败');
    }
  } finally {
    creating.value = false;
  }
};

// ---------- 设计 / 删除 ----------
const handleDesign = (record: OnlineFormRecord) => {
  router.push(`/businessprocess/form/design/${record.id}`);
};

const handleDelete = async (id: string) => {
  const res = await deleteFormApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};
</script>
