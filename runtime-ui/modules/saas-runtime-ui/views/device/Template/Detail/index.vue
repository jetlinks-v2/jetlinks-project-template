<template>
  <j-page-container :tabList="list" :tabActiveKey="templateStore.tabActiveKey" :showBack="true" @tabChange="onTabChange">
    <template #title>
      <div style="display: flex; align-items: center">
        <a-tooltip>
          <template #title>{{ templateStore.current.name }}</template>
          <div class="templateDetailHead">{{ templateStore.current.name }}</div>
        </a-tooltip>
        <div style="margin: -5px 0 0 20px" v-if="permissionStore.hasPermission('device/Template:action')">
          <j-permission-button
            style="padding: 0"
            type="text"
            hasPermission="device/Template:action"
            :popConfirm="{
              title:
                templateStore.current.state?.value === 'enabled'
                  ? ($t('template.list.disableConfirm') || '确认禁用？')
                  : ($t('template.list.enableConfirm') || '确认启用？'),
              onConfirm: templateStore.current.state?.value === 'enabled' ? handleUndeploy : handleDeploy,
            }"
          >
            <a-switch
              :checked="templateStore.current.state?.value === 'enabled'"
              :checked-children="$t('template.list.state.enabled') || '正常'"
              :un-checked-children="$t('template.list.state.disabled') || '禁用'"
              :disabled="!permissionStore.hasPermission('device/Template:action')"
            />
          </j-permission-button>
        </div>
      </div>
    </template>
    <template #extra>
      <j-permission-button
        type="primary"
        hasPermission="device/Template:update"
        placement="topRight"
        :disabled="templateStore.current?.state?.value === 'disabled'"
        :tooltip="templateStore.current?.state?.value === 'enabled' ? { title: $t('template.detail.enableFirst') || '请先启用模板' } : undefined"
        :popConfirm="{ title: $t('template.detail.applyConfigConfirm') || '确认应用配置？', onConfirm: handleDeploy }"
      >
        {{ $t('template.detail.applyConfig') || '应用配置' }}
      </j-permission-button>
    </template>

    <FullPage :fixed="false">
      <div style="height: 100%; padding: 24px">
        <component :is="tabs[templateStore.tabActiveKey]" :class="templateStore.tabActiveKey === 'Metadata' ? 'metedata' : ''" v-bind="{ type: 'template' }" />
      </div>
    </FullPage>
  </j-page-container>
</template>

<script setup lang="ts" name="DeviceTemplateDetail">
import { useI18n } from 'vue-i18n'
import { useRouterParams } from '@jetlinks-web/hooks'
import { EventEmitter, onlyMessage } from '@jetlinks-web/utils'
import { useAuthStore } from '@jetlinks-web-core/store'
import { useTemplateStore } from '../../../../store/template'
import { deployTemplate, undeployTemplate } from '../../../../api/template'
import { tabs } from './asyncComponent'

const { t: $t } = useI18n()
const permissionStore = useAuthStore()
const route = useRoute()
const templateStore = useTemplateStore()
const routerParams = useRouterParams()

const list = ref([
  { key: 'Info', tab: $t('template.detail.basic') || '基本信息' },
  { key: 'Metadata', tab: $t('template.detail.metadata') || '物模型', class: 'objectModel' },
  { key: 'Device', tab: $t('template.detail.access') || '设备接入' },
  { key: 'Document', tab: $t('template.detail.document') || '接入说明' },
])

const onTabChange = (key: string) => {
  if (templateStore.tabActiveKey === 'Metadata') {
    EventEmitter.emit('MetadataTabs', () => {
      templateStore.tabActiveKey = key
    })
  } else {
    templateStore.tabActiveKey = key
  }
}

const handleDeploy = () => {
  if (!templateStore.current.id) return
  const req = deployTemplate(templateStore.current.id)
  req.then((res) => {
    if (res.status === 200) {
      onlyMessage($t('template.message.success') || '操作成功')
      templateStore.refresh(templateStore.current.id)
    }
  })
  return req
}

const handleUndeploy = () => {
  if (!templateStore.current.id) return
  const req = undeployTemplate(templateStore.current.id)
  req.then((res) => {
    if (res.status === 200) {
      onlyMessage($t('template.message.success') || '操作成功')
      templateStore.refresh(templateStore.current.id)
    }
  })
  return req
}

onMounted(() => {
  templateStore.reSet()
  templateStore.refresh(route.params.id as string)
  templateStore.tabActiveKey = routerParams.params?.value.tab || 'Info'
})
</script>

<style scoped lang="less">
.ant-switch-loading,
.ant-switch-disabled {
  cursor: not-allowed;
}
.templateDetailHead {
  max-width: 300px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
