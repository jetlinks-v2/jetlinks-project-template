<script setup lang="ts">
import { moduleRegistry } from '@jetlinks-web-core/utils/module-registry'

const visualizationManagerApis: any = moduleRegistry.getResource('visualization-manager-ui', 'apis')
const { DesignerPreview }: any = moduleRegistry.getResource('visualization-manager-ui', 'components')

const draftId = ref('')
const previewRef = ref<any>(null)
const refreshing = ref(false)

const componentScope = {
  includeDefaults: false,
  modules: ['device-manager-ui']
} as const

const getProjectInfo = async () => {
  const res = await visualizationManagerApis.ensureDashboardProject({
    entityId: 'device_default',
    groupId: 'device_default',
    projectName: 'device 默认看板',
    groupName: 'device',
    configuration: {}
  })
  draftId.value = res.draftId
}

const mapSpaceTree = (nodes: any[]): any[] => {
  return nodes.map((node) => ({
    title: node.name,
    value: node.id,
    key: node.id,
    children: node.children?.length ? mapSpaceTree(node.children) : undefined
  }))
}

const handleRefresh = async () => {
  if (refreshing.value) return
  refreshing.value = true
  try {
    await previewRef.value?.reload?.()
  } finally {
    setTimeout(() => {
      refreshing.value = false
    }, 600)
  }
}

const handleSpaceChange = (_value: string | undefined) => {
  // 空间切换后的逻辑，后续可扩展
}

getProjectInfo()
</script>

<template>
  <j-page-container>
    <FullPage>
      <DesignerPreview
          ref="previewRef"
          :draftId="draftId"
          :showHeader="false"
          :embedded="true"
          :componentScope="componentScope"
          pageKey="device_default"
      />
    </FullPage>
  </j-page-container>
</template>

<style scoped lang="less">
.home-warp {
  display: flex;
  gap: 24px;
  height: 100%;

  .home-left {
    flex: 1;
    min-width: 400px;
    position: relative;
  }

  .home-right {
    width: 400px;
  }
}
</style>