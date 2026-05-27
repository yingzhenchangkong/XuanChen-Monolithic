<template>
  <a-drawer v-model:open="open" title="岗位员工分配" placement="right" :width="600">
    <div class="query-style btn-style">
      <a-form layout="inline" :model="modelUnAssignUser">
        <a-form-item name="listUnAssignUser" label="用户">
          <a-select v-model:value="modelUnAssignUser.listUnAssignUser" style="width: 400px"
            :options="optionsUnAssignUser" mode="multiple" :fieldNames="{ label: 'nickName', value: 'userId' }"
            placeholder="请选择用户" allowClear show-search :filter-option="filterOption">
          </a-select>
        </a-form-item>
        <a-button type="primary" @click="handleAssign">
          <template #icon>
            <UsergroupAddOutlined />
          </template>分配
        </a-button>
      </a-form>
    </div>
    <a-table :dataSource="dataSource" :columns="columnsAssignUser" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #title>
        <span class="btn-style">
          <a-button type="primary" size="small" @click="handleCancleAssignBatch">
            <template #icon>
              <UsergroupDeleteOutlined />
            </template>
            批量取消分配
          </a-button>
        </span>
        <a-tag color="processing">
          {{ `已选中 ${state.selectedRowKeys.length} 条记录` }}
          <a @click="handleCancelSelect">清空</a>
        </a-tag>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleCancleAssign(record)">
            <UserDeleteOutlined />取消分配
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
import { columnsAssignUser } from '../post.data';
import { PostApiUrl, cancelAssignUser, cancleAssignUserBatch, assignUser, assignUserBatch, getListUnAssignUser } from '../post.api';

const url = {
  list: PostApiUrl.ASSIGN_USER_LIST_ASSIGN_USER,
}
/** 查询参数 */
const queryParams = reactive({
  postId: '',
})
const open = ref(false);
const show = (postId: string) => {
  open.value = true;
  handleCancelSelect();
  queryParams.postId = postId;
  modelUnAssignUser.listUnAssignUser = [];
  refresh();
}

const refresh = () => {
  loadData();
  loadDataUnAssignUser();
}

const handleCancel = () => {
  open.value = false;
}
/** 取消分配 */
const handleCancleAssign = async (record: any) => {
  const res: any = await cancelAssignUser(record.userId, queryParams.postId);
  message.success(res.msg);
  refresh();
}
/** 批量取消分配 */
const handleCancleAssignBatch = async () => {
  const userIds = state.selectedRowKeys.map(key => String(key));
  const res: any = await cancleAssignUserBatch(userIds, queryParams.postId);
  message.success(res.msg);
  refresh();
}
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })

const modelUnAssignUser = reactive({
  listUnAssignUser: [],
});

const optionsUnAssignUser = ref([]);

const loadDataUnAssignUser = async () => {
  const res: any = await getListUnAssignUser(queryParams.postId);
  optionsUnAssignUser.value = res.data.records;
}

const filterOption = (input: string, option: any) => {
  if (!option) return false;
  const label = option.nickName || '';
  return label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

/** 分配 */
const handleAssign = async () => {
  const selectedUserIds = modelUnAssignUser.listUnAssignUser;
  if (selectedUserIds.length === 0) {
    message.error('请选择用户');
    return;
  }
  let res: any;
  const postId = queryParams.postId;
  if (selectedUserIds.length === 1) {
    res = await assignUser(selectedUserIds[0]!, postId);
  } else if (selectedUserIds.length > 1) {
    const userIds = selectedUserIds.map(key => String(key));
    res = await assignUserBatch(userIds, postId);
  }
  message.success(res.msg || '操作成功');
  refresh();
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>