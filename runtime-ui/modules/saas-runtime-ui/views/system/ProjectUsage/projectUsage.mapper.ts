import type { ProjectResourceUsage, ProjectUsageMetric } from './types'

const asRecord = (value: unknown): Record<string, unknown> => (
  value && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {}
)

const asArray = (value: unknown): unknown[] => Array.isArray(value) ? value : []

const asText = (value: unknown): string => {
  if (typeof value === 'string') return value.trim()
  if (typeof value === 'number') return String(value)
  if (!value || typeof value !== 'object' || Array.isArray(value)) return ''
  const record = asRecord(value)
  return asText(record.text) || asText(record.name) || asText(record.value)
}

const asNumber = (value: unknown): number => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

const getLimitValue = (limit: unknown): number | null => {
  const source = asRecord(limit)
  const rawValue = source.limit ?? source.value
  if (rawValue === undefined || rawValue === null || rawValue === '') return null
  const value = Number(rawValue)
  return Number.isFinite(value) ? value : null
}

/** 无有效上限不计算比例，超额时保留原始用量但进度最多显示 100%。 */
export const calculateUsagePercent = (usage: unknown, limit: unknown): number => {
  const current = Math.max(asNumber(usage), 0)
  const maximum = getLimitValue(limit)
  if (maximum === null || maximum <= 0) return 0
  return Math.min(Math.round((current / maximum) * 1000) / 10, 100)
}

const mapMetric = (
  resourceId: string,
  metric: unknown,
  metricIndex: number,
): ProjectUsageMetric => {
  const source = asRecord(metric)
  const metricDefinition = asRecord(source.metric)
  const limitInfo = asRecord(source.limit)
  const usage = Math.max(asNumber(source.usage), 0)
  const limit = getLimitValue(limitInfo)
  const metricId = asText(metricDefinition.id) || asText(source.metricId) || String(metricIndex)

  return {
    id: `${resourceId}:${metricId}`,
    name: asText(metricDefinition.name) || metricId,
    code: metricId,
    description: asText(metricDefinition.description) || asText(source.description),
    usage,
    limit,
    percent: calculateUsagePercent(usage, limitInfo),
    unlimited: limit === -1,
  }
}

/** 将当前租户资源详情转换为页面卡片模型，能力-only 资源保留为空指标卡片。 */
export const mapProjectResourceUsage = (resources: unknown): ProjectResourceUsage[] => (
  asArray(resources).map((item, resourceIndex) => {
    const source = asRecord(item)
    const id = asText(source.id) || String(resourceIndex)
    const metrics = asArray(source.metrics).map((metric, metricIndex) => (
      mapMetric(id, metric, metricIndex)
    ))
    const limitedPercents = metrics
      .filter(metric => !metric.unlimited && metric.limit !== null && metric.limit > 0)
      .map(metric => metric.percent)

    return {
      id,
      name: asText(source.name) || id,
      description: asText(source.description),
      metrics,
      maxUsagePercent: limitedPercents.length ? Math.max(...limitedPercents) : null,
    }
  })
)
