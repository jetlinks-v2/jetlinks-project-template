<template>
  <a-modal
    :open="open"
    class="resource-expansion-support-modal"
    :footer="null"
    :title="null"
    :width="560"
    centered
    @cancel="close"
    @update:open="emit('update:open', $event)"
  >
    <div class="resource-expansion-support">
      <div class="resource-expansion-support__icon" aria-hidden="true">
        <AIcon type="CustomerServiceOutlined" />
      </div>
      <h2>{{ t('ProjectUsage.support.title') }}</h2>
      <p>{{ t('ProjectUsage.support.description') }}</p>
      <a-qrcode value="https://www.jetlinks.cn/" :size="320" />
      <a-button class="resource-expansion-support__confirm" type="primary" size="large" @click="emit('update:open', false)">
        {{ t('ProjectUsage.support.confirm') }}
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ (event: 'update:open', value: boolean): void }>()
const { t } = useI18n()

const close = () => emit('update:open', false)
</script>

<style scoped lang="less">
.resource-expansion-support {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-5) var(--space-4) var(--space-3);
  text-align: center;

  &__icon {
    display: grid;
    width: 5rem;
    height: 5rem;
    place-items: center;
    border-radius: 50%;
    background: var(--jet-theme-primary-soft);
    color: var(--jet-theme-primary);
    font-size: var(--fs-32);
  }

  h2 {
    margin: var(--space-4) 0 var(--space-2);
    color: var(--jet-theme-text);
    font-size: var(--fs-24);
    font-weight: 600;
    line-height: var(--lh-tight);
  }

  p {
    max-width: 30rem;
    margin: 0 0 var(--space-4);
    color: var(--jet-theme-text-secondary);
    font-size: var(--fs-16);
    line-height: var(--lh-relaxed);
  }

  :deep(.ant-qrcode) {
    margin-bottom: var(--space-5);
  }

  :deep(.ant-btn) {
    min-width: 12.5rem;
  }

  &__confirm {
    background: var(--jet-theme-primary);
    border-color: var(--jet-theme-primary);

    &:hover,
    &:focus {
      background: var(--jet-theme-primary);
      border-color: var(--jet-theme-primary);
    }
  }
}

:deep(.ant-modal-content) {
  border-radius: var(--r-3);
}
</style>
