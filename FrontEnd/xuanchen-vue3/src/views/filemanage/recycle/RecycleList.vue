<template>
  <XCListPage ref="listRef" :url="url" :columns="recycleColumns" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="回收站">
    <!-- 类型/大小列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'ext'">
        <a-tag color="blue">{{ String(record.ext).toUpperCase() }}</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'fileSize'">
        {{ formatFileSize(record.fileSize as number) }}
      </template>
    </template>

    <!-- 操作列：恢复 / 彻底删除 -->
    <template #operation="{ record }">
      <a @click="handleRestore(record.id)">
        <UndoOutlined /> 恢复
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="彻底删除后不可恢复，存储空间立即释放，确定吗？" placement="left"
        @confirm="() => handlePurge(record.id)">
        <a class="danger-link">
          <DeleteOutlined /> 彻底删除
        </a>
      </a-popconfirm>
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { DeleteOutlined, UndoOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import { getRecyclePageApi, purgeFileApi, restoreFileApi } from '../filemanage.api';
import { formatFileSize, recycleColumns } from '../filemanage.data';

const listRef = ref();

const url = {
  list: '/fileinfo/recycle',
};

const queryParams = reactive<Record<string, unknown>>({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const queryFormItems = reactive([
  { name: 'keyword', label: '文件名', type: 'input' as const, placeholder: '请输入文件名关键字' },
]);

const handleRestore = async (id: string) => {
  const res = await restoreFileApi(id);
  if (res.code === 200) {
    message.success('恢复成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '恢复失败');
  }
};

const handlePurge = async (id: string) => {
  const res = await purgeFileApi(id);
  if (res.code === 200) {
    message.success('已彻底删除！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '彻底删除失败');
  }
};
</script>

<style scoped>
.danger-link {
  color: #ff4d4f;
}
</style>
