<template>
  <section class="resource-usage-section" :aria-label="t('ProjectUsage.title')" :aria-busy="state.status === 'loading'">
    <div v-if="state.status === 'loading'" class="resource-usage-section__loading">
      <a-spin :tip="t('ProjectUsage.state.loading')" />
    </div>
    <CloudEmpty v-else-if="state.status === 'unavailable'" :description="state.message" type="page" />
    <a-result v-else-if="state.status === 'forbidden'" status="403" :title="state.message" />
    <a-result
      v-else-if="state.status === 'error'"
      status="error"
      :title="t('ProjectUsage.state.loadFailed')"
      :sub-title="state.message"
    >
      <template #extra>
        <a-button @click="emit('retry')">{{ t('ProjectUsage.retry') }}</a-button>
      </template>
    </a-result>
    <CloudEmpty v-else-if="state.status === 'empty'" :description="t('ProjectUsage.empty')" type="page" />
    <div v-else class="resource-usage-section__groups">
      <section v-for="group in groups" :key="group.key" class="resource-usage-group" :class="`resource-usage-group--${group.key}`">
        <h2>{{ t(`ProjectUsage.group.${group.key}`) }}</h2>
        <div class="resource-usage-group__list">
          <div v-if="!group.items.length" class="resource-usage-group__empty">{{ t('ProjectUsage.groupEmpty') }}</div>
          <ResourceUsageCard
            v-for="item in group.items"
            :key="item.id"
            :item="item"
            @expand="supportModalOpen = true"
          />
        </div>
      </section>
    </div>
    <ResourceExpansionSupportModal v-model:open="supportModalOpen" />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProjectResourceUsage, ViewState } from '../types'
import ResourceUsageCard from './ResourceUsageCard.vue'
import ResourceExpansionSupportModal from './ResourceExpansionSupportModal.vue'
import { groupResourceUsage } from '../resourceUsageGroups'

const props = defineProps<{ resources: ProjectResourceUsage[]; state: ViewState }>()
const emit = defineEmits<{ (event: 'retry'): void }>()
const { t } = useI18n()
const supportModalOpen = ref(false)
const groups = computed(() => groupResourceUsage(props.resources))
</script>

<style scoped>
.resource-usage-section {
  min-width: 0;
}
.resource-usage-section__loading {
  display: grid;
  min-height: 12rem;
  place-items: center;
}
.resource-usage-section__groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: var(--space-4);
}
.resource-usage-group {
  min-width: 0;
  overflow: hidden;
  border: 0.0625rem solid var(--jet-theme-border-secondary);
  border-radius: var(--jet-theme-radius);
  background: var(--jet-theme-bg-container);
}
.resource-usage-group h2 {
  margin: 0;
  padding: var(--space-4);
  border-bottom: 0.0625rem solid var(--jet-theme-border-secondary);
  color: var(--jet-theme-text);
  font-size: var(--fs-20);
  font-weight: 600;
}
.resource-usage-group__list {
  height: min(65rem, calc(100vh - 13rem));
  min-height: 24rem;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}
.resource-usage-group__empty {
  padding: var(--space-5);
  color: var(--jet-theme-text-disabled);
  text-align: center;
}
.resource-usage-group--basic { --usage-icon: var(--jet-theme-primary); --usage-icon-bg: var(--jet-theme-primary-soft); }
.resource-usage-group--ai {
  --usage-icon: color-mix(in srgb, var(--accent-blue) 50%, var(--accent-magenta));
  --usage-icon-bg: color-mix(in srgb, var(--accent-magenta) 10%, var(--jet-theme-bg-container));
}
.resource-usage-group--agent {
  --usage-icon: var(--accent-cyan);
  --usage-icon-bg: color-mix(in srgb, var(--accent-cyan) 15%, var(--jet-theme-bg-container));
}
.resource-usage-group--other { --usage-icon: var(--jet-theme-primary); --usage-icon-bg: var(--jet-theme-primary-soft); }
@media (max-width: 75rem) {
  .resource-usage-section__groups { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 48rem) {
  .resource-usage-section__groups { grid-template-columns: 1fr; }
  .resource-usage-group__list { height: auto; max-height: 30rem; min-height: 0; }
}
</style>
