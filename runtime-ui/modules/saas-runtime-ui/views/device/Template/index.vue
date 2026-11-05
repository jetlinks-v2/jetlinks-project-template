<template>

  <DefineEmptyCreate>
    <j-permission-button type="primary" hasPermission="device/Template:add" @click="handleAdd">
      <template #icon><AIcon type="PlusOutlined" /></template>
      {{ $t('template.list.add') || '新增' }}
    </j-permission-button>
  </DefineEmptyCreate>
  <j-page-container v-bind="$attrs">
    <div class="template-page">
      <div class="template-page__left">
        <TagManagerSidebar
          v-model:selectedTagIds="selectedTagIds"
          :client="tagClient"
          :permission="'device/Template'"
          @change="reloadByTag"
        />
      </div>
      <div class="template-page__right">
        <div class="template-page__toolbar">
          <div class="template-page__toolbar-title">{{ $t('template.list.title') || '产品模板' }}</div>
          <EmptyCreateButton />
        </div>
        <div class="template-page__panel">
          <pro-search
            :columns="searchColumns"
            target="deviceTemplateList"
            type="simple"
            style="padding: 0; margin-bottom: 12px"
            @search="handleSearch"
          />

          <a-spin :spinning="loading">
            <div v-if="templateList.length" class="template-card-grid">
              <div v-for="item in templateList" :key="item.id" class="template-card" @click="handleView(item)">
                  <div class="template-card__status">
                    <j-badge-status
                      :status="item.state?.value === 'enabled' ? 'success' : 'error'"
                      :text="item.state?.value === 'enabled' ? ($t('template.list.state.enabled') || '正常') : ($t('template.list.state.disabled') || '禁用')"
                    />
                  </div>
                <div class="template-card__main">
                  <div class="template-card__cover">
                    <img :src="getTemplatePhoto(item)" :alt="item.name || '-'" @error="handleTemplateImageError" />
                  </div>
                  <div class="template-card__content">
                    <div class="template-card__head">
                      <div class="template-card__name">
                        <j-ellipsis>{{ item.name || '-' }}</j-ellipsis>
                      </div>
                    </div>
                    <div class="template-card__meta">
                      <span>{{ item.classifiedName || '-' }}</span>
                      <span>{{ item.accessName || ($t('template.list.notConfigured') || '未配置接入') }}</span>
                    </div>
                    <div class="template-card__desc">
                      <j-ellipsis>{{ item.description || item.describe || '-' }}</j-ellipsis>
                    </div>
                  </div>
                </div>
                <div class="template-card__footer">
                  <div class="template-card__tags" :ref="(el) => (tagWrapRefMap[String(item.id || '')] = (el as any)?.$el || el)">
                    <a-tag
                      v-for="(tag, index) in getTemplateTags(item)"
                      v-show="index < getVisibleTagCount(item)"
                      :ref="(el) => setTagItemRef(String(item.id || ''), index, el as any)"
                      :key="`${item.id}-${tag.id}`"
                    >
                      <AIcon type="TagOutlined" />
                      {{ tag.name }}
                    </a-tag>
                    <a-tooltip v-if="getHiddenTagCount(item) > 0" :title="getHiddenTemplateTagNames(item)">
                      <span class="more-tag">+{{ getHiddenTagCount(item) }}</span>
                    </a-tooltip>
                  </div>
                </div>
                <div class="template-card__tools" @click.stop>
                  <div class="template-card__tool-btn">
                    <j-permission-button type="link" hasPermission="device/Template:update" @click="handleEdit(item)">
                      <AIcon type="EditOutlined" />
                      <span>{{ $t('template.list.edit') || '编辑' }}</span>
                    </j-permission-button>
                  </div>
                  <div class="template-card__tool-btn">
                    <j-permission-button
                      type="link"
                      hasPermission="device/Template:action"
                      :popConfirm="{
                        title: item.state?.value === 'enabled' ? ($t('template.list.disableConfirm') || '确认禁用？') : ($t('template.list.enableConfirm') || '确认启用？'),
                        onConfirm: () => handleToggleState(item),
                      }"
                    >
                      <AIcon :type="item.state?.value === 'enabled' ? 'StopOutlined' : 'CheckCircleOutlined'" />
                      <span>{{ item.state?.value === 'enabled' ? ($t('template.list.disable') || '禁用') : ($t('template.list.enable') || '启用') }}</span>
                    </j-permission-button>
                  </div>
                  <div class="template-card__tool-btn template-card__tool-btn--delete" :class="{ 'is-disabled': item.state?.value === 'enabled' }">
                    <j-permission-button
                      type="link"
                      danger
                      :disabled="item.state?.value === 'enabled'"
                      :tooltip="item.state?.value === 'enabled' ? { title: $t('template.list.disableBeforeDelete') || '请先禁用后再删除' } : undefined"
                      hasPermission="device/Template:delete"
                      :popConfirm="{
                        title: $t('template.list.deleteConfirm') || '确认删除？',
                        onConfirm: () => handleDelete(item.id),
                      }"
                    >
                      <AIcon type="DeleteOutlined" />
                    </j-permission-button>
                  </div>
                </div>
              </div>
            </div>
            <CloudEmpty v-else style="margin-top: 60px" :search="searchTerms.length > 0 || selectedTagIds.length > 0" :search-name="$t('EmptyCreate.names.device_templates')">
              <EmptyCreateButton />
            </CloudEmpty>
          </a-spin>

          <div class="template-page__pagination">
            <a-pagination
              :current="pageIndex + 1"
              :pageSize="pageSize"
              :total="total"
              show-size-changer
              :pageSizeOptions="['12', '24', '48', '96']"
              @change="onPageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <Save ref="saveRef" :isAdd="isAdd" :title="title" @success="refresh" />
  </j-page-container>
