<template>
  <div class="bpm-designer">
    <!-- 顶部工具栏 -->
    <div class="bpm-designer-toolbar">
      <a-button @click="handleBack">
        <ArrowLeftOutlined /> 返回
      </a-button>
      <span class="bpm-designer-title">{{ definitionName || '流程设计' }}</span>
      <a-button-group>
        <a-button @click="handleUndo" title="撤销">
          <UndoOutlined />
        </a-button>
        <a-button @click="handleRedo" title="恢复">
          <RedoOutlined />
        </a-button>
        <a-button @click="handleZoomReset" title="自适应画布">
          <CompressOutlined />
        </a-button>
      </a-button-group>
      <div class="bpm-designer-spacer"></div>
      <span class="bpm-designer-form-label">在线表单：</span>
      <a-select v-model:value="boundFormId" class="bpm-designer-form-select" allowClear placeholder="不使用在线表单"
        @change="handleBindForm">
        <a-select-option v-for="form in formOptions" :key="form.id" :value="form.id">
          {{ form.formName }}
        </a-select-option>
      </a-select>
      <a-tag v-if="dirty" color="orange">有未保存更改</a-tag>
      <a-button type="primary" :loading="saving" @click="handleSave">
        <SaveOutlined /> 保存流程图
      </a-button>
    </div>

    <div class="bpm-designer-body">
      <!-- 画布 -->
      <div ref="canvasRef" class="bpm-designer-canvas"></div>

      <!-- 属性侧栏 -->
      <div class="bpm-designer-panel">
        <template v-if="!selectedElement">
          <div class="bpm-panel-empty">
            <p>左侧调色板仅提供流程白名单元素：</p>
            <ul>
              <li>开始事件 / 结束事件</li>
              <li>审批节点（用户任务）</li>
              <li>抄送节点（知会不审批）</li>
              <li>排他网关（条件分支）</li>
              <li>并行网关（多分支同时进行）</li>
            </ul>
            <p><b>操作方法</b>：点击左上角调色板图标 → 移动鼠标到画布目标位置 → 单击放置；</p>
            <p>将鼠标悬停在元素上可拖出连线（或用调色板第一个连线工具），把开始事件连到审批节点、最终连到结束事件。</p>
            <p>选中画布中的元素后在此配置属性。</p>
          </div>
        </template>
        <template v-else>
          <h4 class="bpm-panel-title">{{ panelTitle }}</h4>

          <div class="bpm-panel-field">
            <label>节点名称</label>
            <a-input v-model:value="panel.name" :maxlength="100" allowClear
              :disabled="selectedType === 'bpmn:StartEvent' || selectedType === 'bpmn:EndEvent'" @change="applyName" />
          </div>

          <!-- 用户任务：审批方式 + 审批人规则 -->
          <template v-if="selectedType === 'bpmn:UserTask'">
            <div class="bpm-panel-field">
              <label>审批方式</label>
              <a-radio-group v-model:value="panel.approvalMode" @change="applyApprovalMode">
                <a-radio :value="1">或签</a-radio>
                <a-radio :value="2">会签</a-radio>
              </a-radio-group>
              <div class="bpm-panel-hint">{{ approvalModeText(panel.approvalMode) }}</div>
            </div>
            <div class="bpm-panel-field">
              <label>审批人规则（多规则取并集）</label>
              <div v-for="(rule, idx) in panel.rules" :key="idx" class="bpm-rule-row">
                <a-select v-model:value="rule.ruleType" style="width: 130px" @change="applyRules">
                  <a-select-option :value="1">指定用户</a-select-option>
                  <a-select-option :value="2">指定角色</a-select-option>
                  <a-select-option :value="3">发起人自选</a-select-option>
                  <a-select-option :value="4">部门负责人</a-select-option>
                  <a-select-option :value="5">直属上级</a-select-option>
                  <a-select-option :value="6">表单字段</a-select-option>
                </a-select>
                <a-input v-if="rule.ruleType === 1" v-model:value="rule.ruleValue" style="flex: 1"
                  placeholder="用户名，逗号分隔" @change="applyRules" />
                <a-input v-else-if="rule.ruleType === 2" v-model:value="rule.ruleValue" style="flex: 1"
                  placeholder="角色编码" @change="applyRules" />
                <span v-else-if="rule.ruleType === 3" class="bpm-rule-hint">发起时选择</span>
                <a-input v-else-if="rule.ruleType === 4" v-model:value="rule.ruleValue" style="flex: 1"
                  placeholder="部门编码，留空=发起人所在部门" @change="applyRules" />
                <a-select v-else-if="rule.ruleType === 5" v-model:value="rule.ruleValue" style="flex: 1"
                  placeholder="第几级主管" @change="applyRules">
                  <a-select-option value="1">第1级（本部门负责人）</a-select-option>
                  <a-select-option value="2">第2级（上级部门负责人）</a-select-option>
                  <a-select-option value="3">第3级</a-select-option>
                  <a-select-option value="4">第4级</a-select-option>
                  <a-select-option value="5">第5级</a-select-option>
                </a-select>
                <a-input v-else-if="rule.ruleType === 6" v-model:value="rule.ruleValue" style="flex: 1"
                  placeholder="表单字段名（值为用户名或用户名数组）" @change="applyRules" />
                <a-input v-else v-model:value="rule.ruleValue" style="flex: 1" @change="applyRules" />
                <a @click="removeRule(idx)">
                  <MinusCircleOutlined />
                </a>
              </div>
              <a @click="addRule">
                <PlusCircleOutlined /> 添加规则
              </a>
            </div>
          </template>

          <!-- 抄送节点：抄送对象规则（仅指定用户/角色） -->
          <template v-if="selectedType === 'bpmn:SendTask'">
            <div class="bpm-panel-field">
              <label>抄送对象规则（多规则取并集）</label>
              <div v-for="(rule, idx) in panel.rules" :key="idx" class="bpm-rule-row">
                <a-select v-model:value="rule.ruleType" style="width: 130px" @change="applyRules">
                  <a-select-option :value="1">指定用户</a-select-option>
                  <a-select-option :value="2">指定角色</a-select-option>
                </a-select>
                <a-input v-model:value="rule.ruleValue" style="flex: 1"
                  :placeholder="rule.ruleType === 1 ? '用户名，逗号分隔' : '角色编码'" @change="applyRules" />
                <a @click="removeRule(idx)">
                  <MinusCircleOutlined />
                </a>
              </div>
              <a @click="addRule">
                <PlusCircleOutlined /> 添加规则
              </a>
              <div class="bpm-panel-hint">流转到该节点时向抄送对象推送通知，不影响流程推进。</div>
            </div>
          </template>

          <!-- 顺序流：条件表达式（可视化构造 + 手工输入） + 默认流 -->
          <template v-if="selectedType === 'bpmn:SequenceFlow'">
            <div class="bpm-panel-field">
              <label>条件表达式（网关出边）</label>
              <template v-if="panel.isGatewayOut">
                <div class="bpm-cond-builder">
                  <a-input v-model:value="panel.condVar" style="flex: 1" placeholder="变量名" />
                  <a-select v-model:value="panel.condOp" style="width: 88px">
                    <a-select-option value="==">等于</a-select-option>
                    <a-select-option value="!=">不等于</a-select-option>
                    <a-select-option value="&gt;">大于</a-select-option>
                    <a-select-option value="&gt;=">大于等于</a-select-option>
                    <a-select-option value="&lt;">小于</a-select-option>
                    <a-select-option value="&lt;=">小于等于</a-select-option>
                  </a-select>
                  <a-input v-model:value="panel.condValue" style="flex: 1" placeholder="比较值" />
                  <a-button size="small" @click="buildCondition">生成</a-button>
                </div>
                <div class="bpm-panel-hint">变量为业务表单字段或 applyUser；数字不加引号，文本自动加单引号。</div>
              </template>
              <a-input v-model:value="panel.condition" placeholder="如 amount > 5000" allowClear
                :disabled="!panel.isGatewayOut" @change="applyCondition" />
              <div class="bpm-panel-hint" v-if="!panel.isGatewayOut">仅排他网关的出边支持条件表达式</div>
              <div class="bpm-panel-hint" v-else>
                也可直接手写表达式；语法在保存时校验
              </div>
            </div>
            <div class="bpm-panel-field" v-if="panel.isGatewayOut">
              <a-checkbox :checked="panel.isDefaultFlow" @change="applyDefaultFlow">
                设为网关默认流（条件均不满足时走此边）
              </a-checkbox>
            </div>
          </template>

          <template v-if="selectedType === 'bpmn:ExclusiveGateway'">
            <div class="bpm-panel-hint">
              网关按出边上的条件表达式选择路径；请为每条出边配置条件，或指定其中一条为默认流。
            </div>
          </template>

          <template v-if="selectedType === 'bpmn:ParallelGateway'">
            <div class="bpm-panel-hint">
              并行网关无需条件：多入边=汇聚（所有分支到齐才继续），多出边=分叉（所有分支同时推进）。<br />
              注意：所有并行分支必须最终汇合到同一汇聚网关后再进入结束节点，避免分支各自直达结束。
            </div>
          </template>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { Modal, message } from 'ant-design-vue';
