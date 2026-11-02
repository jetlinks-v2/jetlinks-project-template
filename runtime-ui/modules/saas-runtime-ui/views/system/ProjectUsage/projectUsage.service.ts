import i18n from '@jetlinks-web-core/locales'
import { getCurrentProjectResourceUsage_api } from '../../../api/projectUsage'
import { mapProjectResourceUsage } from './projectUsage.mapper'
import type { ProjectResourceUsage } from './types'

const unwrapList = (response: unknown): unknown[] => {
  if (Array.isArray(response)) return response

  const source = response && typeof response === 'object'
    ? response as Record<string, unknown>
    : {}
  if (source.success === false) {
    throw new Error(String(source.message || i18n.global.t('ProjectUsage.state.loadFailed')))
  }
  if (Array.isArray(source.result)) return source.result
  throw new Error(i18n.global.t('ProjectUsage.state.loadFailed'))
}

/** 查询当前项目运行时的资源额度与用量，不依赖 SaaS 控制台项目接口。 */
export const fetchProjectResourceUsage = async (): Promise<ProjectResourceUsage[]> => {
  const resources = unwrapList(await getCurrentProjectResourceUsage_api())
  return mapProjectResourceUsage(resources)
}
