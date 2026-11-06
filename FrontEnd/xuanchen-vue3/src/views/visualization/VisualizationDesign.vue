<template>
  <a-card>
    <div class="bs-gallery-head">
      <a-space>
        <a-input v-model:value="keyword" placeholder="按名称/编码搜索" allowClear style="width: 240px"
          @press-enter="loadData" @change="onKeywordClear" />
        <a-button type="primary" @click="loadData">
          <template #icon>
            <SearchOutlined />
          </template>查询
        </a-button>
      </a-space>
      <a-space>
        <a-button @click="goList">
          <template #icon>
            <UnorderedListOutlined />
          </template>列表管理
        </a-button>
        <a-button type="primary" @click="handleAdd">
          <template #icon>
            <PlusOutlined />
          </template>新建设计
        </a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="!records.length && !loading" description="还没有大屏，点击右上角「新建设计」开始" style="margin: 60px 0" />
      <div class="bs-gallery">
        <div v-for="record in records" :key="record.id" class="bs-card">
          <div class="bs-card-cover" @click="handleDesign(record)">
            <img v-if="record.thumbnail" :src="getImageView(String(record.thumbnail)) || ''" alt="" />
            <div v-else class="bs-card-cover-empty">
              <FundProjectionScreenOutlined />
              <span>点击进入设计</span>
            </div>
            <a-tag class="bs-card-status" :color="screenStatusColor(record.status as number)">
              {{ screenStatusText(record.status as number) }}
            </a-tag>
          </div>
          <div class="bs-card-info">
            <div class="bs-card-name" :title="String(record.screenName ?? '')">{{ record.screenName }}</div>
            <div class="bs-card-code">{{ record.screenCode }}</div>
            <div class="bs-card-actions">
              <a @click="handleDesign(record)">
                <HighlightOutlined /> 设计
              </a>
              <a-divider type="vertical" />
              <a v-if="record.status === 1" @click="handlePreview(record)">
                <EyeOutlined /> 预览
              </a>
              <span v-else class="op-disabled">
                <EyeOutlined /> 预览
              </span>
              <a-divider type="vertical" />
              <a-popconfirm v-if="record.status === 1" title="确定停用该大屏吗？" placement="top"
                @confirm="() => handleUnpublish(record.id)">
                <a>
                  <PauseCircleOutlined /> 停用
                </a>
              </a-popconfirm>
              <a-popconfirm v-else title="确定发布该大屏吗？" placement="top"
                @confirm="() => handlePublish(record.id)">
                <a>
                  <SendOutlined /> 发布
                </a>
              </a-popconfirm>
              <a-divider type="vertical" />
              <a @click="handleEdit(record)">
                <EditOutlined /> 信息
              </a>
            </div>
          </div>
        </div>
      </div>
    </a-spin>

    <Operation :operation-title="operationTitle" ref="operationRef" @childOK="loadData" />
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  EditOutlined, EyeOutlined, FundProjectionScreenOutlined, HighlightOutlined,
  PauseCircleOutlined, PlusOutlined, SearchOutlined, SendOutlined, UnorderedListOutlined,
} from '@ant-design/icons-vue';

import Operation from './modal/Operation.vue';
import { VisualizationApiUrl, publishScreenApi, unpublishScreenApi } from './visualization.api';
import { screenStatusColor, screenStatusText } from './visualization.data';
import { getAction } from '@/utils/httpAction';
import { getImageView } from '@/utils/ImageUtil';
import type { VisualizationRecord } from './visualization.types';
import type { PageResult } from '@/types/api';

const router = useRouter();
const records = ref<VisualizationRecord[]>([]);
const loading = ref(false);
const keyword = ref('');
const operationTitle = ref('');
const operationRef = ref<{ add: () => void; edit: (record: VisualizationRecord) => void } | null>(null);

const loadData = async () => {
  loading.value = true;
  try {
    const kw = keyword.value.trim();
    const res = await getAction<PageResult<VisualizationRecord>>(VisualizationApiUrl.INDEX_LIST, {
      pageNo: 1,
      pageSize: 100,
      screenName: kw || undefined,
      screenCode: kw || undefined,
    });
    if (res.code === 200 && res.data) {
      records.value = res.data.records ?? [];
    } else {
      message.error(res.msg || '大屏列表加载失败！');
    }
  } catch {
    // HTTP 层错误已由拦截器统一提示
  } finally {
    loading.value = false;
  }
};

const onKeywordClear = () => {
  if (!keyword.value) loadData();
};

const handleAdd = () => {
  operationTitle.value = '新建设计';
  operationRef.value?.add();
};

const handleEdit = (record: VisualizationRecord) => {
  operationTitle.value = '编辑大屏信息';
  operationRef.value?.edit(record);
};

const handleDesign = (record: VisualizationRecord) => {
  const id = String(record.id ?? '');
  if (!id) return;
  router.push({ name: 'visualizationDesigner', params: { id } });
};

const handlePreview = (record: VisualizationRecord) => {
  const code = String(record.screenCode ?? '');
  if (!code) return;
  const href = router.resolve({ name: 'visualizationViewer', params: { code } }).href;
  window.open(href, '_blank');
};

/** 发布：状态置为已发布，展示页即可访问 */
const handlePublish = async (id?: string) => {
  if (!id) return;
  const res = await publishScreenApi(id);
  if (res.code === 200) {
    message.success('发布成功！');
    loadData();
  } else {
    message.error(res.msg || '发布失败');
  }
};

/** 停用：状态置为停用，展示页不可访问 */
const handleUnpublish = async (id?: string) => {
  if (!id) return;
  const res = await unpublishScreenApi(id);
  if (res.code === 200) {
    message.success('停用成功！');
    loadData();
  } else {
    message.error(res.msg || '停用失败');
  }
};

const goList = () => {
  router.push({ name: 'visualizationlist' });
};

onMounted(loadData);
</script>

<style scoped>
.bs-gallery-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.bs-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.bs-card {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.bs-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.bs-card-cover {
  position: relative;
  height: 158px;
  background:
    radial-gradient(circle at 30% 20%, rgba(64, 169, 255, 0.25), transparent 60%),
    #0b1020;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bs-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.bs-card-cover-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(207, 227, 255, 0.6);
  font-size: 13px;
}

.bs-card-cover-empty :deep(.anticon) {
  font-size: 34px;
}

.bs-card-status {
  position: absolute;
  right: 6px;
  top: 6px;
}

.bs-card-info {
  padding: 10px 12px;
}

.bs-card-name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bs-card-code {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.bs-card-actions {
  margin-top: 8px;
  font-size: 13px;
}

.op-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
