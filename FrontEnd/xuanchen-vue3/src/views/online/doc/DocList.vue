<template>
  <XCListPage ref="listRef" :url="url" :columns="columnsIndex" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="协同文档">
    <template #toolbar-prepend>
      <a-button type="primary" @click="openCreateModal('word')">
        <template #icon><PlusOutlined /></template>新增
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'ext'">
        <a-tag :color="extColor((record as OnlineFileInfo).ext)">{{ (record as OnlineFileInfo).ext.toUpperCase() }}</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'role'">
        <a-tag v-if="(record as OnlineFileInfo).role === 'owner'" color="blue">属主</a-tag>
        <a-tag v-else color="green">协作者</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'permission'">
        <span v-if="(record as OnlineFileInfo).role === 'owner'">-</span>
        <span v-else>{{ (record as OnlineFileInfo).permission === 1 ? '可编辑' : '只读' }}</span>
      </template>
    </template>

    <template #operation="{ record }">
      <a @click="handleOpen(record as OnlineFileInfo)">
        <EditOutlined /> 编辑
      </a>
      <template v-if="record.role === 'owner'">
        <a-divider type="vertical"></a-divider>
        <a @click="openRenameModal(record as OnlineFileInfo)">
          <FormOutlined /> 重命名
        </a>
        <a-divider type="vertical"></a-divider>
        <a @click="openCollabModal(record as OnlineFileInfo)">
          <TeamOutlined /> 协作者
        </a>
        <a-divider type="vertical"></a-divider>
        <a-popconfirm title="删除后进入文件中心回收站，确定删除吗？" placement="left"
          @confirm="() => handleDelete(record.id)">
          <a>
            <DeleteOutlined /> 删除
          </a>
        </a-popconfirm>
      </template>
    </template>
  </XCListPage>

  <!-- 新建弹窗：命名后落库文件中心，再进 ONLYOFFICE 协同编辑器 -->
  <a-modal v-model:open="createOpen" title="新建协同文档" :confirm-loading="creating" @ok="handleCreate">
    <a-form layout="vertical">
      <a-form-item label="文档类型">
        <a-radio-group v-model:value="newType">
          <a-radio-button value="word"><FileWordOutlined /> Word</a-radio-button>
          <a-radio-button value="cell"><FileExcelOutlined /> Excel</a-radio-button>
          <a-radio-button value="slide"><FilePptOutlined /> PPT</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="文档名称">
        <a-input v-model:value="newName" placeholder="请输入文档名称" :maxlength="100" @press-enter="handleCreate" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 重命名弹窗 -->
  <a-modal v-model:open="renameOpen" title="重命名" :confirm-loading="renaming" @ok="handleRename">
    <a-input v-model:value="renameName" placeholder="请输入文档名称" :maxlength="100" @press-enter="handleRename" />
  </a-modal>

  <!-- 协作者管理弹窗 -->
  <a-modal v-model:open="collabOpen" title="协作者管理" :footer="null" :width="520">
    <div class="collab-add">
      <a-input v-model:value="collabUser" placeholder="协作者登录名" style="width: 200px" />
      <a-select v-model:value="collabPerm" style="width: 110px">
        <a-select-option :value="1">可编辑</a-select-option>
        <a-select-option :value="2">只读</a-select-option>
      </a-select>
      <a-button type="primary" :loading="adding" @click="handleAddCollab">添加</a-button>
    </div>
    <a-empty v-if="collaborators.length === 0" description="暂无协作者，添加后对方可在「在线文档-协同文档」中共同编辑" />
    <a-list v-else :data-source="collaborators" size="small">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>{{ item.username }}</template>
            <template #description>{{ item.permission === 1 ? '可编辑' : '只读' }}</template>
          </a-list-item-meta>
          <template #actions>
            <a-popconfirm title="移除后对方将无法再编辑，确定移除吗？" @confirm="() => handleRemoveCollab(item)">
              <a style="color: #ff4d4f"><DeleteOutlined /> 移除</a>
            </a-popconfirm>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  DeleteOutlined,
  EditOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FileWordOutlined,
  FormOutlined,
  PlusOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import {
  DocApiUrl,
  addCollaboratorApi,
  createDocApi,
  listCollaboratorsApi,
  removeCollaboratorApi,
  removeDocApi,
  renameDocApi,
} from './doc.api';
import type { CollaboratorRecord, OnlineFileInfo } from './doc.types';
import { queryParams, queryFormItems, columnsIndex, extColor } from './doc.data';