</template>

<script setup lang="ts" name="DeviceTemplate">
import { createReusableTemplate } from '@vueuse/core'

import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMenuStore } from '@jetlinks-web-core/store'
import { TagManagerSidebar } from '@jetlinks-web-core/components/TagManagerSidebar'
import { onlyMessage, accessConfigTypeFilter } from '@jetlinks-web-core/utils/comm'
import { device } from '@device-manager-ui/assets'
import { category, getProviders, queryGatewayList } from '@device-manager-ui/api/product'
import { accessType } from '@device-manager-ui/views/device/data'
import Save from './Save/index.vue'
import type { TemplateDetailItem, TemplateItem } from './typings'
import { queryTemplateDetailList, deleteTemplate, deployTemplate, undeployTemplate } from '../../../api/template'
import {
  queryTagCategoryNoPaging,
  queryTagTree,
  saveTagCategory,
  deleteTagCategory as deleteTagCategoryApi,
  saveTag,
  deleteTag as deleteTagApi,
} from '../../../api/tag'

const { t: $t } = useI18n()
const menuStore = useMenuStore()

const isAdd = ref(0)
const title = ref('')
const saveRef = ref()

const tagType = 'device'
const selectedTagIds = ref<string[]>([])
const tagClient = {
  queryCategories: () =>
    queryTagCategoryNoPaging({
      paging: false,
      sorts: [{ name: 'sortIndex', order: 'asc' }],
      terms: [{ column: 'type', termType: 'eq', value: tagType }],
    }),
  queryTags: (categoryId: string) => queryTagTree({ terms: [{ column: 'categoryId', value: categoryId }] }),
  saveCategory: (payload: any) => saveTagCategory({ ...payload, type: tagType }),
  updateCategory: (payload: any) => saveTagCategory({ ...payload, type: tagType }),
  deleteCategory: (id: string) => deleteTagCategoryApi(id),
  saveTag: (payload: any) => saveTag(payload),
  updateTag: (payload: any) => saveTag(payload),
  deleteTag: (id: string) => deleteTagApi(id),
}

const loading = ref(false)
const templateList = ref<TemplateItem[]>([])
const templateTagMap = ref<Record<string, any[]>>({})
const tagWrapRefMap = ref<Record<string, HTMLElement | null>>({})
const tagItemRefMap = ref<Record<string, HTMLElement[]>>({})
const tagWidthMap = ref<Record<string, number[]>>({})
const visibleTagCountMap = ref<Record<string, number>>({})
const typeList = ref<any[]>([])
const searchTerms = ref<any[]>([])
const pageIndex = ref(0)
const pageSize = ref(12)
const total = ref(0)
let resizeTimer: any

