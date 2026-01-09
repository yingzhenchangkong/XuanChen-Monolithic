<template>
  <div class="query-style btn-style">
    <a-form layout="inline" :model="model">
      <a-form-item name="listUser" label="员工">
        <a-select v-model:value="model.listUser" style="width: 200px" :options="optionsUser" mode="multiple"
          :fieldNames="{ label: 'nickName', value: 'userName' }" placeholder="请选择员工" allowClear>
        </a-select>
      </a-form-item>
      <a-button type="primary" @click="handleLink">
        <template #icon>
          <PlusOutlined />
        </template>关联用户
      </a-button>
    </a-form>
  </div>
  <a-table :dataSource="dataSource" :columns="columnsDeptUser" :pagination="ipagination" :loading="loading"
    :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange, type: 'radio' }" bordered
    rowKey="id" size="small" @change="handleTableChange">
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'operation'">
        <a @click="handleUnlink(record)">
          <UserAddOutlined />取消关联
        </a>
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue';
import { columnsDeptUser } from '../dept.data';
import { useList } from '@/hooks/useList';
import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';

const props = defineProps({
  deptCode: { type: String, default: '', required: true }
})

const url = {
  list: '/system/userdept/listDeptUser',
  link: '/system/userdept/link',
  unlink: '/system/userdept/unlink',
  selectUser: '/system/user/select',
}

const queryParams = reactive({ deptCode: props.deptCode });
const model = reactive({
  listUser: [],
});

const optionsUser = ref([]);
const getSelectUser = async () => {
  const res = await getAction(url.selectUser, {});
  optionsUser.value = res.data;
};
getSelectUser();

const handleLink = async () => {
  if (props.deptCode === '') {
    message.error('请选择部门');
    return;
  }
  if (model.listUser.length === 0) {
    message.error('请选择员工');
    return;
  }
  const res: any = await postAction(url.link, { listUser: model.listUser, deptCode: props.deptCode });
  if (res.code === 200) {
    message.success(res.msg);
    model.listUser = [];
    queryParams.deptCode = props.deptCode;
    loadData();
  } else {
    message.error(res.msg);
  }
}

async function handleUnlink(record: any) {
  const res: any = await deleteAction(url.unlink, { id: record.id })
  if (res.code === 200) {
    message.success(res.msg);
    queryParams.deptCode = props.deptCode;
    loadData();
  } else {
    message.warning("取消关联失败!");
  }
}

onMounted(() => {
  loading.value = false;
})

watch(() => props.deptCode, (val: string) => {
  if (val) {
    queryParams.deptCode = val;
    loadData();
  }
})

const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
</script>