const router = useRouter();
const listRef = ref();

/** 列表接口 */
const url = {
  list: DocApiUrl.INDEX_LIST,
};

// ---------- 新建 ----------
const createOpen = ref(false);
const newType = ref('word');
const newName = ref('');
const creating = ref(false);

const openCreateModal = (type: string) => {
  newType.value = type;
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
    const res = await createDocApi(newType.value, newName.value.trim());
    if (res.code === 200 && res.data) {
      createOpen.value = false;
      message.success('创建成功！');
      //复用文件中心的 ONLYOFFICE 协同编辑全屏路由
      router.push(`/fileedit/${res.data.id}`);
    } else {
      message.error(res.msg || '创建失败');
    }
  } finally {
    creating.value = false;
  }
};

// ---------- 编辑 / 删除 ----------
const handleOpen = (record: OnlineFileInfo) => {
  router.push(`/fileedit/${record.id}`);
};

const handleDelete = async (id: string) => {
  const res = await removeDocApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

// ---------- 重命名 ----------
const renameOpen = ref(false);
const renameId = ref('');
const renameName = ref('');
const renaming = ref(false);

const openRenameModal = (record: OnlineFileInfo) => {
  renameId.value = record.id;
  //去掉扩展名后编辑（后端重命名时会自动补回扩展名）
  const dot = record.originalName.lastIndexOf('.');
  renameName.value = dot > 0 ? record.originalName.slice(0, dot) : record.originalName;
  renameOpen.value = true;
};

const handleRename = async () => {
  if (!renameName.value.trim()) {
    message.warning('请输入文档名称');
    return;
  }
  renaming.value = true;
  try {
    const res = await renameDocApi(renameId.value, renameName.value.trim());
    if (res.code === 200) {
      renameOpen.value = false;
      message.success('重命名成功！');
      listRef.value?.loadData();
    } else {
      message.error(res.msg || '重命名失败');
    }
  } finally {
    renaming.value = false;
  }
};

// ---------- 协作者管理 ----------
const collabOpen = ref(false);
const collabFileId = ref('');
const collabUser = ref('');
const collabPerm = ref(1);
const adding = ref(false);
const collaborators = ref<CollaboratorRecord[]>([]);

const openCollabModal = async (record: OnlineFileInfo) => {
  collabFileId.value = record.id;
  collabUser.value = '';
  collabPerm.value = 1;
  collaborators.value = [];
  collabOpen.value = true;
  const res = await listCollaboratorsApi(record.id);
  if (res.code === 200 && res.data) {
    collaborators.value = res.data;
  } else {
    message.error(res.msg || '获取协作者失败');
  }
};

const handleAddCollab = async () => {
  if (!collabUser.value.trim()) {
    message.warning('请输入协作者登录名');
    return;
  }
  adding.value = true;
  try {
    const res = await addCollaboratorApi(collabFileId.value, collabUser.value.trim(), collabPerm.value);
    if (res.code === 200) {
      message.success('添加成功！');
      collabUser.value = '';
      const listRes = await listCollaboratorsApi(collabFileId.value);
      if (listRes.code === 200 && listRes.data) {
        collaborators.value = listRes.data;
      }
    } else {
      message.error(res.msg || '添加失败');
    }
  } finally {
    adding.value = false;
  }
};

const handleRemoveCollab = async (item: CollaboratorRecord) => {
  const res = await removeCollaboratorApi(item.fileId, item.username);
  if (res.code === 200) {
    message.success('移除成功！');
    collaborators.value = collaborators.value.filter((c) => c.id !== item.id);
  } else {
    message.error(res.msg || '移除失败');
  }
};
</script>

<style scoped>
.collab-add {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
