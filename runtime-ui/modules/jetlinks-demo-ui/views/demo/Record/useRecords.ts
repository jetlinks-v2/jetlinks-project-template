import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@jetlinks-web/hooks'
import { onlyMessage } from '@jetlinks-web/utils'
import type { ConditionFilterChangePayload, ConditionFilterField } from '@jetlinks-web-core/components/ConditionFilter'
import { createRecord, deleteRecord, queryRecords, updateRecord } from '../../../api'
import type { DemoRecord, DemoRecordDraft, DemoRecordQuery, EditableRecordField } from '../../../types'

export const useRecords = () => {
  const { t } = useI18n()
  const { hasPerm: canUpdate } = usePermission('demo/Record:update')
  const { hasPerm: canCreate } = usePermission('demo/Record:add')
  const tableRef = ref<{ reload: () => void }>()
  const params = ref<ConditionFilterChangePayload['filter']>({ terms: [] })
  const createOpen = ref(false)
  const createDraft: DemoRecordDraft = { name: '', description: '' }
  const loadFailed = ref(false)
  const savingIds = ref<string[]>([])
  const deletingIds = ref<string[]>([])
  const drafts = reactive<Record<string, Partial<DemoRecordDraft>>>({})
  const fieldErrors = reactive<Record<string, Partial<Record<EditableRecordField, boolean>>>>({})
  const editableFields: EditableRecordField[] = ['name', 'description']

  const columns = computed(() => [
    { title: t('demo.record.name'), dataIndex: 'name', key: 'name', scopedSlots: true, width: 260 },
    { title: t('demo.record.description'), dataIndex: 'description', key: 'description', scopedSlots: true },
    { title: t('demo.record.createTime'), dataIndex: 'createTime', key: 'createTime', scopedSlots: true, width: 190 },
    { title: t('demo.record.actions'), dataIndex: 'action', key: 'action', scopedSlots: true, width: 100 },
  ])
  const filterFields = computed<ConditionFilterField[]>(() => [{
    title: t('demo.record.name'),
    dataIndex: 'name',
    search: { type: 'string', defaultTermType: 'like' },
  }])
  const createSchema = computed(() => [
    {
      name: 'name',
      label: t('demo.record.name'),
      itemType: 'input',
      componentProps: { maxlength: 64, placeholder: t('demo.record.namePlaceholder') },
      rules: [{ required: true, whitespace: true, message: t('demo.record.nameRequired') }],
    },
    {
      name: 'description',
      label: t('demo.record.description'),
      itemType: 'textarea',
      componentProps: { rows: 3, maxlength: 512, placeholder: t('demo.record.descriptionPlaceholder') },
    },
  ])

  const reload = () => tableRef.value?.reload()

  const loadRecords = async (query: DemoRecordQuery, options?: { signal?: AbortSignal }) => {
    const response = await queryRecords(query, options)
    if (!options?.signal?.aborted) loadFailed.value = !response.success
    return response
  }

  const search = ({ filter }: ConditionFilterChangePayload) => {
    params.value = filter
  }

  const create = async (value: DemoRecordDraft) => {
    const response = await createRecord({ name: value.name, description: value.description })
    if (response.success) onlyMessage(t('demo.record.saveSuccess', { name: value.name }))
    return response
  }

  const created = () => {
    createOpen.value = false
    reload()
  }

  /** 临时输入与服务端记录分离；失败保留输入并标明未保存，同一行保存期间暂停其他编辑。 */
  const saveField = async (record: DemoRecord, field: EditableRecordField, value: string) => {
    drafts[record.id] = { ...drafts[record.id], [field]: value }
    fieldErrors[record.id] = { ...fieldErrors[record.id], [field]: false }
    if (value === (record[field] ?? '')) {
      delete drafts[record.id][field]
      return
    }

    savingIds.value.push(record.id)
    try {
      const response = await updateRecord(record.id, {
        name: record.name,
        description: record.description ?? '',
        [field]: value,
      })
      if (response.success && response.result) {
        record[field] = value
        delete drafts[record.id][field]
        onlyMessage(t('demo.record.saveSuccess', { name: record.name }))
        reload()
      } else {
        fieldErrors[record.id][field] = true
      }
    } catch {
      // 请求层提供后端错误提示，此处保留当前单元格的未保存状态。
      fieldErrors[record.id][field] = true
    } finally {
      savingIds.value = savingIds.value.filter(id => id !== record.id)
    }
  }

  const remove = async (record: DemoRecord) => {
    deletingIds.value.push(record.id)
    try {
      const response = await deleteRecord(record.id)
      if (response.success) {
        delete drafts[record.id]
        delete fieldErrors[record.id]
        onlyMessage(t('demo.record.deleteSuccess', { name: record.name }))
        reload()
      }
    } finally {
      deletingIds.value = deletingIds.value.filter(id => id !== record.id)
    }
  }

  return {
    tableRef, params, columns, filterFields, createSchema, createDraft, createOpen, loadFailed,
    canUpdate, canCreate, savingIds, deletingIds, drafts, fieldErrors, editableFields,
    loadRecords, search, create, created, saveField, remove,
  }
}
