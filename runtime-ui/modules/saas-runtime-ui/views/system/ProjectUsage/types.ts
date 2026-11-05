export interface ProjectUsageMetric {
  id: string
  name: string
  code: string
  description: string
  usage: number
  limit: number | null
  percent: number
  unlimited: boolean
}

export interface ProjectResourceUsage {
  id: string
  name: string
  description: string
  metrics: ProjectUsageMetric[]
  maxUsagePercent: number | null
}

export interface ViewState {
  status: 'loading' | 'success' | 'empty' | 'error' | 'unavailable' | 'forbidden'
  message: string
}