import Modeler from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
import {
  ArrowLeftOutlined, CompressOutlined, MinusCircleOutlined, PlusCircleOutlined,
  RedoOutlined, SaveOutlined, UndoOutlined,
} from '@ant-design/icons-vue';

import { XcModdleDescriptor, readRules, writeRules } from '@/components/xuanchen/businessprocess/bpmn/xcModdle';
import { approvalModeText } from '../businessprocess.data';
import {
  bindDefinitionFormApi, getDiagramApi, saveDiagramApi,
} from '../definition/definition.api';
import { getFormOptionsApi } from '../form/form.api';
import type { OnlineFormRecord } from '../form/form.types';

const EMPTY_DIAGRAM_XML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xc="http://xuanchen-monolithic.cn/schema/bpmn/xc"
  id="xc-definitions" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="xc-process" isExecutable="false">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="xc-diagram">
    <bpmndi:BPMNPlane id="xc-plane" bpmnElement="xc-process">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="182" y="182" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

const route = useRoute();
const router = useRouter();
const definitionId = String(route.params.id ?? '');

const canvasRef = ref<HTMLDivElement>();
const definitionName = ref('');
/** 绑定的在线表单（空 = 不使用在线表单） */
const boundFormId = ref('');
const formOptions = ref<OnlineFormRecord[]>([]);
const saving = ref(false);
const dirty = ref(false);
const selectedElement = ref<any>(null);
const selectedType = ref('');

