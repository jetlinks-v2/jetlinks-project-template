<template>
  <article class="resource-card">
    <span class="resource-card__icon" aria-hidden="true"><AIcon :type="item.icon" /></span>
    <div class="resource-card__content">
      <div class="resource-card__heading">
        <strong :title="item.titleKey ? t(item.titleKey) : item.name">
          {{ item.titleKey ? t(item.titleKey) : item.name }}
        </strong>
        <span>{{ item.metric ? usageText(item.metric) : '--' }}</span>
      </div>
      <a-progress
        :percent="item.metric?.percent || 0"
        :show-info="false"
        :stroke-color="progressColor(item.metric?.percent || 0)"
        :aria-label="`${item.titleKey ? t(item.titleKey) : item.name}: ${item.metric ? usageText(item.metric) : '--'}`"
      />
      <p :title="descriptionText">
        {{ descriptionText }}
      </p>
    </div>
    <a-button class="resource-card__expand" size="small" @click="emit('expand')">
      <template #icon><AIcon type="PlusOutlined" /></template>
      {{ t('ProjectUsage.expand') }}
    </a-button>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { UsageDisplayItem } from '../resourceUsageGroups'
import { useUsageFormat } from '../useUsageFormat'

const props = defineProps<{ item: UsageDisplayItem }>()
const emit = defineEmits<{ (event: 'expand'): void }>()
const { t } = useI18n()
const { usageText, progressColor } = useUsageFormat()
const descriptionText = computed(() => props.item.description
  || (props.item.descriptionKey ? t(props.item.descriptionKey) : '')
  || t(props.item.metric ? 'ProjectUsage.description.default' : 'ProjectUsage.noMetrics'))
</script>

<style scoped lang="less" src="./ResourceUsageCard.less"></style>
