<template>
  <j-page-container>
    <FullPage flex transparent-background>
      <a-alert v-if="loadFailed" type="error" show-icon :message="$t('demo.team.loadFailed')" />
      <div class="inspection-teams__toolbar">
        <j-permission-button type="primary" has-permission="demo/InspectionTeam:add" @click="createOpen = true">
          <AIcon type="PlusOutlined" />
          {{ $t('demo.team.add') }}
        </j-permission-button>
      </div>
      <j-pro-table
        class="inspection-teams__table"
        mode="TABLE"
        :columns="columns"
        :data-source="teams"
        row-key="id"
        :loading="loading"
        no-pagination
        :scroll="{ x: 850 }"
      >
        <template v-for="field in editableFields" :key="field" #[field]="record">
          <InputEditable
            v-if="canUpdate && !busyIds.includes(record.id) && !loading"
            :value="drafts[record.id]?.[field] ?? record[field] ?? ''"
            :max-length="field === 'name' ? 64 : 512"
            @change="(value: string) => saveField(record, field, value)"
          />
          <j-ellipsis v-else>{{ (drafts[record.id]?.[field] ?? record[field]) || $t('comm.table.empty') }}</j-ellipsis>
        </template>
        <template #parentId="record">
          <a-tree-select
            v-if="canUpdate"
            :value="(drafts[record.id]?.parentId ?? record.parentId) || undefined"
            :tree-data="parentOptions(record.id)"
            tree-data-simple-mode
            allow-clear
            show-search
            tree-node-filter-prop="title"
            :placeholder="$t('demo.team.noParent')"
            :disabled="busyIds.includes(record.id) || loading"
            class="inspection-teams__select"
            @change="(value?: string) => saveField(record, 'parentId', value)"
          />
          <j-ellipsis v-else>{{ teamNames.get(record.parentId) || $t('demo.team.noParent') }}</j-ellipsis>
        </template>
        <template #state="record">
          <a-select
            v-if="canUpdate"
            :value="drafts[record.id]?.state ?? stateValue(record)"
            :options="stateOptions(record)"
            :disabled="busyIds.includes(record.id) || loading"
            class="inspection-teams__select"
            @change="(value: string) => saveField(record, 'state', value)"
          />
          <span v-else>{{ stateText(record) }}</span>
        </template>
        <template #action="record">
          <j-permission-button
            type="link"
            has-permission="demo/InspectionTeam:members"
            :disabled="loading || busyIds.includes(record.id)"
            @click="openMembers(record)"
          >
            {{ $t('demo.team.members') }}
          </j-permission-button>
          <j-permission-button
            type="link"
            danger
            has-permission="demo/InspectionTeam:delete"
            :loading="busyIds.includes(record.id)"
            :disabled="loading"
            :pop-confirm="{ title: $t('demo.team.deleteConfirm', { name: record.name }), onConfirm: () => remove(record) }"
          >
            {{ $t('demo.record.delete') }}
          </j-permission-button>
          <span v-if="Object.values(errors[record.id] || {}).some(Boolean)" class="inspection-teams__error" role="alert">{{ $t('demo.record.saveFailed') }}</span>
        </template>
      </j-pro-table>
    </FullPage>
    <EditDialog
      v-if="createOpen"
      :title="$t('demo.team.add')"
      :schema="createSchema"
      :data="createDraft"
      :request="create"
      @close="createOpen = false"
      @save="created"
    />
    <MembersDialog v-if="memberTeam" :key="memberTeam.id" :team="memberTeam" @close="memberTeam = undefined" />
  </j-page-container>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@jetlinks-web/hooks'
import { onlyMessage } from '@jetlinks-web/utils'
import { FullPage } from '@jetlinks-web-core/layout'
import { getApplicationRuntimeEntry } from '@jetlinks-web-core/utils'
import InputEditable from '@jetlinks-web-core/components/Editable/InputEditable.vue'
import EditDialog from '@jetlinks-web-core/components/EditDialog/index.vue'
import { createInspectionTeam, deleteInspectionTeam, queryInspectionTeams, updateInspectionTeam } from '../../../api'
import type { InspectionTeam, InspectionTeamDraft } from '../../../types'

