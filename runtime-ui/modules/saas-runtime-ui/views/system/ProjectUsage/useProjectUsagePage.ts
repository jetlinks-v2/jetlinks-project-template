import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { isAxiosError } from 'axios'
import { getProjectUsageUnavailableReason } from '../../../utils/projectUsage'
import { fetchProjectResourceUsage } from './projectUsage.service'
import type { ProjectResourceUsage, ViewState } from './types'

/** 维护当前项目用量的加载状态；重试、项目切换和卸载后均不能应用过期结果。 */
export const useProjectUsagePage = () => {
  const { t } = useI18n()
  const route = useRoute()
  const resources = ref<ProjectResourceUsage[]>([])
  const state = ref<ViewState>({ status: 'loading', message: '' })
  let requestId = 0

  const loadResources = async () => {
    const currentRequest = ++requestId
    resources.value = []
    state.value = { status: 'loading', message: '' }

    try {
      // 缺少上下文属于正常缺省态；先更新请求序号，防止旧项目结果覆盖此状态。
      const unavailableReason = getProjectUsageUnavailableReason()
      if (unavailableReason) {
        state.value = { status: 'unavailable', message: t(`ProjectUsage.state.${unavailableReason}`) }
        return
      }
      const result = await fetchProjectResourceUsage()
      if (currentRequest !== requestId) return
      resources.value = result
      state.value = { status: result.length ? 'success' : 'empty', message: '' }
    } catch (error) {
      if (currentRequest !== requestId) return
      const forbidden = isAxiosError(error) && error.response?.status === 403
      state.value = {
        status: forbidden ? 'forbidden' : 'error',
        message: forbidden
          ? t('ProjectUsage.state.forbidden')
          : error instanceof Error && error.message ? error.message : t('ProjectUsage.state.loadFailed'),
      }
    }
  }

  watch(() => route.fullPath, () => { void loadResources() }, { immediate: true })
  onBeforeUnmount(() => { requestId++ })

  return { resources, state, loadResources }
}
