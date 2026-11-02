<template>
  <div class="bind-success-page">
    <div class="success-card">
      <div class="success-icon">
        <AIcon type="CheckCircleFilled" />
      </div>
      <div class="success-title">{{ tText('mobile.product.success.title', '绑定成功') }}</div>
      <div class="success-description">
        {{ tText('mobile.product.success.description', '网关已完成绑定') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { onBeforeRouteLeave } from 'vue-router'

import { useMenuStore } from '@jetlinks-web-core/store'

const { t: $t } = useI18n()
const menuStore = useMenuStore()
const route = useRoute()

const allowLeave = ref(false)

const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const getQueryValue = (value: unknown) => Array.isArray(value) ? String(value[0] || '') : (value ? String(value) : '')
const deviceId = computed(() => getQueryValue(route.query.deviceId))

const keepCurrentHistory = () => {
  window.history.pushState(null, '', window.location.href)
}

const handlePopState = () => {
  if (!allowLeave.value) {
    keepCurrentHistory()
  }
}

onBeforeRouteLeave(() => allowLeave.value)

onMounted(() => {
  keepCurrentHistory()
  window.addEventListener('popstate', handlePopState)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePopState)
})
</script>

<style scoped lang="less">
.bind-success-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  padding: 24px 16px;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3f7 100%);
}

.success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 480px;
  padding: 32px 20px 24px;
}

.success-icon {
  color: #52c41a;
  font-size: 56px;
  line-height: 1;
}

.success-title {
  margin-top: 20px;
  color: #111827;
  font-size: 26px;
  font-weight: 600;
  line-height: 34px;
}

.success-description {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
}
</style>
