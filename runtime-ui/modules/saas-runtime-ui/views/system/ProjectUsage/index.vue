<template>
  <j-page-container>
    <FullPage transparentBackground showScroll>
      <ResourceUsageSection
        :resources="resources"
        :state="state"
        @retry="loadResources"
      />
    </FullPage>
  </j-page-container>
</template>

<script setup lang="ts" name="SystemProjectUsage">
import ResourceUsageSection from './components/ResourceUsageSection.vue'
import { useProjectUsagePage } from './useProjectUsagePage'

const { resources, state, loadResources } = useProjectUsagePage()
</script>
