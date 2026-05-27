<template>
  <a-drawer v-model:open="open" title="角色用户配置" placement="right" :width="600">
    <div class="query-style btn-style">
      <a-form layout="inline" :model="modelUnAuthUser">
        <a-form-item name="listUnAuthUser" label="用户">
          <a-select v-model:value="modelUnAuthUser.listUnAuthUser" style="width: 400px" :options="optionsUnAuthUser"
            mode="multiple" :fieldNames="{ label: 'nickName', value: 'userId' }" placeholder="请选择用户" allowClear
            show-search :filter-option="filterOption">
          </a-select>
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
import { RoleApiUrl, cancelAuthUser, cancleAuthUserBatch, authUser, authUserBatch, getListUnAuthUser } from '../role.api';

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
  loadDataUnAuthUser();
}

const handleCancel = () => {
  open.value = false;
}
/** 取消授权 */
const handleCancleAuth = async (record: any) => {
  const res: any = await cancelAuthUser(record.userId, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
/** 批量取消授权 */
const handleCancleAuthBatch = async () => {
  const userIds = state.selectedRowKeys.map(key => String(key));
  const res: any = await cancleAuthUserBatch(userIds, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })

const modelUnAuthUser = reactive({
  listUnAuthUser: [],
});
const optionsUnAuthUser = ref([]);
const loadDataUnAuthUser = async () => {
  const res: any = await getListUnAuthUser(queryParams.roleId);
  optionsUnAuthUser.value = res.data.records;
}
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
  let res: any;
  const roleId = queryParams.roleId;
  if (selectedUserIds.length === 1) {
    res = await authUser(selectedUserIds[0]!, roleId);
  } else if (selectedUserIds.length > 1) {
    const userIds = selectedUserIds.map(key => String(key));
    res = await authUserBatch(userIds, roleId);
  }
  message.success(res.msg || '操作成功');
  modelUnAuthUser.listUnAuthUser = [];
  refresh();
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>