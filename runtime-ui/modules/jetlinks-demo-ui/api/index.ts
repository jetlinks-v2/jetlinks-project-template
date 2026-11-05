import { Request } from '@jetlinks-web/core'
import type { DemoRecord, DemoRecordDraft, DemoRecordQuery } from '../types'

const records = new Request('/demo/record')

export const queryRecords = (params: DemoRecordQuery, options?: { signal?: AbortSignal }) =>
  records.page<DemoRecord>(params, options)

export const createRecord = (data: DemoRecordDraft) => records.save<DemoRecord>(data)

export const updateRecord = (id: string, data: DemoRecordDraft) =>
  records.put<boolean>(`/${encodeURIComponent(id)}`, data)

export const deleteRecord = (id: string) => records.delete<DemoRecord>(encodeURIComponent(id))
