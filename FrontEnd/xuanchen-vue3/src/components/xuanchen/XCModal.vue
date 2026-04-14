<template>
  <a-modal v-model:open="visible" :width="width" :wrap-class-name="isFullScreen ? 'full-modal' : ''"
    :ok-text="okText" :cancel-text="cancelText" :closable="closable" @ok="handleOk" @cancel="handleCancel">
    <template #title>
      <div class="modal-title">
        <span>{{ title }}</span>
        <div class="title-actions">
          <slot name="title-actions">
            <a-button v-if="showFullscreenBtn" type="text" @click="toggleFullScreen">
              <FullscreenExitOutlined v-if="isFullScreen" />
              <FullscreenOutlined v-else />
            </a-button>
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
import { ref, watch } from 'vue';
import { FullscreenOutlined, FullscreenExitOutlined, CloseOutlined } from '@ant-design/icons-vue';

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
  }
});

const emit = defineEmits(['ok', 'cancel']);
const visible = ref(props.open);
const isFullScreen = ref(props.defaultFullscreen);
watch(() => props.open, (newVal) => {
  visible.value = newVal;
});
// 切换全屏状态
const toggleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
};

// 处理确认
const handleOk = () => {
  emit('ok');
};

// 处理取消
const handleCancel = () => {
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
.full-modal {
  .ant-modal {
    max-width: 100%;
    width: 100%;
    top: 0;
    left: 0;
    padding-bottom: 0;
    margin: 0;
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
    overflow: auto;
  }

  .ant-modal-header,
  .ant-modal-footer {
    flex-shrink: 0;
  }
}

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