/** 属性侧栏标题 */
const panelTitle = computed(() => {
  switch (selectedType.value) {
    case 'bpmn:StartEvent': return '开始事件';
    case 'bpmn:EndEvent': return '结束事件';
    case 'bpmn:UserTask': return '审批节点';
    case 'bpmn:SendTask': return '抄送节点';
    case 'bpmn:ExclusiveGateway': return '排他网关';
    case 'bpmn:ParallelGateway': return '并行网关';
    case 'bpmn:SequenceFlow': return '连线';
    default: return '属性';
  }
});

const panel = reactive({
  name: '',
  approvalMode: 1 as number,
  rules: [] as Array<{ ruleType: number; ruleValue: string }>,
  condition: '',
  isGatewayOut: false,
  isDefaultFlow: false,
  condVar: '',
  condOp: '==' as string,
  condValue: '',
});

let modeler: any = null;
let modeling: any = null;

/** 白名单调色板：仅 开始/结束/审批节点/排他网关 + 全局连线 */
const whitelistPaletteModule = {
  __init__: ['xcPaletteProvider'],
  xcPaletteProvider: ['type', XcPaletteProvider],
};

function XcPaletteProvider(this: any, palette: any, create: any, elementFactory: any, globalConnect: any, translate: any) {
  palette.registerProvider(this);
  this.getPaletteEntries = () => {
    const createAction = (type: string, group: string, className: string, title: string) => ({
      group,
      className,
      title,
      action: {
        dragstart: (event: any) => create.start(event, elementFactory.createShape({ type })),
        click: (event: any) => create.start(event, elementFactory.createShape({ type })),
      },
    });
    return [
      {
        title: translate('Activate the global connect tool'),
        className: 'bpmn-icon-connection-multi',
        group: 'tools',
        action: { click: (event: any) => globalConnect.toggle(event) },
      },
      { separator: true },
      createAction('bpmn:StartEvent', 'event', 'bpmn-icon-start-event-none', '创建开始事件'),
      createAction('bpmn:UserTask', 'activity', 'bpmn-icon-user-task', '创建审批节点'),
      createAction('bpmn:SendTask', 'activity', 'bpmn-icon-send-task', '创建抄送节点'),
      createAction('bpmn:ExclusiveGateway', 'gateway', 'bpmn-icon-gateway-xor', '创建排他网关'),
      createAction('bpmn:ParallelGateway', 'gateway', 'bpmn-icon-gateway-parallel', '创建并行网关'),
      createAction('bpmn:EndEvent', 'event', 'bpmn-icon-end-event-none', '创建结束事件'),
    ];
  };
}
XcPaletteProvider.$inject = ['palette', 'create', 'elementFactory', 'globalConnect', 'translate'];

