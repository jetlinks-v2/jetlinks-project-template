<template>
  <j-page-container>
    <FullPage has-padding transparent-background>
      <a-alert v-if="loadFailed" type="error" show-icon :message="$t('demo.record.loadFailed')" />
      <j-pro-table
        ref="tableRef"
        class="demo-record-table"
        mode="TABLE"
        row-key="id"
        :columns="columns"
        :request="loadRecords"
        :params="params"
        :default-params="{ sorts: [{ name: 'createTime', order: 'desc' }] }"
        :scroll="{ x: 780 }"
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
          <j-permission-button type="primary" has-permission="demo/Record:add" @click="createOpen = true">
            <AIcon type="PlusOutlined" />
            {{ $t('demo.record.add') }}
          </j-permission-button>
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
    <EditDialog
      v-if="createOpen"
      :title="$t('demo.record.add')"
      :schema="createSchema"
      :data="createDraft"
      :request="create"
      @close="createOpen = false"
      @save="created"
    />
  </j-page-container>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { FullPage } from '@jetlinks-web-core/layout'
import ConditionFilter from '@jetlinks-web-core/components/ConditionFilter'
import InputEditable from '@jetlinks-web-core/components/Editable/InputEditable.vue'
import EditDialog from '@jetlinks-web-core/components/EditDialog/index.vue'
import { useRecords } from './useRecords'

const {
  tableRef, params, columns, filterFields, createSchema, createDraft, createOpen, loadFailed,
  canUpdate, canCreate, savingIds, deletingIds, drafts, fieldErrors, editableFields,
  loadRecords, search, create, created, saveField, remove,
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
