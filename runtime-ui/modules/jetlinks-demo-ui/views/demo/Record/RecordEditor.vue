<template>
  <EditDialog
    :title="$t(record ? 'demo.record.editInspection' : 'demo.record.add')"
    :schema="schema"
    :data="formData"
    :request="request"
    :handle-request-data="buildDraft"
    @close="emit('close')"
    @save="emit('save')"
  >
    <template #inspectionItems>
      <a-space direction="vertical" class="inspection-items">
        <a-alert type="info" show-icon :message="$t('demo.record.itemsHint')" />
        <div v-for="(item, index) in items" :key="index" class="inspection-item">
          <div class="inspection-item__header">
            <span>{{ $t('demo.record.itemTitle', { index: index + 1 }) }}</span>
            <a-button type="link" danger size="small" :disabled="items.length === 1" @click="items.splice(index, 1)">
              {{ $t('demo.record.removeItem') }}
            </a-button>
          </div>
          <a-input v-model:value="item.name" :maxlength="64" :aria-label="$t('demo.record.itemName')" :placeholder="$t('demo.record.itemNamePlaceholder')" />
          <a-textarea v-model:value="item.requirement" :maxlength="512" :rows="2" :aria-label="$t('demo.record.itemRequirement')" :placeholder="$t('demo.record.itemRequirementPlaceholder')" />
        </div>
        <a-button block @click="items.push({ name: '', requirement: '' })">
          <AIcon type="PlusOutlined" />
          {{ $t('demo.record.addItem') }}
        </a-button>
      </a-space>
    </template>
  </EditDialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import EditDialog from '@jetlinks-web-core/components/EditDialog/index.vue'
import type { DemoInspectionItem, DemoRecord, DemoRecordDraft, InspectionTeam } from '../../../types'

const props = defineProps({
  record: { type: Object as PropType<DemoRecord>, default: undefined },
  teams: { type: Array as PropType<InspectionTeam[]>, required: true },
  request: { type: Function as PropType<(draft: DemoRecordDraft) => Promise<{ success: boolean }>>, required: true },
})
const emit = defineEmits(['close', 'save'])
const { t } = useI18n()
const formData = {
  name: props.record?.name ?? '',
  description: props.record?.description ?? '',
  inspectionTeamId: props.record?.inspectionTeamId,
}
// EditDialog 的 schema 插槽承载清单，数组单独保存并在提交时组装。
const items = ref<DemoInspectionItem[]>(props.record
  ? props.record.inspectionItems.map(item => ({ ...item }))
  : [{ name: '', requirement: '' }])

/** 禁用班组及其下级不可接收新计划，原归属仍可保留以调整现有清单。 */
const teamOptions = computed(() => {
  const disabledIds = new Set(props.teams.filter(team =>
    (typeof team.state === 'string' ? team.state : team.state.value) === 'disabled').map(team => team.id))
  let changed = true
  while (changed) {
    changed = false
    props.teams.forEach(team => {
      if (team.parentId && disabledIds.has(team.parentId) && !disabledIds.has(team.id)) {
        disabledIds.add(team.id)
        changed = true
      }
    })
  }
  return props.teams.map(team => ({
    id: team.id,
    pId: team.parentId || undefined,
    value: team.id,
    title: team.name,
    disabled: disabledIds.has(team.id) && team.id !== props.record?.inspectionTeamId,
  }))
})

const schema = computed(() => [
  ...(!props.record ? [{
    name: 'name',
    label: t('demo.record.name'),
    itemType: 'input',
    componentProps: { maxlength: 64, placeholder: t('demo.record.namePlaceholder') },
    rules: [{ required: true, whitespace: true, message: t('demo.record.nameRequired') }],
  }] : []),
  {
    name: 'inspectionTeamId',
    label: t('demo.record.inspectionTeam'),
    itemType: 'tree-select',
    componentProps: { treeData: teamOptions.value, treeDataSimpleMode: true, showSearch: true, treeNodeFilterProp: 'title', placeholder: t('demo.record.teamRequired') },
    rules: [{ required: true, message: t('demo.record.teamRequired') }],
  },
  ...(!props.record ? [{
    name: 'description',
    label: t('demo.record.description'),
    itemType: 'textarea',
    componentProps: { rows: 2, maxlength: 512, placeholder: t('demo.record.descriptionPlaceholder') },
  }] : []),
  {
    name: 'inspectionItems',
    label: t('demo.record.inspectionItems'),
    slotName: 'inspectionItems',
    rules: [{ validator: async () => {
      if (!items.value.length || items.value.some(item => !item.name.trim() || !item.requirement.trim())) {
        throw new Error(t('demo.record.itemsRequired'))
      }
    } }],
  },
])

/** 只提交业务字段，巡检项数量始终由实际清单决定。 */
const buildDraft = (value: Omit<DemoRecordDraft, 'inspectionItems'>): DemoRecordDraft => ({
  name: value.name,
  description: value.description,
  inspectionTeamId: value.inspectionTeamId,
  inspectionItems: items.value.map(item => ({ name: item.name, requirement: item.requirement })),
})
</script>

<style scoped>
.inspection-items {
  width: 100%;
}

.inspection-item {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-jet-border-secondary);
  border-radius: var(--radius-jet);
}

.inspection-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
