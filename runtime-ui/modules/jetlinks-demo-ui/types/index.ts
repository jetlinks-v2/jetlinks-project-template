import type { ConditionFilterTerm } from '@jetlinks-web-core/components/ConditionFilter'

export interface DemoRecord {
  id: string
  name: string
  description?: string
  createTime: number
}

export interface DemoRecordDraft {
  name: string
  description: string
}

export interface DemoRecordQuery {
  pageIndex?: number
  pageSize?: number
  terms?: ConditionFilterTerm[]
  sorts?: Array<{ name: string; order: 'asc' | 'desc' }>
}

export type EditableRecordField = keyof DemoRecordDraft
