import type { ConditionFilterTerm } from '@jetlinks-web-core/components/ConditionFilter'

export interface DemoRecord {
  id: string
  name: string
  description?: string
  inspectionTeamId: string
  inspectionItems: DemoInspectionItem[]
  createTime: number
}

export interface DemoRecordDraft {
  name: string
  description: string
  inspectionTeamId: string
  inspectionItems: DemoInspectionItem[]
}

export interface DemoInspectionItem {
  name: string
  requirement: string
}

export type InspectionTeamState = 'enabled' | 'disabled'

export interface InspectionTeamDraft {
  name: string
  description: string
  parentId?: string
  state: InspectionTeamState
}

export interface InspectionTeam extends Omit<InspectionTeamDraft, 'state'> {
  id: string
  state: InspectionTeamState | { value: InspectionTeamState; text: string }
}

export interface InspectionTeamMember {
  id: string
  name: string
  username: string
}

export interface InspectionTeamMemberQuery {
  pageIndex?: number
  pageSize?: number
  terms?: ConditionFilterTerm[]
  sorts?: Array<{ name: string; order: 'asc' | 'desc' }>
}

export interface DemoRecordQuery {
  pageIndex?: number
  pageSize?: number
  terms?: ConditionFilterTerm[]
  sorts?: Array<{ name: string; order: 'asc' | 'desc' }>
}

export type EditableRecordField = 'name' | 'description'
