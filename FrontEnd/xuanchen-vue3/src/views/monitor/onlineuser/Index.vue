<template>
  <a-card>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading" bordered
      rowKey="id" size="small">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'operation'">
          <a-popconfirm title="确定强制退出吗？" @confirm="() => handleForceLogout(record.token)" placement="left">
            <a>
              <LogoutOutlined />强制退出
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { message } from 'ant-design-vue';
import { useList } from '@/hooks/useList';

import { columns } from './onlineuser.data';
import { OnlineUserApiUrl, forceLogout } from './onlineuser.api';

/** url */
const url = reactive({
  list: OnlineUserApiUrl.LIST,
});

const handleForceLogout = async (token: string) => {
  const res: any = await forceLogout(token);
  if (res.code === 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.error(res.msg);
  }
};

const {
  loadData,
  dataSource, loading, ipagination
} = useList({ url })
loadData();
</script>