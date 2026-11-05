<script setup lang="ts">
import globalI18n from '@jetlinks-web-core/locales'

import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { omit } from 'lodash-es'

import ConditionFilter, {
  buildQueryFilter,
  type ConditionFilterChangePayload,
  type ConditionFilterTerm,
} from '@jetlinks-web-core/components/ConditionFilter'
import { getAllNotice } from '@jetlinks-web-core/api/account/center'
import {
  changeAllStatus,
  changeStatus_api,
  getList_api,
} from '@jetlinks-web-core/api/account/notificationRecord'
import { useUserStore } from '@jetlinks-web-core/store/user'
import { useRouterParams } from '@jetlinks-web/hooks'
import { onlyMessage } from '@jetlinks-web/utils'
import { getMessageSearchColumns, messageTableColumns } from './messageColumns'
import ViewDialog from './ViewDialog.vue'
import { resolveNoticeTexts } from '@jetlinks-web-core/layout/components/noticeTextResolver'

interface NoticeProviderItem {
  name: string
  provider: string
  children: Array<{ provider: string; name?: string }>
}

const props = withDefaults(defineProps<{
  excludeProviders?: string[]
}>(), {
  excludeProviders: () => [],
})

const user = useUserStore()
const userRuntime = user as typeof user & { messageInfo?: Record<string, any> }
const routerParams = useRouterParams()

const noticeProviders = ref<NoticeProviderItem[]>([])
const filterTerms = ref<ConditionFilterTerm[]>([])
const submittedTerms = ref<ConditionFilterTerm[]>([])
const tableRef = ref()
const viewVisible = ref(false)
const viewItem = ref<Record<string, any>>({})

const providerIds = computed(() => noticeProviders.value.flatMap(item => item.children.map(child => child.provider)))

const searchColumns = computed(() => getMessageSearchColumns())

const defaultParams = computed(() => ({
  sorts: [{ name: 'notifyTime', order: 'desc' }],
  terms: [
    ...(providerIds.value.length
      ? [
        {
          terms: [
            {
              column: 'topicProvider',
              value: providerIds.value,
              termType: 'in',
            },
          ],
          type: 'and',
        },
      ]
      : []),
    ...(props.excludeProviders.length
      ? [{
          column: 'topicProvider',
          value: props.excludeProviders,
          termType: 'nin',
        }]
      : []),
  ],
}))

const queryParams = computed(() => ({
  terms: buildQueryFilter(submittedTerms.value, searchColumns.value).terms,
}))

const formatDateTime = (value?: number | string) => {
  if (!value) return globalI18n.global.t('comm.table.empty-2')
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : globalI18n.global.t('comm.table.empty-2')
}

const loadNoticeProviders = async () => {
  const resp = await getAllNotice()
  if (resp.status !== 200) return

  const dataMap = new Map<string, NoticeProviderItem>()
  ;(resp.result || []).forEach((item: any) => {
    const key = item.type?.id
    if (!key) return

    if (!dataMap.has(key)) {
      dataMap.set(key, {
        name: item.name,
        provider: key,
        children: [{ ...omit(item, ['type']) }],
      })
    } else {
      dataMap.get(key)?.children.push({ ...omit(item, ['type']) })
    }
  })
  noticeProviders.value = [...dataMap.values()]
    .map(item => ({
      ...item,
      children: item.children.filter(child => !props.excludeProviders.includes(child.provider)),
    }))
    .filter(item => item.children.length > 0)
  refresh()
}

const refresh = () => {
  tableRef.value?.reload?.()
}

const requestNotifications = async (params: Record<string, any>) => {
  const response = await getList_api(params)
  const result = response?.result
  const records = Array.isArray(result) ? result : result?.data
  if (!Array.isArray(records)) return response
  const resolved = await resolveNoticeTexts(records)
  return {
    ...response,
    result: Array.isArray(result) ? resolved : { ...result, data: resolved },
  }
}

watch(globalI18n.global.locale, refresh)

const handleFilterUpdate = (terms: ConditionFilterTerm[]) => {
  filterTerms.value = terms
}

const handleFilterSearch = (payload: ConditionFilterChangePayload) => {
  submittedTerms.value = payload.terms || []
}

const changeStatus = async (row: any) => {
  const type = row.state?.value === 'read' ? '_unread' : '_read'
  const resp = await changeStatus_api(type, [row.id])
  if (resp.status === 200) {
    onlyMessage(globalI18n.global.t('Service.index.success'))
    refresh()
    user.updateAlarm()
  }
}