const normalizeDetailListResult = (res: any): { data: TemplateDetailItem[]; total: number } => {
  const raw = res?.result
  if (Array.isArray(raw)) {
    return {
      data: raw,
      total: raw.length,
    }
  }
  const data = raw?.data || raw?.records || []
  const totalCount = raw?.total ?? raw?.totalElements ?? data.length
  return {
    data,
    total: Number(totalCount || 0),
  }
}

const buildTagMap = (detailList: TemplateDetailItem[]) => {
  const tagMap: Record<string, any[]> = {}
  detailList.forEach((item: TemplateDetailItem) => {
    const targetId = String(item.id || '')
    if (!targetId) return
    tagMap[targetId] = Array.isArray(item?.tags) ? item.tags : []
  })
  templateTagMap.value = tagMap
}

const buildTerms = () => {
  const tagBind = selectedTagIds.value.map((tagId) => ({
    column: 'id$common-tag-bind',
    value: [
      { column: 'tagId$common-tag-child', value: [tagId] },
      { column: 'targetType', termType: 'eq', value: 'device-template' },
    ],
  }))

  return [...(searchTerms.value || []), ...tagBind]
}

const queryTemplatePage = async () => {
  loading.value = true
  const res: any = await queryTemplateDetailList({
    pageIndex: pageIndex.value,
    pageSize: pageSize.value,
    terms: buildTerms(),
    sorts: [{ name: 'createTime', order: 'desc' }],
  }).catch(() => ({}))

  if (res?.status === 200 || res?.success) {
    const { data, total: totalCount } = normalizeDetailListResult(res)
    const detailList = data || []
    templateList.value = detailList
    buildTagMap(detailList)
    total.value = totalCount
    await recalcTagVisibility()
  } else {
    templateList.value = []
    total.value = 0
    templateTagMap.value = {}
    tagWidthMap.value = {}
    visibleTagCountMap.value = {}
  }
  loading.value = false
}

const getTemplateTags = (item: TemplateItem) => templateTagMap.value[String(item.id)] || []
const setTagItemRef = (id: string, index: number, el: any) => {
  if (!id) return
  if (!tagItemRefMap.value[id]) tagItemRefMap.value[id] = []
  const dom = el?.$el || el
  if (dom instanceof HTMLElement) tagItemRefMap.value[id][index] = dom
}
const getVisibleTagCount = (item: TemplateItem) => {
  const id = String(item.id || '')
  return visibleTagCountMap.value[id] ?? getTemplateTags(item).length
}
const getHiddenTagCount = (item: TemplateItem) => {
  const totalCount = getTemplateTags(item).length
  return Math.max(totalCount - getVisibleTagCount(item), 0)
}
const getHiddenTemplateTagNames = (item: TemplateItem) =>
  getTemplateTags(item)
    .slice(getVisibleTagCount(item))
    .map((tag) => tag?.name)
    .filter(Boolean)
    .join('、')

const calcVisibleCount = (widthList: number[], containerWidth: number) => {
  if (!widthList.length || containerWidth <= 0) return 0
  const gap = 6
  const fitCount = (maxWidth: number) => {
    let used = 0
    let count = 0
    for (let index = 0; index < widthList.length; index += 1) {
      const nextWidth = widthList[index] + (index > 0 ? gap : 0)
      if (used + nextWidth > maxWidth) break
      used += nextWidth
      count += 1
    }
    return count
  }

  let reserveWidth = 0
  let count = fitCount(containerWidth)
  if (count >= widthList.length) return count

  for (let index = 0; index < 3; index += 1) {
    count = fitCount(Math.max(containerWidth - reserveWidth, 0))
    const hiddenCount = widthList.length - count
    if (hiddenCount <= 0) return widthList.length
    const nextReserve = 18 + String(hiddenCount).length * 8
    if (nextReserve === reserveWidth) break
    reserveWidth = nextReserve
  }
  return Math.max(1, Math.min(count, widthList.length))
}

