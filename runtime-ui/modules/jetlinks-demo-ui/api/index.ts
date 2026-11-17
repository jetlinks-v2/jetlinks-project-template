import { Request } from '@jetlinks-web/core'
import type {
  DemoRecord, DemoRecordDraft, DemoRecordQuery, InspectionTeam, InspectionTeamDraft,
  InspectionTeamMember, InspectionTeamMemberQuery,
} from '../types'

const records = new Request('/demo/record')
const inspectionTeams = new Request('/demo/inspection-team')

export const queryRecords = (params: DemoRecordQuery, options?: { signal?: AbortSignal }) =>
  records.page<DemoRecord>(params, options)

export const createRecord = (data: DemoRecordDraft) => records.save<DemoRecord>(data)

export const updateRecord = (id: string, data: Partial<DemoRecordDraft>) =>
  records.put<boolean>(`/${encodeURIComponent(id)}`, data)

export const deleteRecord = (id: string) => records.delete<DemoRecord>(encodeURIComponent(id))

/** 批量取得班组目录，供计划回显、筛选与班组维护共用。 */
export const queryInspectionTeams = () => inspectionTeams.noPage<InspectionTeam>({ sorts: [{ name: 'name', order: 'asc' }] })

export const createInspectionTeam = (data: InspectionTeamDraft) => inspectionTeams.save<InspectionTeam>(data)

export const updateInspectionTeam = (id: string, data: Partial<InspectionTeamDraft>) =>
  inspectionTeams.put<boolean>(`/${encodeURIComponent(id)}`, data)

export const deleteInspectionTeam = (id: string) => inspectionTeams.delete<InspectionTeam>(encodeURIComponent(id))

/** 应用身份沿现有 Scope 传递；demo 后端通过平台命令查询成员并增量维护班组关系。 */
export const queryInspectionTeamMembers = (teamId: string, params: InspectionTeamMemberQuery, options?: { signal?: AbortSignal }) =>
  inspectionTeams.page<InspectionTeamMember>(params, { ...options, url: `/${encodeURIComponent(teamId)}/members/_query` })

export const queryInspectionTeamMemberCandidates = (teamId: string, params: InspectionTeamMemberQuery, options?: { signal?: AbortSignal }) =>
  inspectionTeams.page<InspectionTeamMember>(params, { ...options, url: `/${encodeURIComponent(teamId)}/members/_candidates/_query` })

export const bindInspectionTeamMembers = (teamId: string, userIds: string[]) =>
  inspectionTeams.post<void>(`/${encodeURIComponent(teamId)}/members/_bind`, userIds)

export const unbindInspectionTeamMembers = (teamId: string, userIds: string[]) =>
  inspectionTeams.post<number>(`/${encodeURIComponent(teamId)}/members/_unbind`, userIds)
