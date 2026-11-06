<template>
  <a-card :bordered="false" title="我的待办">
    <template #extra>
      <a-space>
        <a-button type="link" size="small" @click="emit('refresh')">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button type="primary" size="small" @click="openAdd">
          <PlusOutlined />
          新增
        </a-button>
      </a-space>
    </template>
    <a-empty v-if="!todos?.length" description="暂无待办，点右上角新增" :image-style="{ height: '48px' }" />
    <div v-else class="todo-list">
      <div v-for="todo in todos" :key="todo.id" class="todo-item">
        <a-tag :color="priorityColor(todo.priority)" class="priority-tag">{{ priorityText(todo.priority) }}</a-tag>
        <div class="todo-main">
          <div class="todo-title" :title="todo.title">{{ todo.title }}</div>
          <div class="todo-meta">
            <span v-if="todo.dueDate">截止：{{ todo.dueDate }}</span>
            <span v-else>创建：{{ todo.createTime }}</span>
          </div>
        </div>
        <a-space :size="0">
          <a-tooltip title="完成">
            <a-button type="text" size="small" style="color: #52c41a;" @click="handleFinish(todo)">
              <CheckCircleOutlined />
            </a-button>
          </a-tooltip>
          <a-tooltip title="编辑">
            <a-button type="text" size="small" @click="openEdit(todo)">
              <EditOutlined />
            </a-button>
          </a-tooltip>
          <a-popconfirm title="确定删除该待办？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(todo)">
            <a-button type="text" size="small" danger>
              <DeleteOutlined />
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>

    <XCModal v-model:open="modalOpen" :title="editingId ? '编辑待办' : '新增待办'" :width="480" ok-text="保存" cancel-text="取消" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="标题" name="title">
          <a-input v-model:value="form.title" placeholder="请输入待办标题" :maxlength="200" allow-clear />
        </a-form-item>
        <a-form-item label="内容" name="content">
          <a-textarea v-model:value="form.content" placeholder="请输入待办内容（选填）" :maxlength="1000" :rows="3" />
        </a-form-item>
        <a-form-item label="优先级" name="priority">
          <a-radio-group v-model:value="form.priority">
            <a-radio v-for="opt in TODO_PRIORITY_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="截止时间" name="dueDate">
          <a-date-picker
            v-model:value="form.dueDate"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择截止时间（选填）"
            style="width: 100%;"
          />
        </a-form-item>
      </a-form>
    </XCModal>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance, Rule } from 'ant-design-vue/es/form';
import XCModal from '@/components/xuanchen/XCModal.vue';
import { addTodoApi, deleteTodoApi, editTodoApi, finishTodoApi } from '../index.api';
import { priorityColor, priorityText, TODO_PRIORITY_OPTIONS } from '../index.data';
import type { DashboardTodo, TodoSaveParams } from '../index.types';

defineProps<{
  todos?: DashboardTodo[];
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const modalOpen = ref(false);
const submitting = ref(false);
const editingId = ref<string | null>(null);
const formRef = ref<FormInstance | null>(null);

const defaultForm = () => ({
  title: '',
  content: '',
  priority: 2,
  dueDate: '',
});
const form = reactive(defaultForm());

const rules: Record<string, Rule[]> = {
  title: [{ required: true, whitespace: true, message: '请输入待办标题', trigger: 'blur' }],
};

const openAdd = () => {
  editingId.value = null;
  Object.assign(form, defaultForm());
  formRef.value?.resetFields();
  modalOpen.value = true;
};

const openEdit = (todo: DashboardTodo) => {
  editingId.value = todo.id;
  form.title = todo.title;
  form.content = '';
  form.priority = todo.priority ?? 2;
  form.dueDate = todo.dueDate ?? '';
  formRef.value?.resetFields();
  modalOpen.value = true;
};

const handleSave = async () => {
  if (submitting.value) return;
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const params: TodoSaveParams = {
      title: form.title.trim(),
      content: form.content?.trim() || null,
      priority: form.priority,
      dueDate: form.dueDate || null,
    };
    const ok = editingId.value
      ? await editTodoApi({ ...params, id: editingId.value })
      : await addTodoApi(params);
    if (ok) {
      message.success(editingId.value ? '修改成功' : '添加成功');
      modalOpen.value = false;
      emit('refresh');
    } else {
      message.error('保存失败，请重试');
    }
  } finally {
    submitting.value = false;
  }
};

const handleFinish = async (todo: DashboardTodo) => {
  const ok = await finishTodoApi(todo.id);
  if (ok) {
    message.success('已完成');
    emit('refresh');
  } else {
    message.error('操作失败，请重试');
  }
};

const handleDelete = async (todo: DashboardTodo) => {
  const ok = await deleteTodoApi(todo.id);
  if (ok) {
    message.success('删除成功');
    emit('refresh');
  } else {
    message.error('删除失败，请重试');
  }
};
</script>

<style lang="scss" scoped>
.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  &:last-child {
    border-bottom: none;
  }
}

.priority-tag {
  flex-shrink: 0;
  margin-inline-end: 0;
}

.todo-main {
  flex: 1;
  min-width: 0;
}

.todo-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.todo-meta {
  margin-top: 2px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