const refreshPanel = (element: any) => {
  selectedElement.value = element;
  if (!element || !element.businessObject) {
    selectedType.value = '';
    return;
  }
  selectedType.value = element.type;
  panel.name = element.businessObject.name ?? '';
  panel.approvalMode = Number(element.businessObject.get('xc:approvalMode') ?? 1) || 1;
  panel.rules = readRules(element).map((r: { ruleType: number; ruleValue?: string }) => ({
    ruleType: r.ruleType, ruleValue: r.ruleValue ?? '',
  }));
  panel.condition = element.businessObject.get('xc:condition') ?? '';
  // 是否网关出边 + 是否默认流
  const source = element.source;
  panel.isGatewayOut = !!source && source.type === 'bpmn:ExclusiveGateway';
  panel.isDefaultFlow = !!(source?.businessObject?.default && source.businessObject.default === element.businessObject.id);
};

const applyName = () => {
  if (!modeling || !selectedElement.value) return;
  modeling.updateProperties(selectedElement.value, { name: panel.name });
};

const applyApprovalMode = () => {
  if (!modeling || !selectedElement.value) return;
  modeling.updateProperties(selectedElement.value, { 'xc:approvalMode': String(panel.approvalMode) });
};

const applyRules = () => {
  if (!modeling || !selectedElement.value) return;
  writeRules(modeling, selectedElement.value, panel.rules.filter((r) => r.ruleType));
};

const addRule = () => {
  panel.rules.push({ ruleType: 1, ruleValue: '' });
};

const removeRule = (idx: number) => {
  panel.rules.splice(idx, 1);
  applyRules();
};

const applyCondition = () => {
  if (!modeling || !selectedElement.value) return;
  modeling.updateProperties(selectedElement.value, { 'xc:condition': panel.condition });
};

/** 可视化条件构造：变量/操作符/值 -> SpEL（数字不加引号，其余加单引号） */
const buildCondition = () => {
  if (!panel.condVar.trim()) {
    message.warning('请先输入变量名');
    return;
  }
  const raw = panel.condValue.trim();
  const literal = raw !== '' && !Number.isNaN(Number(raw)) ? raw : `'${raw.replaceAll("'", "\\'")}'`;
  panel.condition = `${panel.condVar.trim()} ${panel.condOp} ${literal}`;
  applyCondition();
};

const applyDefaultFlow = () => {
  if (!modeling || !selectedElement.value) return;
  const flow = selectedElement.value;
  const source = flow.source;
  if (!source) return;
  if (panel.isDefaultFlow) {
    modeling.updateProperties(source, { default: flow.businessObject.id });
  } else if (source.businessObject?.default === flow.businessObject.id) {
    modeling.updateProperties(source, { default: undefined });
  }
};

const handleUndo = () => modeler?.get('commandStack')?.undo();
const handleRedo = () => modeler?.get('commandStack')?.redo();
const handleZoomReset = () => modeler?.get('canvas')?.zoom('fit-viewport', 'auto');

