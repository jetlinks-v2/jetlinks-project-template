<script setup lang="ts">
import globalI18n from '@jetlinks-web-core/locales'

import { computed, ref, watch } from 'vue'
import { onlyMessage } from '@jetlinks-web/utils'
import { updateMeInfo_api } from '@jetlinks-web-core/api/account/center'
import { useUserStore } from '@jetlinks-web-core/store/user'

type EditType = 'mobile' | 'email'

const props = defineProps<{
  open: boolean
  provider: EditType
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const user = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = ref({
  telephone: '',
  email: '',
})

const title = computed(() => (props.provider === 'mobile' ? globalI18n.global.t('SaasManager.generated.3b54e5719938') : globalI18n.global.t('SaasManager.generated.466050bac96d')))
const fieldName = computed(() => (props.provider === 'mobile' ? 'telephone' : 'email'))
const fieldLabel = computed(() => (props.provider === 'mobile' ? globalI18n.global.t('AccountContent.secureMobile') : globalI18n.global.t('AccountContent.secureEmail')))
const placeholder = computed(() => (props.provider === 'mobile' ? globalI18n.global.t('Login.mobilePlaceholder') : globalI18n.global.t('Login.emailPlaceholder')))
const rules = computed(() => {
  if (props.provider === 'mobile') {
    return [{ pattern: /^1[3456789]\d{9}$/, message: globalI18n.global.t('Login.mobileInvalid') }]
  }
  return [{ type: 'email', message: globalI18n.global.t('SaasManager.generated.bdb7fd599d25') }]
})

const resetForm = () => {
  form.value = {
    telephone: user.userInfo?.telephone || '',
    email: user.userInfo?.email || '',
  }
  formRef.value?.clearValidate?.()
}

const close = () => {
  emit('update:open', false)
  resetForm()
}

const handleOk = async () => {
  await formRef.value?.validate?.()
  loading.value = true
  try {
    const payload = {
      ...user.userInfo,
      telephone: form.value.telephone,
      email: form.value.email,
    }
    const resp: any = await updateMeInfo_api(payload)
    if (resp?.status === 200 || resp?.success) {
      onlyMessage(globalI18n.global.t('Region.detail.saveSuccess'), 'success')
      await user.getUserInfo()
      emit('success')
      emit('update:open', false)
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  value => {
    if (value) resetForm()
  },
)
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    :confirm-loading="loading"
    :ok-text="$t('Region.detail.save')"
    :cancel-text="$t('Service.index.cancel')"
    :mask-closable="false"
    @ok="handleOk"
    @cancel="close"
  >
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="fieldLabel" :name="fieldName" :rules="rules">
        <a-input
          v-model:value="form[fieldName]"
          :placeholder="placeholder"
          :maxlength="provider === 'mobile' ? 11 : 64"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
