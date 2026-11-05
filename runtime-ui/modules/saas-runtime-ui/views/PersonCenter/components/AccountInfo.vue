<script setup lang="ts">
import globalI18n from '@jetlinks-web-core/locales'

import { computed, onMounted, ref, type Component } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { EditOutlined } from '@ant-design/icons-vue'

import defaultAvatar from '@saas-runtime-ui/assets/default_handsome.jpg'
import { getSelfIdentities_api } from '@jetlinks-web-core/api/account/center'
import { useUserStore, type UserInfo } from '@jetlinks-web-core/store/user'
import IdentityEditDialog from './IdentityEditDialog.vue'
import { moduleRegistry } from '@jetlinks-web-core/utils/module-registry'

const ThirdPartyAccountBindingRows = moduleRegistry.getResourceItem<Component>(
  'authentication-manager-ui', 'components', 'ThirdPartyAccountBindingRows',
)

interface IdentityItem {
  id: string
  provider: string
  identity: string
  isPrimary?: boolean
}

type UserWithContact = Partial<UserInfo> & {
  avatar?: string
  telephone?: string
  email?: string
}

const emit = defineEmits<{
  (e: 'open-edit-password'): void
}>()

const user = useUserStore()
const { t } = useI18n()
const identityList = ref<IdentityItem[]>([])
const identityDialogOpen = ref(false)
const editingProvider = ref<'mobile' | 'email'>('mobile')

const userInfo = computed<UserWithContact>(() => user.userInfo || {})
const displayName = computed(() => userInfo.value.name || userInfo.value.username || '--')
const displayAccount = computed(() => userInfo.value.username || '--')
const avatarUrl = computed(() => userInfo.value.avatar || defaultAvatar)

const phoneIdentity = computed(() => getPrimaryIdentity('mobile'))
const emailIdentity = computed(() => getPrimaryIdentity('email'))
const safePhone = computed(() => phoneIdentity.value?.identity || userInfo.value.telephone)
const safeEmail = computed(() => emailIdentity.value?.identity || userInfo.value.email)

const accountItems = computed(() => [
  { label: globalI18n.global.t('AccountContent.accountId'), value: String(userInfo.value.id || '--') },
  { label: globalI18n.global.t('SaasManager.generated.f63da5b6950e'), value: user.accountTypeName || '--' },
  { label: globalI18n.global.t('Region.detail.username'), value: displayAccount.value },
  { label: globalI18n.global.t('AccountContent.secureMobile'), value: maskIdentity(safePhone.value, 'mobile'), provider: 'mobile' as const },
  { label: globalI18n.global.t('AccountContent.secureEmail'), value: maskIdentity(safeEmail.value, 'email'), provider: 'email' as const },
])

const getPrimaryIdentity = (provider: string) => {
  const list = identityList.value.filter(item => item.provider === provider)
  return list.find(item => item.isPrimary) || list[0]
}

const maskIdentity = (value?: string, type?: 'mobile' | 'email') => {
  if (!value) return globalI18n.global.t('AccountContent.unbound')
  if (type === 'mobile') return value.replace(/^(.{3}).*(.{4})$/, '$1****$2')
  if (type === 'email') return value.replace(/^(.{2}).*(@.*)$/, '$1****$2')
  return value
}

const formatDateTime = (value?: number | string) => {
  if (!value) return '--'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : '--'
}

const loadIdentities = async () => {
  const resp = await getSelfIdentities_api()
  identityList.value = resp.result || []
}

const openIdentityDialog = (provider: 'mobile' | 'email') => {
  editingProvider.value = provider
  identityDialogOpen.value = true
}

const refreshIdentityInfo = async () => {
  await Promise.all([loadIdentities(), user.getUserInfo()])
}

onMounted(async () => {
  await user.getUserInfo()
  await loadIdentities()
})
</script>

<template>
  <ContentPanel class="account-card">
    <div class="account-card__profile">
      <a-avatar :src="avatarUrl" :size="56" />
      <div class="account-card__name">
        <h2>{{ displayName }}</h2>
        <span>{{ displayAccount }}</span>
      </div>
    </div>

    <div class="account-card__section-title">{{ $t('AccountContent.accountInfo') }}</div>
    <dl class="account-card__list">
      <div
        v-for="item in accountItems"
        :key="item.label"
        class="account-card__item"
      >
        <dt>{{ item.label }}</dt>
        <dd>
          <span>{{ item.value }}</span>
          <a-tooltip v-if="item.provider" :title="t('PersonCenter.common.edit')">
            <a-button
              type="link"
              size="small"
              class="account-card__edit"
              :aria-label="t('PersonCenter.common.edit')"
              @click="openIdentityDialog(item.provider)"
            >
              <EditOutlined />
            </a-button>
          </a-tooltip>
        </dd>
      </div>
      <component :is="ThirdPartyAccountBindingRows" v-if="ThirdPartyAccountBindingRows" />
      <div class="account-card__item">
        <dt>{{ $t('AccountContent.registerTime') }}</dt>
        <dd><span>{{ formatDateTime(userInfo.createTime) }}</span></dd>
      </div>
    </dl>

    <a-button block class="account-card__password" @click="emit('open-edit-password')">
      {{ $t('SaasManager.generated.0ae20ac3af1b') }}
    </a-button>

    <IdentityEditDialog
      v-model:open="identityDialogOpen"
      :provider="editingProvider"
      @success="refreshIdentityInfo"
    />
  </ContentPanel>
</template>

<style scoped lang="less" src="./AccountInfo.less" />