const handleSave = async () => {
  if (!modeler) return;
  saving.value = true;
  try {
    const { xml } = await modeler.saveXML({ format: true });
    const res = await saveDiagramApi(definitionId, xml);
    if (res.code !== 200) {
      Modal.error({ title: '保存失败（服务端校验未通过）', content: res.msg || '流程图校验失败' });
      return;
    }
    message.success('保存成功！');
    dirty.value = false;
  } catch (e: any) {
    // saveXML 抛错通常是 XML 生成问题（如悬空元素）
    Modal.error({ title: '保存失败', content: e?.message || '流程图导出失败，请检查画布元素' });
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  if (dirty.value) {
    Modal.confirm({
      title: '有未保存的更改',
      content: '离开将丢失未保存的流程图修改，确定离开吗？',
      okText: '离开',
      cancelText: '继续编辑',
      onOk: () => router.back(),
    });
  } else {
    router.back();
  }
};

onBeforeRouteLeave((_to, _from, next) => {
  if (!dirty.value) {
    next();
    return;
  }
  Modal.confirm({
    title: '有未保存的更改',
    content: '离开将丢失未保存的流程图修改，确定离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false),
  });
});

onMounted(async () => {
  await nextTick();
  if (!canvasRef.value || !definitionId) return;
  modeler = new Modeler({
    container: canvasRef.value,
    keyboard: { bindTo: document },
    additionalModules: [whitelistPaletteModule],
    moddleExtensions: { xc: XcModdleDescriptor },
  });
  modeling = modeler.get('modeling');
  modeler.on('commandStack.changed', () => {
    dirty.value = true;
  });
  modeler.on('selection.changed', (event: any) => {
    refreshPanel(event.newSelection?.[0] ?? null);
  });
  modeler.on('element.changed', (event: any) => {
    if (selectedElement.value && event.element?.id === selectedElement.value.id) {
      refreshPanel(event.element);
    }
  });
  try {
    const { bpmnXml, name, onlineFormId } = await getDiagramApi(definitionId);
    definitionName.value = name;
    boundFormId.value = onlineFormId || '';
    await modeler.importXML(bpmnXml && bpmnXml.trim() ? bpmnXml : EMPTY_DIAGRAM_XML);
    modeler.get('canvas').zoom('fit-viewport', 'auto');
    dirty.value = false;
  } catch (e: any) {
    message.error(e?.message || '流程图加载失败');
  }
  // 撤销/恢复后同步 commandStack 基线
  modeler.get('commandStack')?.clear?.();
  // 在线表单下拉选项（加载失败不阻塞设计器）
  try {
    const res = await getFormOptionsApi();
    if (res.code === 200) {
      formOptions.value = res.data ?? [];
    }
  } catch {
    /* 忽略：表单下拉为空仅影响绑定 */
  }
});

/** 绑定/解绑在线表单（独立保存，不影响 BPMN 未保存状态） */
const handleBindForm = async () => {
  try {
    const res = await bindDefinitionFormApi(definitionId, boundFormId.value || '');
    if (res.code !== 200) {
      message.error(res.msg || '绑定失败');
    }
  } catch {
    message.error('绑定失败，请重试');
  }
};

onBeforeUnmount(() => {
  modeler?.destroy?.();
  modeler = null;
});
</script>

<style>
/* 全屏设计器（非 scoped：需覆盖 bpmn-js 内部样式与 body 滚动） */
.bpm-designer {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  z-index: 100;
}

.bpm-designer-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.bpm-designer-title {
  font-weight: 600;
  font-size: 15px;
}

.bpm-designer-form-label {
  font-size: 13px;
  color: #4e5969;
  white-space: nowrap;
}

.bpm-designer-form-select {
  width: 220px;
}

.bpm-designer-spacer {
  flex: 1;
}

.bpm-designer-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.bpm-designer-canvas {
  flex: 1;
  min-width: 0;
  height: 100%;
}

.bpm-designer-canvas .djs-container {
  background: #fafafa;
}

.bpm-designer-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  padding: 16px;
  overflow-y: auto;
}

.bpm-panel-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.bpm-panel-empty {
  font-size: 12px;
  color: #999;
  line-height: 2;
}

.bpm-panel-field {
  margin-bottom: 16px;
}

.bpm-panel-field>label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.bpm-panel-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.6;
}

.bpm-rule-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.bpm-cond-builder {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
</style>
