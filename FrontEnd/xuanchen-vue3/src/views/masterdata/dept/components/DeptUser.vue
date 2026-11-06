<template>
  <div class="query-style btn-style">
    <a-form layout="inline" :model="model">
      <a-form-item name="listPerson" label="人员">
        <a-select v-model:value="model.listPerson" style="width: 200px" :options="optionsUser" mode="multiple"
          :fieldNames="{ label: 'realName', value: 'id' }" placeholder="请选择人员" allowClear show-search
          :filter-option="filterOption">
        </a-select>
      </a-form-item>
      <a-button type="primary" @click="handleLink">
        <template #icon>
          <PlusOutlined />
        </template>关联人员
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
import { useList } from '@/hooks/useList';
import { message } from 'ant-design-vue';

import { DeptApiUrl, link, unlink } from '../dept.api';
import { getPersonSelect } from '../../person/person.api';
import { columnsDeptUser } from '../dept.data';
import type { SelectOption } from '@/types/api';

const props = defineProps({
  deptCode: { type: String, default: '', required: true }
})

const url = {
  list: DeptApiUrl.USER_LIST,
}

const queryParams = reactive({ deptCode: props.deptCode });
const model = reactive({
  listPerson: [],
});

const optionsUser = ref<SelectOption[]>([]);
const getSelectUser = async () => {
  //数据来源：人员表（id + real_name）；展示"姓名（人员编码）"区分同名人员，value 仍为人员ID
  optionsUser.value = (await getPersonSelect()).map((item: any) => ({
    ...item,
    realName: item.personCode ? `${item.realName}（${item.personCode}）` : (item.realName || ''),
  }));
};
getSelectUser();

const filterOption = (input: string, option: any) => {
  if (!option) return false;
  const label = option.realName || '';
  return label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

const handleLink = async () => {
  if (props.deptCode === '') {
    message.error('请选择部门');
    return;
  }
  if (model.listPerson.length === 0) {
    message.error('请选择人员');
    return;
  }
  const res = await link(model.listPerson, props.deptCode);
  if (res.code === 200) {
    message.success(res.msg);
    model.listPerson = [];
    queryParams.deptCode = props.deptCode;
    loadData();
  } else {
    message.error(res.msg);
  }
}

async function handleUnlink(record: any) {
  const res = await unlink(record.id);
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