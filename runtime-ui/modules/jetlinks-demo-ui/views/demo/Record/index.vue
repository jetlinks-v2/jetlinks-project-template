<template>
  <j-page-container>
    <FullPage has-padding transparent-background>
      <a-alert v-if="loadFailed" type="error" show-icon :message="$t('demo.record.loadFailed')" />
      <a-alert v-if="teamsLoadFailed" type="error" show-icon :message="$t('demo.team.loadFailed')" />
      <j-pro-table
        ref="tableRef"
        class="demo-record-table"
        mode="TABLE"
        row-key="id"
        :columns="columns"
        :request="loadRecords"
        :params="params"
        :default-params="{ sorts: [{ name: 'createTime', order: 'desc' }] }"
        :scroll="{ x: 1180 }"
        @request-error="loadFailed = true"
      >
        <template #headerLeftRender>
          <ConditionFilter
            :fields="filterFields"
            :placeholder="$t('demo.record.searchPlaceholder')"
            @change="search"
          />
        </template>
        <template #headerRightRender>
          <a-space>
            <a-button v-if="canOpenTeams" @click="menuStore.jumpPage('demo/InspectionTeam')">{{ $t('demo.team.manage') }}</a-button>
            <j-permission-button type="primary" has-permission="demo/Record:add" :disabled="teamsLoading || teamsLoadFailed" @click="createOpen = true">
              <AIcon type="PlusOutlined" />
              {{ $t('demo.record.add') }}
            </j-permission-button>
          </a-space>
        </template>
        <template #inspectionTeamId="record">
          <j-ellipsis>{{ teamNames.get(record.inspectionTeamId) || record.inspectionTeamId }}</j-ellipsis>
        </template>
        <template #inspectionItems="record">
          {{ record.inspectionItems.length }}
        </template>
        <template v-for="field in editableFields" :key="field" #[field]="record">
          <div class="demo-record-cell">
            <InputEditable
              v-if="canUpdate && !savingIds.includes(record.id) && !deletingIds.includes(record.id)"
              :value="drafts[record.id]?.[field] ?? record[field] ?? ''"
              :max-length="field === 'name' ? 64 : 512"
              @change="(value: string) => saveField(record, field, value)"
            />
            <j-ellipsis v-else>{{ (drafts[record.id]?.[field] ?? record[field]) || $t('comm.table.empty') }}</j-ellipsis>
            <span v-if="fieldErrors[record.id]?.[field]" class="demo-record-cell__error" role="alert">
              {{ $t('demo.record.saveFailed') }}
            </span>
          </div>
        </template>
        <template #createTime="record">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template #action="record">
          <j-permission-button
            type="link"
            has-permission="demo/Record:update"
            :disabled="savingIds.includes(record.id) || deletingIds.includes(record.id) || teamsLoading || teamsLoadFailed"
            @click="editingRecord = record"
          >
            {{ $t('demo.record.editInspection') }}
          </j-permission-button>
          <j-permission-button
            type="link"
            danger
            has-permission="demo/Record:delete"
            :loading="deletingIds.includes(record.id)"
            :disabled="savingIds.includes(record.id)"
            :pop-confirm="{ title: $t('demo.record.deleteConfirm', { name: record.name }), onConfirm: () => remove(record) }"
          >
            {{ $t('demo.record.delete') }}
          </j-permission-button>
          <span v-if="savingIds.includes(record.id)" role="status">{{ $t('demo.record.saving') }}</span>
        </template>
        <template #emptyText>
          <a-empty v-if="!loadFailed" :description="params.terms.length ? $t('demo.record.noMatches') : $t(canCreate ? 'demo.record.emptyCreate' : 'demo.record.empty')" />
        </template>
      </j-pro-table>
    </FullPage>
    <RecordEditor
      v-if="createOpen || editingRecord"
      :record="editingRecord"
      :teams="teams"
      :request="editingRecord ? saveInspection : create"
      @close="createOpen = false; editingRecord = undefined"
      @save="created"
    />
  </j-page-container>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, defineAsyncComponent } from 'vue'
import { FullPage } from '@jetlinks-web-core/layout'
import ConditionFilter from '@jetlinks-web-core/components/ConditionFilter'
import InputEditable from '@jetlinks-web-core/components/Editable/InputEditable.vue'
import { useMenuStore } from '@jetlinks-web-core/store/menu'
import { useRecords } from './useRecords'

const RecordEditor = defineAsyncComponent(() => import('./RecordEditor.vue'))
const menuStore = useMenuStore()
const canOpenTeams = computed(() => menuStore.hasMenu('demo/InspectionTeam'))
const {
  tableRef, params, columns, filterFields, createOpen, editingRecord, loadFailed,
  teams, teamsLoading, teamsLoadFailed, teamNames,
  canUpdate, canCreate, savingIds, deletingIds, drafts, fieldErrors, editableFields,
  loadRecords, search, create, created, saveInspection, saveField, remove,
} = useRecords()
</script>

<style scoped>
.demo-record-table {
  min-height: 0;
}

.demo-record-cell {
  min-width: 0;
}

.demo-record-cell__error {
  display: block;
  margin-top: var(--space-1);
  color: var(--danger);
  font-size: var(--fs-12);
}
</style>