const recalcTagVisibility = async () => {
  if (!templateList.value.length) return
  templateList.value.forEach((item) => {
    const id = String(item.id || '')
    if (!id) return
    visibleTagCountMap.value[id] = getTemplateTags(item).length
  })
  await nextTick()

  templateList.value.forEach((item) => {
    const id = String(item.id || '')
    if (!id) return
    const tags = getTemplateTags(item)
    if (!tags.length) {
      visibleTagCountMap.value[id] = 0
      return
    }
    const wrap = tagWrapRefMap.value[id]
    const tagEls = tagItemRefMap.value[id] || []
    if (!wrap || !tagEls.length) {
      visibleTagCountMap.value[id] = tags.length
      return
    }
    const widths = tagEls.slice(0, tags.length).map((element) => element?.getBoundingClientRect().width || 0)
    tagWidthMap.value[id] = widths
    visibleTagCountMap.value[id] = calcVisibleCount(widths, wrap.clientWidth)
  })
}

const handleResize = () => {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    templateList.value.forEach((item) => {
      const id = String(item.id || '')
      const widths = tagWidthMap.value[id] || []
      const wrap = tagWrapRefMap.value[id]
      if (!id || !widths.length || !wrap) return
      visibleTagCountMap.value[id] = calcVisibleCount(widths, wrap.clientWidth)
    })
  }, 80)
}

const reloadByTag = () => {
  pageIndex.value = 0
  queryTemplatePage()
}

const handleSearch = (e: any) => {
  searchTerms.value = e?.terms || []
  pageIndex.value = 0
  queryTemplatePage()
}

const onPageChange = (current: number, size: number) => {
  pageIndex.value = Math.max(current - 1, 0)
  pageSize.value = size
  queryTemplatePage()
}

const getTemplatePhoto = (item: TemplateItem) => item?.photoUrl || device.deviceProduct

const handleTemplateImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  if (target.src !== device.deviceProduct) target.src = device.deviceProduct
}

const handleAdd = () => {
  isAdd.value = 1
  title.value = $t('template.form.title.add') || '新增模板'
  nextTick(() => saveRef.value?.show({}))
}

const handleView = (record: TemplateItem) => {
  menuStore.jumpPage('device/Template/Detail', { params: { id: record.id } })
}

const handleEdit = (record: TemplateItem) => {
  isAdd.value = 2
  title.value = $t('template.form.title.edit') || '编辑模板'
  nextTick(() => saveRef.value?.show(record))
}

const handleToggleState = async (record: TemplateItem) => {
  try {
    if (record.state.value === 'enabled') {
      await undeployTemplate(record.id)
      onlyMessage($t('template.message.disableSuccess') || '禁用成功', 'success')
    } else {
      await deployTemplate(record.id)
      onlyMessage($t('template.message.enableSuccess') || '启用成功', 'success')
    }
    queryTemplatePage()
  } catch {
    onlyMessage($t('template.message.failed') || '操作失败', 'error')
  }
}

const handleDelete = async (id: string) => {
  try {
    await deleteTemplate(id)
    onlyMessage($t('template.message.deleteSuccess') || '删除成功', 'success')
    queryTemplatePage()
  } catch {
    onlyMessage($t('template.message.deleteFailed') || '删除失败', 'error')
  }
}

const refresh = () => queryTemplatePage()

const normalizeTreeData = (items: Record<string, any>[] = []): Record<string, any>[] =>
  items.map(({ children, ...item }) => ({
    ...item,
    key: item.id,
    children: children?.length ? normalizeTreeData(children) : undefined,
  }))

const searchColumns = [
  {
    title: $t('Product.index.660348-28'),
    dataIndex: 'name',
    key: 'name',
    search: { type: 'string' },
  },
  {
    title: $t('Product.index.660348-33'),
    key: 'classifiedId',
    dataIndex: 'classifiedId',
    search: {
      type: 'treeSelect',
      options: async () => {
        return new Promise((res) => {
          category({ paging: false }).then((resp) => {
            res(normalizeTreeData(resp.result || []))
          })
        })
      },
    },
  },
  {
    title: $t('Product.index.660348-29'),
    key: 'accessProvider',
    dataIndex: 'accessProvider',
    search: {
      type: 'select',
      options: async () => {
        return new Promise((resolve) => {
          getProviders().then((resp: any) => {
            const data = resp.result || []
            resolve(
              accessConfigTypeFilter(data).filter((i: any) => {
                return accessType.includes(i.value)
              }),
            )
          })
        })
      },
    },
  },
  {
    title: $t('Product.index.660348-5'),
    key: 'accessId',
    dataIndex: 'accessId',
    search: {
      type: 'select',
      options: async () => {
        return new Promise((res) => {
          queryGatewayList({ paging: false }).then((resp: any) => {
            typeList.value = (resp.result || []).map((item: any) => ({
              label: item.name,
              value: item.id,
            }))
            res(typeList.value)
          })
        })
      },
    },
  },
]

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  clearTimeout(resizeTimer)
})

