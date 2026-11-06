<template>
  <a-drawer v-model:open="open" title="角色用户配置" placement="right" :width="600">
    <div class="query-style btn-style">
      <a-form layout="inline" :model="modelUnAuthUser">
        <a-form-item name="listUnAuthUser" label="用户">
          <XCSelect v-model="modelUnAuthUser.listUnAuthUser" style="width: 400px"
            apiUrl="/system/accountrole/listUnAuthUser" :watchValue="queryParams.roleId" watchParamName="roleId"
            dataPath="data.records" mode="multiple" :fieldNames="{ label: 'nickName', value: 'accountId' }"
            placeholder="请选择用户" showSearch :filter-option="filterOption" />
        </a-form-item>
        <a-button type="primary" @click="handleAuth">
          <template #icon>
            <UsergroupAddOutlined />
          </template>授权
        </a-button>
      </a-form>
    </div>
    <a-table :dataSource="dataSource" :columns="columnsAssignUser" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #title>
        <span class="btn-style">
          <a-button type="primary" size="small" @click="handleCancleAuthBatch">
            <template #icon>
              <UsergroupDeleteOutlined />
            </template>
            批量取消授权
          </a-button>
        </span>
        <a-tag color="processing">
          {{ `已选中 ${state.selectedRowKeys.length} 条记录` }}
          <a @click="handleCancelSelect">清空</a>
        </a-tag>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleCancleAuth(record)">
            <UserDeleteOutlined />取消授权
          </a>
        </template>
      </template>
    </a-table>
    <template #footer>
      <a-button @click="handleCancel" style="float: right;">关闭</a-button>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { useList } from '@/hooks/useList'
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { columnsAssignUser } from '../role.data';
import { RoleApiUrl, cancelAuthUser, cancleAuthUserBatch, authUser, authUserBatch } from '../role.api';
import XCSelect from '@/components/xuanchen/XCSelect.vue';

const url = {
  list: RoleApiUrl.ASSIGN_USER_LIST_AUTH_USER,
}
/** 查询参数 */
const queryParams = reactive({
  roleId: '',
})
const open = ref(false);
const show = (roleId: string) => {
  open.value = true;
  handleCancelSelect();
  queryParams.roleId = roleId;
  modelUnAuthUser.listUnAuthUser = [];
  refresh();
}

const refresh = () => {
  loadData();
}

const handleCancel = () => {
  open.value = false;
}
/** 取消授权 */
const handleCancleAuth = async (record: any) => {
  try {
    const res = await cancelAuthUser(record.accountId, queryParams.roleId);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    refresh();
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示
  }
}
/** 批量取消授权 */
const handleCancleAuthBatch = async () => {
  const accountIds = state.selectedRowKeys.map(key => String(key));
  try {
    const res = await cancleAuthUserBatch(accountIds, queryParams.roleId);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    refresh();
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示
  }
}
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })

const modelUnAuthUser = reactive({
  listUnAuthUser: [] as string[],
});
const filterOption = (input: string, option: any) => {
  if (!option) return false;
  const label = option.nickName || '';
  return label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}
/** 授权 */
const handleAuth = async () => {
  const selectedUserIds = modelUnAuthUser.listUnAuthUser;
  if (selectedUserIds.length === 0) {
    message.warning('请选择用户');
    return;
  }
  const roleId = queryParams.roleId;
  try {
    const res = selectedUserIds.length === 1
      ? await authUser(selectedUserIds[0], roleId)
      : await authUserBatch(selectedUserIds.map(key => String(key)), roleId);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg || '操作成功');
    modelUnAuthUser.listUnAuthUser = [];
    refresh();
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示
  }
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>