const readPage = async () => {
  const ids = tableRef.value?.dataSource?.map((item: any) => item.id) || []
  if (!ids.length) return

  const resp = await changeStatus_api('_read', ids)
  if (resp.status === 200) {
    onlyMessage(globalI18n.global.t('Service.index.success'))
    refresh()
    user.updateAlarm()
  }
}

const readAll = async () => {
  const resp = await changeAllStatus('_read', providerIds.value)
  if (resp.status === 200) {
    onlyMessage(globalI18n.global.t('Service.index.success'))
    refresh()
    user.updateAlarm()
  }
}

const view = (row: any) => {
  viewItem.value = row
  viewVisible.value = true
}

watch(() => userRuntime.messageInfo?.id, (value) => {
  if (value) view(userRuntime.messageInfo)
}, { immediate: true })

onMounted(() => {
  loadNoticeProviders()
  if (routerParams.params?.value.row) {
    view(routerParams.params.value.row)
  }
})

onUnmounted(() => {
  userRuntime.messageInfo = {}
  viewVisible.value = false
  routerParams?.clear?.()
})
</script>

<template>
  <div class="station-message">
    <div class="station-message__toolbar">
      <ConditionFilter
        class="station-message__filter"
        :fields="searchColumns"
        :common-fields="['topicProvider', 'message', 'state']"
        :model-value="filterTerms"
        :placeholder="$t('SaasManager.generated.8f71a5b1fe90')"
        @update:model-value="handleFilterUpdate"
        @change="handleFilterSearch"
      />
      <a-dropdown trigger="hover">
        <a-button type="link"> {{ $t('SaasManager.generated.eae7c2c8a99d') }} <AIcon type="DownOutlined" />
        </a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item>
              <j-permission-button
                type="text"
                :popConfirm="{
                  title: $t('PersonCenter.message.confirmPageRead'),
                  onConfirm: readPage,
                }"
              > {{ $t('SaasManager.generated.1de859e2f82c') }} </j-permission-button>
            </a-menu-item>
            <a-menu-item>
              <j-permission-button
                type="text"
                :popConfirm="{
                  title: $t('PersonCenter.message.confirmAllRead'),
                  onConfirm: readAll,
                }"
              > {{ $t('MessageContent.markAllRead') }} </j-permission-button>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>

    <j-pro-table
      ref="tableRef"
      :columns="messageTableColumns"
      :request="requestNotifications"
      mode="TABLE"
      style='flex: 1; min-height: 0'
      :params="queryParams"
      :default-params="defaultParams"
      :body-style="{ padding: 0 }"
    >
      <template #topicProvider="slotProps">
        {{ slotProps.topicName || slotProps.topicProvider }}
      </template>
      <template #notifyTime="slotProps">
        {{ formatDateTime(slotProps.notifyTime) }}
      </template>
      <template #state="slotProps">
        <j-badge-status
          :status="slotProps.state?.value"
          :text="slotProps.state?.text"
          :status-names="{ read: 'success', unread: 'error' }"
        />
      </template>
      <template #action="slotProps">
        <a-space :size="16">
          <j-permission-button
            type="link"
            :popConfirm="{
              title: slotProps.state?.value === 'read'
                ? $t('PersonCenter.message.confirmMarkUnread')
                : $t('PersonCenter.message.confirmMarkRead'),
              onConfirm: () => changeStatus(slotProps),
            }"
            :tooltip="{
              title: slotProps.state?.value === 'read'
                ? $t('PersonCenter.message.markUnread')
                : $t('PersonCenter.message.markRead'),
            }"
          >
            <AIcon type="icon-a-PIZHU1" />
          </j-permission-button>
          <j-permission-button
            type="link"
            :tooltip="{ title: $t('PersonCenter.message.view') }"
            @click="view(slotProps)"
          >
            <AIcon type="SearchOutlined" />
          </j-permission-button>
        </a-space>
      </template>
    </j-pro-table>

    <ViewDialog
      v-if="viewVisible"
      v-model:visible="viewVisible"
      :data="viewItem"
    />
  </div>
</template>

<style scoped lang="less">
.station-message {
  display: flex;
  height: 100%;
  flex-direction: column;
  gap: var(--space-4);
}

.station-message__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.station-message__filter {
  flex: 1;
  min-width: 0;
}
</style>
