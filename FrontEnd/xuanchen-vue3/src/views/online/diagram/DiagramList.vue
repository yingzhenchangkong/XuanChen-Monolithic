<template>
  <XCListPage ref="listRef" :url="url" :columns="columnsIndex" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="流程图">
    <template #toolbar-prepend>
      <a-button type="primary" @click="openCreateModal">
        <template #icon>
          <PlusOutlined />
        </template>新建流程图
      </a-button>
    </template>

    <template #operation="{ record }">
      <a @click="handleEdit(record as OnlineDocRecord)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除后不可恢复，确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>
  </XCListPage>

  <!-- 新建弹窗：命名后先落库再进编辑器（a-modal 自带 footer，符合组件约定） -->
  <a-modal v-model:open="createOpen" title="新建流程图" :confirm-loading="creating" @ok="handleCreate">
    <a-input v-model:value="newName" placeholder="请输入文档名称" :maxlength="100" @press-enter="handleCreate" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import { DiagramApiUrl, deleteDiagramApi, saveDiagramApi } from './diagram.api';
import type { OnlineDocRecord } from './diagram.types';
import { queryParams, queryFormItems, columnsIndex } from './diagram.data';

const router = useRouter();
const listRef = ref();

/** 列表接口 */
const url = {
  list: DiagramApiUrl.INDEX_LIST,
  delete: DiagramApiUrl.INDEX_DELETE,
};

// ---------- 新建 ----------
const createOpen = ref(false);
const newName = ref('');
const creating = ref(false);

const openCreateModal = () => {
  newName.value = '';
  createOpen.value = true;
};

const handleCreate = async () => {
  if (!newName.value.trim()) {
    message.warning('请输入文档名称');
    return;
  }
  creating.value = true;
  try {
    const res = await saveDiagramApi(undefined, newName.value.trim(), '');
    if (res.code === 200 && res.data) {
      createOpen.value = false;
      message.success('创建成功！');
      router.push(`/onlinedoc/diagram/edit/${res.data.id}`);
    } else {
      message.error(res.msg || '创建失败');
    }
  } finally {
    creating.value = false;
  }
};

// ---------- 编辑 / 删除 ----------
const handleEdit = (record: OnlineDocRecord) => {
  router.push(`/onlinedoc/diagram/edit/${record.id}`);
};

const handleDelete = async (id: string) => {
  const res = await deleteDiagramApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};
</script>
