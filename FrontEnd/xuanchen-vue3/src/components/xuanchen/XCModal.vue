<template>
  <a-modal v-model:open="visible" :width="width" :wrap-class-name="wrapClassNames" :footer="hideFooter ? null : undefined"
    :ok-text="okText" :cancel-text="cancelText" :closable="closable" :destroy-on-close="destroyOnClose" @ok="handleOk"
    @cancel="handleCancel">
    <template #title>
      <div class="modal-title">
        <span>{{ title }}</span>
        <div class="title-actions">
          <slot name="title-actions">
            <a-tooltip v-if="showFullscreenBtn" :title="isFullScreen ? '退出全屏' : '全屏'">
              <a-button type="text" @click="toggleFullScreen">
                <FullscreenExitOutlined v-if="isFullScreen" />
                <FullscreenOutlined v-else />
              </a-button>
            </a-tooltip>
            <a-button type="text" @click="handleCancel">
              <CloseOutlined />
            </a-button>
          </slot>
        </div>
      </div>
    </template>
    <slot></slot>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { PropType } from 'vue';

const props = defineProps({
  title: {
    type: String,
    default: '操作'
  },
  width: {
    type: [String, Number],
    default: 800
  },
  okText: {
    type: String,
    default: '确认'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  closable: {
    type: Boolean,
    default: false
  },
  showFullscreenBtn: {
    type: Boolean,
    default: true
  },
  open: {
    type: Boolean,
    default: false
  },
  defaultFullscreen: {
    type: Boolean,
    default: false
  },
  /** 隐藏底部「确认/取消」按钮（纯展示类弹窗，如设计器） */
  hideFooter: {
    type: Boolean,
    default: false
  },
  /** 关闭时销毁弹窗内容（预览类弹窗用：避免 iframe/img 残留旧内容） */
  destroyOnClose: {
    type: Boolean,
    default: false
  },
  /** 追加到弹窗根节点的 class（样式隔离/尺寸定制用，全屏时仍会带上 xc-full-modal） */
  wrapClassName: {
    type: String,
    default: ''
  },
  /** 关闭前拦截：返回 false（或 resolve false）则不关闭，用于「未保存改动」确认 */
  beforeClose: {
    type: Function as PropType<() => boolean | Promise<boolean>>,
    default: undefined
  }
});

const emit = defineEmits(['ok', 'cancel', 'update:open']);
const visible = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val)
});
const isFullScreen = ref(props.defaultFullscreen);

/** 全屏样式类不能丢：xc-full-modal 负责把弹窗撑满视口 */
const wrapClassNames = computed(() =>
  [props.wrapClassName, isFullScreen.value ? 'xc-full-modal' : ''].filter(Boolean).join(' ')
);

// 每次打开都回到 defaultFullscreen 决定的初始状态，避免沿用上次关窗前的全屏状态
watch(
  () => props.open,
  (open) => {
    if (open) {
      isFullScreen.value = props.defaultFullscreen;
    }
  }
);

// 切换全屏状态
const toggleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
};

// 处理确认
const handleOk = () => {
  emit('ok');
};

// 处理取消
const handleCancel = async () => {
  // 关闭前拦截（如设计器的未保存改动确认）；返回 false 则维持打开
  if (props.beforeClose) {
    const canClose = await props.beforeClose();
    if (!canClose) {
      return;
    }
  }
  visible.value = false;
  isFullScreen.value = false;
  emit('cancel');
};

// 暴露方法
defineExpose({
  toggleFullScreen,
  isFullScreen
});
</script>

<style lang="scss">
/**
 * Keep fullscreen styles in this NON-scoped block on purpose:
 * 1. the modal is teleported to body, outside the component scope;
 * 2. `:global(.x) { .y { ... } }` inside a scoped block is compiled to `.x { ... }`,
 *    dropping the nested `.y` selector, so the fullscreen class would only land
 *    on the mask wrapper and the dialog never resizes.
 * See src/components/xuanchen/XCModal.vue history / memory for details.
 */
.xc-full-modal {
  .ant-modal {
    max-width: 100%;
    width: 100% !important;
    top: 0;
    left: 0;
    margin: 0;
    padding-bottom: 0;
    border-radius: 0;
  }

  .ant-modal-content {
    display: flex;
    flex-direction: column;
    height: 100vh;
    border-radius: 0;
  }

  .ant-modal-body {
    flex: 1;
    min-height: 0;
    overflow: auto;
    /* 全屏弹窗多为表单布局：子元素（如 a-row gutter 负外边距）偶发 1~12px 横向溢出
       会在底部拖出一条横向滚动条，直接裁掉；纵向滚动不受影响 */
    overflow-x: hidden;
  }

  .ant-modal-header,
  .ant-modal-footer {
    flex-shrink: 0;
  }
}
</style>

<style lang="scss" scoped>
.modal-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .title-actions {
    display: flex;
    gap: 0px;
  }
}
</style>