const MembersDialog = defineAsyncComponent(() => import('./MembersDialog.vue'))
const { t } = useI18n()
const { hasPerm: canUpdate } = usePermission('demo/InspectionTeam:update')
const teams = ref<InspectionTeam[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const createOpen = ref(false)
const memberTeam = ref<InspectionTeam>()
const createDraft: InspectionTeamDraft = { name: '', description: '', parentId: '', state: 'enabled' }
const busyIds = ref<string[]>([])
const drafts = reactive<Record<string, Partial<InspectionTeamDraft>>>({})
const errors = reactive<Record<string, Partial<Record<keyof InspectionTeamDraft, boolean>>>>({})
const editableFields = ['name', 'description'] as const
const teamNames = computed(() => new Map(teams.value.map(team => [team.id, team.name])))
const columns = computed(() => [
  { title: t('demo.team.name'), dataIndex: 'name', key: 'name', scopedSlots: true, width: 180 },
  { title: t('demo.team.description'), dataIndex: 'description', key: 'description', scopedSlots: true },
  { title: t('demo.team.parent'), dataIndex: 'parentId', key: 'parentId', scopedSlots: true, width: 220 },
  { title: t('demo.team.state'), dataIndex: 'state', key: 'state', scopedSlots: true, width: 140 },
  { title: t('demo.record.actions'), dataIndex: 'action', key: 'action', scopedSlots: true, width: 220 },
])
const stateValue = (team: InspectionTeam) => typeof team.state === 'string' ? team.state : team.state.value
const stateText = (team: InspectionTeam) => typeof team.state === 'string' ? t(`demo.team.${team.state}`) : team.state.text
const stateOptions = (team?: InspectionTeam) => ['enabled', 'disabled'].map(value => ({
  value,
  label: team && stateValue(team) === value ? stateText(team) : t(`demo.team.${value}`),
}))

/** 父级不能选择自身及其后代；服务端同时校验真实层级关系。 */
const parentOptions = (currentId?: string) => {
  const excludedIds = new Set(currentId ? [currentId] : [])
  let changed = true
  while (changed) {
    changed = false
    teams.value.forEach(team => {
      if (team.parentId && excludedIds.has(team.parentId) && !excludedIds.has(team.id)) {
        excludedIds.add(team.id)
        changed = true
      }
    })
  }
  return teams.value.map(team => ({ id: team.id, pId: team.parentId || undefined, value: team.id, title: team.name, disabled: excludedIds.has(team.id) }))
}

const createSchema = computed(() => [
  {
    name: 'name', label: t('demo.team.name'), itemType: 'input',
    componentProps: { maxlength: 64, placeholder: t('demo.team.namePlaceholder') },
    rules: [{ required: true, whitespace: true, message: t('demo.team.nameRequired') }],
  },
  {
    name: 'description', label: t('demo.team.description'), itemType: 'textarea',
    componentProps: { rows: 2, maxlength: 512, placeholder: t('demo.team.descriptionPlaceholder') },
  },
  {
    name: 'parentId', label: t('demo.team.parent'), itemType: 'tree-select',
    componentProps: { treeData: parentOptions(), treeDataSimpleMode: true, allowClear: true, showSearch: true, treeNodeFilterProp: 'title', placeholder: t('demo.team.noParent') },
  },
  { name: 'state', label: t('demo.team.state'), itemType: 'select', componentProps: { options: stateOptions() } },
])

const create = async (draft: InspectionTeamDraft) => {
  const response = await createInspectionTeam(draft)
  if (response.success) onlyMessage(t('demo.record.saveSuccess', { name: draft.name }))
  return response
}

/** 独立班组页统一查询目录，维护成功后刷新同一份层级和名称。 */
const loadTeams = async () => {
  loading.value = true
  try {
    const response = await queryInspectionTeams()
    loadFailed.value = !response.success
    if (response.success) teams.value = response.result
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(loadTeams)

const created = () => {
  createOpen.value = false
  loadTeams()
}

const openMembers = (team: InspectionTeam) => {
  if (!getApplicationRuntimeEntry().applicationId) {
    onlyMessage(t('demo.team.membersApplicationRequired'), 'warning')
    return
  }
  memberTeam.value = team
}

/** 失败保留字段输入；局部更新不覆盖同班组的其他业务字段。 */
const saveField = async (team: InspectionTeam, field: keyof InspectionTeamDraft, value?: string) => {
  drafts[team.id] = { ...drafts[team.id], [field]: value ?? '' }
  errors[team.id] = { ...errors[team.id], [field]: false }
  busyIds.value.push(team.id)
  try {
    const response = await updateInspectionTeam(team.id, { [field]: value ?? '' })
    if (response.success) {
      delete drafts[team.id][field]
      onlyMessage(t('demo.record.saveSuccess', { name: team.name }))
      await loadTeams()
    } else {
      errors[team.id][field] = true
    }
  } catch {
    errors[team.id][field] = true
  } finally {
    busyIds.value = busyIds.value.filter(id => id !== team.id)
  }
}

const remove = async (team: InspectionTeam) => {
  busyIds.value.push(team.id)
  try {
    const response = await deleteInspectionTeam(team.id)
    if (response.success) {
      delete drafts[team.id]
      delete errors[team.id]
      onlyMessage(t('demo.record.deleteSuccess', { name: team.name }))
      await loadTeams()
    }
  } finally {
    busyIds.value = busyIds.value.filter(id => id !== team.id)
  }
}
</script>

<style scoped>
.inspection-teams__select {
  width: 100%;
}

.inspection-teams__toolbar {
  padding: var(--space-4) var(--space-6) 0;
}

.inspection-teams__table {
  min-height: 0;
  flex: 1;
}

.inspection-teams__error {
  display: block;
  color: var(--danger);
  font-size: var(--fs-12);
}
</style>
