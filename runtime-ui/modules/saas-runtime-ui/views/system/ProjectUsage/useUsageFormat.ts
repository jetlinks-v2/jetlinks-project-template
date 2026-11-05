import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProjectUsageMetric } from './types'

export const useUsageFormat = () => {
  const { t, locale } = useI18n()
  const formatter = computed(() => new Intl.NumberFormat(locale.value.startsWith('en') ? 'en-US' : 'zh-CN'))
  const formatNumber = (value: number) => formatter.value.format(value)
  const formatPercent = (value: number | null) => (
    value === null ? '--' : `${Number.isInteger(value) ? value : value.toFixed(1)}%`
  )
  const usageText = (metric: ProjectUsageMetric) => {
    if (metric.unlimited) return `${formatNumber(metric.usage)} / ${t('ProjectUsage.unlimited')}`
    if (metric.limit === null) return formatNumber(metric.usage)
    return `${formatNumber(metric.usage)} / ${formatNumber(metric.limit)}`
  }
  const progressColor = (percent: number) => {
    if (percent >= 90) return 'var(--jet-theme-error)'
    if (percent >= 60) return 'var(--jet-theme-warning)'
    return 'var(--jet-theme-primary)'
  }
  return { formatPercent, usageText, progressColor }
}
