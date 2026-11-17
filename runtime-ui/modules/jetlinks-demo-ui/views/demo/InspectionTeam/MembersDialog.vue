<template>
  <a-modal
    open
    :title="$t('demo.team.membersTitle', { name: team.name })"
    :width="900"
    :mask-closable="false"
    destroy-on-close
    @cancel="emits('close')"
  >
    <a-alert type="info" show-icon :message="$t('demo.team.membersHint')" />
    <a-tabs v-model:active-key="activeTab" class="team-members__tabs">
      <a-tab-pane key="joined" :tab="$t('demo.team.joinedMembers')" :disabled="mutating" />
      <a-tab-pane key="available" :tab="$t('demo.team.availableMembers')" :disabled="mutating" />
    </a-tabs>
    <a-alert v-if="loadFailed" type="error" show-icon :message="$t('demo.team.membersLoadFailed')" />
    <ConditionFilter
      :key="activeTab"
      class="team-members__filter"
      :fields="filterFields"
      :placeholder="$t('demo.team.memberSearchPlaceholder')"
      @change="search"
    />
    <div v-if="activeTab === 'available'" class="team-members__toolbar">
      <j-permission-button
        type="primary"
        has-permission="demo/InspectionTeam:members"
        :loading="joining"
        :disabled="!selectedRowKeys.length || loading || !!removingUserId"
        @click="addMembers"
      >
        <AIcon type="UserAddOutlined" />
        {{ $t('demo.team.addMembers') }}
      </j-permission-button>
    </div>
    <div class="team-members__table">
      <j-pro-table
        :key="activeTab"
        ref="tableRef"
        row-key="id"
        mode="TABLE"
        type="PAGE"
        :columns="columns"
        :request="loadMembers"
        :params="params"
        :row-selection="rowSelection"
        :alert-show="false"
        :body-style="{ padding: 0 }"
        :default-params="{ sorts: [{ name: 'name', order: 'asc' }] }"
      >
        <template #action="member">
          <j-permission-button
            type="link"
            has-permission="demo/InspectionTeam:members"
            :loading="removingUserId === member.id"
            :disabled="mutating || loading"
            :pop-confirm="{
              title: $t('demo.team.removeMemberConfirm', { name: member.name || member.username }),
              onConfirm: () => removeMember(member),
            }"
          >
            {{ $t('demo.team.removeMember') }}
          </j-permission-button>
        </template>
      </j-pro-table>
    </div>
    <template #footer>
      <a-button :disabled="mutating" @click="emits('close')">{{ $t('demo.team.close') }}</a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import ConditionFilter from '@jetlinks-web-core/components/ConditionFilter'
import type { InspectionTeam } from '../../../types'
import { useTeamMembers } from './useTeamMembers'

const props = defineProps<{ team: InspectionTeam }>()
const emits = defineEmits<{ close: [] }>()
const {
  activeTab, tableRef, params, selectedRowKeys, loading, loadFailed, joining, removingUserId, mutating,
  filterFields, columns, rowSelection, loadMembers, search, addMembers, removeMember,
} = useTeamMembers(props.team.id)
</script>

<style scoped>
.team-members__tabs {
  margin-top: var(--space-3);
}

.team-members__filter,
.team-members__toolbar {
  margin-bottom: var(--space-3);
}

.team-members__table {
  height: 26rem;
  min-height: 0;
}
</style>
