import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { onlyMessage } from '@jetlinks-web/utils'
import type { ConditionFilterChangePayload, ConditionFilterField } from '@jetlinks-web-core/components/ConditionFilter'
import {
  bindInspectionTeamMembers, queryInspectionTeamMemberCandidates,
  queryInspectionTeamMembers, unbindInspectionTeamMembers,
} from '../../../api'
import type { InspectionTeamMember, InspectionTeamMemberQuery } from '../../../types'

/** 按当前班组增删直接成员；两个页签共用搜索及分页，不改用户的应用成员身份。 */
export const useTeamMembers = (teamId: string) => {
  const { t } = useI18n()
  const activeTab = ref<'joined' | 'available'>('joined')
  const tableRef = ref<{ reload: () => void }>()
  const params = ref<ConditionFilterChangePayload['filter']>({ terms: [] })
  const selectedRowKeys = ref<string[]>([])
  const loading = ref(false)
  const loadFailed = ref(false)
  const joining = ref(false)
  const removingUserId = ref<string>()
  const mutating = computed(() => joining.value || !!removingUserId.value)

  const filterFields = computed<ConditionFilterField[]>(() => [
    { title: t('demo.team.memberName'), dataIndex: 'name', search: { type: 'string', defaultTermType: 'like' } },
    { title: t('demo.team.memberUsername'), dataIndex: 'username', search: { type: 'string', defaultTermType: 'like' } },
  ])
  const columns = computed(() => [
    { title: t('demo.team.memberName'), dataIndex: 'name', key: 'name', ellipsis: true },
    { title: t('demo.team.memberUsername'), dataIndex: 'username', key: 'username', ellipsis: true },
    ...(activeTab.value === 'joined'
      ? [{ title: t('demo.record.actions'), dataIndex: 'action', key: 'action', width: 140, scopedSlots: true }]
      : []),
  ])

  const toggleSelection = (ids: string[], selected: boolean) => {
    const next = new Set(selectedRowKeys.value)
    ids.forEach(id => selected ? next.add(id) : next.delete(id))
    selectedRowKeys.value = [...next]
  }
  const rowSelection = computed(() => activeTab.value === 'available' ? {
    type: 'checkbox' as const,
    selectedRowKeys: selectedRowKeys.value,
    getCheckboxProps: () => ({ disabled: mutating.value }),
    onSelect: (member: InspectionTeamMember, selected: boolean) => toggleSelection([member.id], selected),
    onSelectAll: (selected: boolean, _members: InspectionTeamMember[], changedMembers: InspectionTeamMember[]) => {
      toggleSelection(changedMembers.map(member => member.id), selected)
    },
    onSelectNone: () => { selectedRowKeys.value = [] },
  } : undefined)

  const loadMembers = async (query: InspectionTeamMemberQuery, options?: { signal?: AbortSignal }) => {
    loading.value = true
    try {
      const request = activeTab.value === 'joined' ? queryInspectionTeamMembers : queryInspectionTeamMemberCandidates
      const response = await request(teamId, query, options)
      if (!options?.signal?.aborted) loadFailed.value = !response.success
      return response
    } catch (error) {
      if (!options?.signal?.aborted) loadFailed.value = true
      throw error
    } finally {
      if (!options?.signal?.aborted) loading.value = false
    }
  }
  const search = ({ filter }: ConditionFilterChangePayload) => {
    params.value = filter
    selectedRowKeys.value = []
  }
  watch(activeTab, () => {
    params.value = { terms: [] }
    selectedRowKeys.value = []
    loadFailed.value = false
  })

  const addedOrRemoved = (message: string) => {
    onlyMessage(message)
    selectedRowKeys.value = []
    tableRef.value?.reload()
  }
  const addMembers = async () => {
    if (!selectedRowKeys.value.length || mutating.value) return
    joining.value = true
    try {
      const response = await bindInspectionTeamMembers(teamId, [...selectedRowKeys.value])
      if (response.success) addedOrRemoved(t('demo.team.membersAdded'))
    } catch {
      // 请求层显示错误；保留选择，便于用户检查后重新提交。
    } finally {
      joining.value = false
    }
  }
  const removeMember = async (member: InspectionTeamMember) => {
    if (mutating.value) return
    removingUserId.value = member.id
    try {
      const response = await unbindInspectionTeamMembers(teamId, [member.id])
      if (response.success) addedOrRemoved(t('demo.team.memberRemoved'))
    } catch {
      // 移出失败不改本地成员列表，沿用请求层的错误反馈。
    } finally {
      removingUserId.value = undefined
    }
  }

  return {
    activeTab, tableRef, params, selectedRowKeys, loading, loadFailed, joining, removingUserId, mutating,
    filterFields, columns, rowSelection, loadMembers, search, addMembers, removeMember,
  }
}