watch(
  () => [templateList.value.length, templateTagMap.value],
  () => {
    nextTick(() => recalcTagVisibility())
  },
  { deep: true },
)

queryTemplatePage()

const [DefineEmptyCreate, EmptyCreateButton] = createReusableTemplate()
</script>

<style scoped lang="less">
.template-page {
  display: flex;
  gap: 16px;
  height: 100%;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
}
.template-page__left {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}
.template-page__right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.template-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.template-page__toolbar-title {
  color: rgba(0, 0, 0, 0.88);
  font-size: 16px;
  font-weight: 600;
}
.template-page__panel {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.template-page__panel :deep(.ant-spin-nested-loading) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.template-page__panel :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.template-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  align-content: start;
  flex: 1;
  overflow-y: auto;
}
.template-card {
  position: relative;
  border: 1px solid #e6ebf5;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.template-card:hover {
  border-color: #91caff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.template-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.template-card__main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.template-card__cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  overflow: hidden;
  flex-shrink: 0;
}
.template-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.template-card__content {
  flex: 1;
  min-width: 0;
}
.template-card__name {
  font-size: 15px;
  font-weight: 600;
  flex: 1;
  min-width: 0;
}
.template-card__meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
}
.template-card__desc {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  min-height: 20px;
}
.template-card__status {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 1;
}

.template-card__footer {
  margin-top: 10px;
}
.template-card__tags {
  display: flex;
  gap: 6px;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}
.template-card__tags :deep(.ant-tag) {
  margin-inline-end: 0;
}
.template-card__tools {
  display: flex;
  margin-top: 10px;
  border-top: 1px solid #f0f0f0;
}
.template-card__tool-btn {
  flex: 1;
  text-align: center;
}
.template-card__tool-btn :deep(button) {
  width: 100%;
  background: #f6f6f6;
  border: 1px solid #e6e6e6;
  color: @primary-color;
  border-radius: 0;
}
.template-card__tool-btn :deep(button):hover {
  background-color: @primary-color-hover;
  border-color: @primary-color-hover;
}
.template-card__tool-btn :deep(button):hover span {
  color: #fff !important;
}
.template-card__tool-btn:not(:last-child) {
  margin-right: 8px;
}
.template-card__tool-btn--delete {
  flex: 0 0 60px;
}
.template-card__tool-btn--delete :deep(button) {
  background: @error-color-deprecated-bg;
  border: 1px solid @error-color-outline;
}
.template-card__tool-btn--delete :deep(button) span {
  color: @error-color !important;
}
.template-card__tool-btn--delete :deep(button):hover {
  background-color: @error-color-hover;
}
.template-card__tool-btn--delete :deep(button):hover span {
  color: #fff !important;
}
.template-card__tool-btn.is-disabled :deep(button) {
  background: #f5f5f5 !important;
  border-color: #e5e7eb !important;
  color: rgba(0, 0, 0, 0.25) !important;
  cursor: not-allowed;
}
.template-card__tool-btn.is-disabled :deep(button) span {
  color: rgba(0, 0, 0, 0.25) !important;
}
.template-card__tool-btn.is-disabled :deep(button:hover) {
  background: #f5f5f5 !important;
  border-color: #e5e7eb !important;
}
.template-card__tool-btn.is-disabled :deep(button:hover) span {
  color: rgba(0, 0, 0, 0.25) !important;
}
.more-tag {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  line-height: 22px;
  cursor: pointer;
  flex-shrink: 0;
}
.template-page__pagination {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}
</style>
