<template>
  <div>
    <div v-if="items.length" class="card-list">
      <div
        v-for="item in items"
        :key="item.id"
        class="product-card"
        :class="{ selected: item.id === selectedId, disabled: Number(item.state) !== 1 }"
        tabindex="0"
        role="button"
        @click="$emit('select', item)"
        @keyup.enter="$emit('select', item)"
      >
        <img class="product-image" :src="item.photoUrl || device.deviceProduct" :alt="item.name" />
        <div class="product-content">
          <div class="product-top">
            <div class="product-name"><j-ellipsis>{{ item.name || '-' }}</j-ellipsis></div>
            <AIcon v-if="item.id === selectedId" type="CheckCircleFilled" class="select-icon" />
          </div>
          <div class="product-status">
            <a-tag :color="getStatusMeta(item.state).color">{{ getStatusMeta(item.state).text }}</a-tag>
            <span v-if="Number(item.state) !== 1" class="status-tip">
              {{ tText('mobile.product.list.status.tip', '禁用产品不可选择') }}
            </span>
          </div>
          <div v-if="item.describe" class="product-description">
            <j-ellipsis>{{ item.describe }}</j-ellipsis>
          </div>
        </div>
      </div>
    </div>
    <CloudEmpty v-else :description="tText('mobile.product.list.empty', '暂无产品，请先新增产品')" />

    <div v-if="hasMore" class="load-more">
      <a-button block :loading="loadingMore" @click="$emit('load-more')">
        {{ tText('mobile.product.list.more', '加载更多') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { device } from '@device-manager-ui/assets'

interface ProductCardItem {
  id: string
  name?: string
  state?: number | string
  photoUrl?: string
  describe?: string
}

defineEmits(['select', 'load-more'])

defineProps({
  items: {
    type: Array as PropType<ProductCardItem[]>,
    default: () => [],
  },
  selectedId: {
    type: String,
    default: '',
  },
  hasMore: {
    type: Boolean,
    default: false,
  },
  loadingMore: {
    type: Boolean,
    default: false,
  },
})

const { t: $t } = useI18n()

const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const getStatusMeta = (state?: number | string) => ({
  text: Number(state) === 1 ? $t('Product.index.660348-2') : $t('Product.index.660348-3'),
  color: Number(state) === 1 ? '#16a34a' : '#9ca3af',
})
</script>

<style scoped lang="less">
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}

.product-card.selected {
  border-color: #1677ff;
  box-shadow: 0 12px 28px rgba(22, 119, 255, 0.14);
}

.product-card.disabled {
  opacity: 0.72;
}

.product-image {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 14px;
  background: #f3f4f6;
  flex-shrink: 0;
}

.product-content {
  flex: 1;
  min-width: 0;
}

.product-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.product-name {
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  word-break: break-all;
}

.select-icon {
  color: #1677ff;
  font-size: 18px;
}

.product-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.status-tip,
.product-description {
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.product-description {
  margin-top: 8px;
}

.load-more {
  margin-top: 16px;
}
</style>
