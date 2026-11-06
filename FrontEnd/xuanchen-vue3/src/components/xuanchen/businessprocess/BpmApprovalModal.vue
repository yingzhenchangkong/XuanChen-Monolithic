<template>
  <XCModal v-model:open="visible" :title="title" :width="1080" @ok="handleOk"
    :ok-text="mode === 'approve' ? '同意' : '关闭'">
    <div class="bpm-approval-body">
      <!-- 左侧：单据信息 + 流程图 -->
      <div class="bpm-approval-left">
        <a-descriptions :column="2" size="small" bordered class="bpm-approval-info">
          <a-descriptions-item label="申请摘要" :span="2">{{ instance?.businessSummary || '-' }}</a-descriptions-item>
          <a-descriptions-item label="流程名称">{{ instance?.definitionName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发起人">{{ instance?.applyUserName || instance?.applyUser || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发起时间">{{ instance?.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="当前状态">
            <a-tag :color="instanceStatusColor(instance?.status)">{{ instanceStatusText(instance?.status) }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>

        <div v-if="formEntries.length" class="bpm-approval-form">
          <h4>申请内容</h4>
          <a-descriptions :column="2" size="small">
            <a-descriptions-item v-for="entry in formEntries" :key="entry[0]" :label="entry[0]">
              {{ entry[1] }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <h4 class="bpm-approval-diagram-title">流程图</h4>
        <div class="bpm-approval-diagram">
          <BpmnPreview :bpmn-xml="bpmnXml" :current-keys="currentKeys" :passed-keys="passedKeys" />
        </div>
      </div>

      <!-- 右侧：审批意见输入 / 时间线 -->
      <div class="bpm-approval-right">
        <template v-if="mode === 'approve'">
          <h4>审批意见</h4>
          <a-textarea v-model:value="opinion" placeholder="请输入审批意见（驳回时必填）" :rows="4" :maxlength="500" />
          <div class="bpm-reject-row">
            <a-select v-model:value="rejectTarget" style="width: 100%; margin-bottom: 8px"
              placeholder="驳回方式：默认办结" allowClear :options="rejectOptions" />
            <a-popconfirm :title="rejectTarget ? '确定驳回至该节点重审吗？' : '确定驳回该申请吗？驳回后流程整体办结。'"
              @confirm="handleReject">
              <a-button danger block :loading="rejecting">驳 回</a-button>
            </a-popconfirm>
            <a-button class="bpm-transfer-btn" @click="openTransfer">
              <SendOutlined /> 转办
            </a-button>
          </div>
        </template>
        <h4>审批记录</h4>
        <a-timeline class="bpm-approval-timeline">
          <a-timeline-item v-for="record in records" :key="record.id"
            :color="recordActionColor(record.action)">
            <div class="bpm-record-line">
              <b>{{ recordActionText(record.action) }}</b>
              <span class="bpm-record-user">{{ record.operatorName || record.operator }}</span>
            </div>
            <div class="bpm-record-sub">
              {{ record.nodeName ? `节点：${record.nodeName}　` : '' }}{{ record.createTime }}
            </div>
            <div v-if="record.opinion" class="bpm-record-opinion">{{ record.opinion }}</div>
          </a-timeline-item>
        </a-timeline>
      </div>
    </div>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferOpen" title="转办" ok-text="确认转办" :confirm-loading="transferring"
      :width="420" @ok="handleTransferOk">
      <a-form layout="vertical">
        <a-form-item label="被转办人" required>
          <a-select v-model:value="transferUser" show-search option-filter-prop="label" :options="userOptions"
            placeholder="请选择被转办人" />
        </a-form-item>
        <a-form-item label="转办说明">
          <a-textarea v-model:value="transferOpinion" :rows="3" :maxlength="200"
            placeholder="选填，将随转办记录留痕" />
        </a-form-item>
      </a-form>
    </a-modal>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { SendOutlined } from '@ant-design/icons-vue';

import XCModal from '@/components/xuanchen/XCModal.vue';
import BpmnPreview from '@/components/xuanchen/businessprocess/BpmnPreview.vue';
import {
  approveTaskApi, getDiagramStateApi, getInstanceRecordsApi, getRejectTargetsApi,
  getUserOptionsApi, rejectTaskApi, rejectTaskToNodeApi, transferTaskApi,
} from '@/views/businessprocess/businessprocess.api';
import type { BpmUserOption } from '@/views/businessprocess/businessprocess.types';
import {
  instanceStatusColor, instanceStatusText, recordActionColor, recordActionText,
} from '@/views/businessprocess/businessprocess.data';
import type {
  BpmInstanceRecord, BpmRecordItem, BpmRejectTarget,
} from '@/views/businessprocess/businessprocess.types';

const props = defineProps<{
  /** approve=审批（含同意/驳回），view=只读查看 */
  mode: 'approve' | 'view';
  taskId?: string;
  instanceId?: string;
  taskNodeName?: string;
}>();

const emit = defineEmits(['handled']);

const title = computed(() =>
  props.mode === 'approve' ? `审批：${props.taskNodeName || '处理任务'}` : '申请详情');

const visible = ref(false);
const opinion = ref('');
const rejecting = ref(false);
const instance = ref<BpmInstanceRecord | null>(null);
const records = ref<BpmRecordItem[]>([]);
const bpmnXml = ref('');
const currentKeys = ref<string[]>([]);
const passedKeys = ref<string[]>([]);

/** 驳回方式：''=办结；否则为驳回目标节点 key */
const rejectTarget = ref('');
const rejectTargets = ref<BpmRejectTarget[]>([]);
const rejectOptions = computed(() => [
  { label: '办结（终止流程）', value: '' },
  ...rejectTargets.value.map((t) => ({ label: `驳回到节点：${t.nodeName}`, value: t.nodeKey })),
]);

/** 转办 */
const transferOpen = ref(false);
const transferring = ref(false);
const transferUser = ref<string | undefined>(undefined);
const transferOpinion = ref('');
const userOptions = ref<Array<{ label: string; value: string }>>([]);

const formEntries = computed<Array<[string, unknown]>>(() => {
  try {
    const raw = (instance.value as any)?.formData;
    if (!raw) return [];
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Object.entries(obj ?? {});
  } catch {
    return [];
  }
});

const open = async (instanceRow?: BpmInstanceRecord, instanceIdOverride?: string) => {
  visible.value = true;
  opinion.value = '';
  records.value = [];
  bpmnXml.value = '';
  currentKeys.value = [];
  passedKeys.value = [];
  rejectTarget.value = '';
  const targetId = instanceIdOverride || instanceRow?.id;
  if (!targetId) return;
  try {
    const detail = await getInstanceRecordsApi(targetId);
    instance.value = detail.instance;
    records.value = detail.records ?? [];
  } catch (e: any) {
    message.error(e?.message || '加载审批详情失败');
  }
  try {
    const state = await getDiagramStateApi(targetId);
    bpmnXml.value = state.bpmnXml ?? '';
    currentKeys.value = state.currentNodeKey ? [state.currentNodeKey] : (state.pendingNodeKeys ?? []);
    passedKeys.value = state.passedNodeKeys ?? [];
  } catch (e: any) {
    message.error(e?.message || '加载流程图状态失败');
  }
  if (props.mode === 'approve' && props.taskId) {
    try {
      rejectTargets.value = await getRejectTargetsApi(props.taskId);
    } catch {
      rejectTargets.value = []; // 拉取失败不阻塞弹窗，仅无法选节点驳回
    }
  }
};

const handleOk = async () => {
  if (props.mode !== 'approve' || !props.taskId) {
    visible.value = false;
    return;
  }
  const res = await approveTaskApi(props.taskId, opinion.value.trim());
  if (res.code !== 200) {
    message.error(res.msg || '审批失败');
    return Promise.reject();
  }
  message.success(res.msg || '审批通过！');
  visible.value = false;
  emit('handled');
};

const handleReject = async () => {
  if (props.mode !== 'approve' || !props.taskId) return;
  if (!opinion.value.trim()) {
    message.warning('驳回必须填写审批意见');
    return;
  }
  rejecting.value = true;
  try {
    const res = rejectTarget.value
      ? await rejectTaskToNodeApi(props.taskId, rejectTarget.value, opinion.value.trim())
      : await rejectTaskApi(props.taskId, opinion.value.trim());
    if (res.code !== 200) {
      message.error(res.msg || '驳回失败');
      return;
    }
    message.success(rejectTarget.value ? '已驳回至指定节点' : '已驳回');
    visible.value = false;
    emit('handled');
  } finally {
    rejecting.value = false;
  }
};

const openTransfer = async () => {
  transferUser.value = undefined;
  transferOpinion.value = '';
  transferOpen.value = true;
  if (!userOptions.value.length) {
    try {
      const users = await getUserOptionsApi();
      userOptions.value = users.map((u) => ({ label: `${u.nickName}（${u.userName}）`, value: u.userName }));
    } catch (e: any) {
      message.error(e?.message || '加载用户列表失败');
    }
  }
};

const handleTransferOk = async () => {
  if (!props.taskId) return;
  if (!transferUser.value) {
    message.warning('请选择被转办人');
    return Promise.reject();
  }
  transferring.value = true;
  try {
    const res = await transferTaskApi(props.taskId, transferUser.value, transferOpinion.value.trim());
    if (res.code !== 200) {
      message.error(res.msg || '转办失败');
      return;
    }
    message.success('转办成功');
    transferOpen.value = false;
    visible.value = false;
    emit('handled');
  } finally {
    transferring.value = false;
  }
};

defineExpose({ open });
</script>

<style scoped>
.bpm-approval-body {
  display: flex;
  gap: 16px;
  min-height: 380px;
}

.bpm-approval-left {
  flex: 1.6;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.bpm-approval-right {
  flex: 1;
  min-width: 260px;
  border-left: 1px solid #f0f0f0;
  padding-left: 16px;
  overflow-y: auto;
}

.bpm-approval-info {
  margin-bottom: 12px;
}

.bpm-approval-form h4,
.bpm-approval-right h4 {
  font-size: 13px;
  margin: 8px 0 8px;
}

.bpm-approval-diagram-title {
  font-size: 13px;
  margin: 8px 0 8px;
}

.bpm-approval-diagram {
  flex: 1;
  min-height: 240px;
}

.bpm-approval-timeline {
  margin-top: 8px;
}

.bpm-reject-row {
  margin-top: 10px;
}

.bpm-transfer-btn {
  width: 100%;
  margin-top: 8px;
}

.bpm-record-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bpm-record-user {
  color: #666;
  font-size: 12px;
}

.bpm-record-sub {
  color: #999;
  font-size: 12px;
  margin-top: 2px;
}

.bpm-record-opinion {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  margin-top: 4px;
}
</style>
