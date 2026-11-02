<template>
  <a-modal
    :title="tText('product.add.picker.title', '选择产品模板')"
    open
    width="1220px"
    :maskClosable="false"
    :bodyStyle="{ paddingBottom: '20px', height: '55vh', overflow: 'hidden' }"
    @ok="handleSubmit"
    @cancel="() => emit('cancel')"
  >
    <div class="choose-template">
      <div class="choose-template__left">
        <TagManagerSidebar
          v-model:selectedTagIds="selectedTagIds"
          :client="tagClient"
          :permission="'device/Template'"
          @change="reloadByTag"
        />
      </div>

      <div class="choose-template__right">
        <pro-search
          :columns="searchColumns"
          target="templateModalInProductAdd"
          @search="handleSearch"
          type="simple"
          style="padding: 0; margin-bottom: 12px"
        />

        <a-spin :spinning="loading">
          <div v-if="templateList.length" class="template-card-list">
            <div
              v-for="item in templateList"
              :key="item.id"
              class="template-card"
              :class="{ active: selectedTemplate?.id === item.id }"
              @click="selectTemplate(item)"
            >
              <div class="template-card__main">
                <div class="template-card__cover">
                  <img :src="getTemplatePhoto(item)" :alt="item.name || '-'" @error="handleTemplateImageError" />
                </div>
                <div class="template-card__content">
                  <div class="template-card__head">
                    <div class="template-card__name">{{ item.name || '-' }}</div>
                    <AIcon v-if="selectedTemplate?.id === item.id" type="CheckCircleFilled" class="selected-icon" />
                  </div>
                  <div class="template-card__meta">
                    <span>{{ item.classifiedName || tText('product.add.picker.unknown.classified', '未分类') }}</span>
                    <span>{{ item.accessName || tText('product.add.picker.unknown.access', '未配置接入') }}</span>
                  </div>
                  <div class="template-card__desc">
                    <j-ellipsis>{{ item.description || item.describe || '-' }}</j-ellipsis>
                  </div>
                </div>
              </div>
              <div class="template-card__tags">
                <a-tag v-for="tag in getVisibleTemplateTags(item)" :key="`${item.id}-${tag.id}`">
                  <AIcon type="TagOutlined" />
                  {{ tag.name }}
                </a-tag>
                <a-tooltip v-if="getHiddenTemplateTags(item).length" :title="getHiddenTemplateTagNames(item)">
                  <span class="more-tag">+{{ getHiddenTemplateTags(item).length }}</span>
                </a-tooltip>
              </div>
            </div>
          </div>
          <CloudEmpty
            v-else
            :description="tText('product.add.picker.template.empty', '未找到匹配模板，请调整标签或筛选条件')"
          />
        </a-spin>

        <div class="template-footer">
          <div class="template-footer__tip">
            {{ selectedTemplate ? `已选择：${selectedTemplate.name}` : tText('product.add.picker.selectTip', '请选择一个模板') }}
          </div>
          <a-pagination
            :current="pageIndex + 1"
            :pageSize="pageSize"
            :total="total"
            show-size-changer
            :pageSizeOptions="['4', '12', '24', '48']"
            :show-total="(v:number) => `${tText('product.add.picker.total', '共')} ${v} ${tText('product.add.picker.total.item', '项')}`"
            @change="onPageChange"
          />
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { TagManagerSidebar } from '@jetlinks-web-core/components/TagManagerSidebar'
import { onlyMessage, accessConfigTypeFilter } from '@jetlinks-web-core/utils/comm'
import { device } from '@device-manager-ui/assets'
import { category, getProviders, queryGatewayList } from '@device-manager-ui/api/product'
import { queryTemplateDetailList } from '../../../../api/template'
import {
  queryTagCategoryNoPaging,
  queryTagTree,
  saveTagCategory,
  deleteTagCategory as deleteTagCategoryApi,
  saveTag,
  deleteTag as deleteTagApi,
} from '../../../../api/tag'
import { accessType } from '@device-manager-ui/views/device/data'
import type { TemplateDetailItem, TemplateItem } from '../../../Template/typings'

const { t: $t } = useI18n()
const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const emit = defineEmits<{
  (e: 'cancel'): void
  (e: 'submit', template: TemplateItem): void
}>()

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

const selectedTemplate = ref<TemplateItem>()
const templateList = ref<TemplateItem[]>([])
const templateTagMap = ref<Record<string, any[]>>({})
const loading = ref(false)
const typeList = ref<any[]>([])
const searchTerms = ref<any[]>([])
const pageIndex = ref(0)
const pageSize = ref(4)
const total = ref(0)

const normalizeDetailListResult = (res: any): { data: TemplateDetailItem[]; total: number } => {
  const raw = res?.result
  if (Array.isArray(raw)) return { data: raw, total: raw.length }
  const data = raw?.data || raw?.records || []
  const totalCount = raw?.total ?? raw?.totalElements ?? data.length
  return { data, total: Number(totalCount || 0) }
}

const buildTagMap = (detailList: TemplateDetailItem[]) => {
  const tagMap: Record<string, any[]> = {}
  detailList.forEach((item: TemplateDetailItem) => {
    const id = String(item.id || '')
    if (!id) return
    tagMap[id] = Array.isArray(item?.tags) ? item.tags : []
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

  return [
    { column: 'state', termType: 'eq', value: 'enabled' },
    ...(searchTerms.value || []),
    ...tagBind,
  ]
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
    templateList.value = data || []
    buildTagMap(templateList.value as TemplateDetailItem[])
    total.value = totalCount
    if (selectedTemplate.value && !templateList.value.some(item => item.id === selectedTemplate.value?.id)) {
      selectedTemplate.value = undefined
    }
    if (!selectedTemplate.value && templateList.value.length) {
      selectedTemplate.value = templateList.value[0]
    }
  } else {
    templateList.value = []
    templateTagMap.value = {}
    total.value = 0
    selectedTemplate.value = undefined
  }
  loading.value = false
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

const selectTemplate = (item: TemplateItem) => {
  selectedTemplate.value = item
}

const getTemplatePhoto = (item: TemplateItem) => item?.photoUrl || device.deviceProduct
const handleTemplateImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  if (target.src !== device.deviceProduct) target.src = device.deviceProduct
}

const getTemplateTags = (item: TemplateItem) => templateTagMap.value[String(item.id)] || []
const MAX_VISIBLE_TAGS = 3
const getVisibleTemplateTags = (item: TemplateItem) => getTemplateTags(item).slice(0, MAX_VISIBLE_TAGS)
const getHiddenTemplateTags = (item: TemplateItem) => getTemplateTags(item).slice(MAX_VISIBLE_TAGS)
const getHiddenTemplateTagNames = (item: TemplateItem) => getHiddenTemplateTags(item).map((tag) => tag?.name).filter(Boolean).join('、')

function normalizeTreeData(items: Record<string, any>[] = []): Record<string, any>[] {
  return items.map(({ children, ...item }) => ({
    ...item,
    key: item.id,
    children: children?.length ? normalizeTreeData(children) : undefined,
  }))
}

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

const handleSubmit = () => {
  if (!selectedTemplate.value) {
    onlyMessage($t('template.message.selectTemplate'), 'error')
    return
  }
  emit('submit', selectedTemplate.value)
}

queryTemplatePage()
</script>

<style scoped lang="less">
.choose-template {
  display: flex;
  gap: 16px;
  height: 100%;
}

.choose-template__left {
  width: 340px;
  height: 100%;
  border-right: 1px solid #f0f0f0;
  padding-right: 16px;
  overflow: hidden;
}


.choose-template__right {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-bottom: 12px;
}

.template-card-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
  max-height: none;
  overflow: hidden;
}

.template-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  border-color: #91caff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.template-card.active {
  border-color: #2f54eb;
  box-shadow: 0 0 0 2px #e6f4ff inset;
  background: #f7faff;
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
  min-width: 0;
}

.selected-icon {
  color: #2f54eb;
  font-size: 16px;
}

.template-card__meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
}

.template-card__desc {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.65);
  min-height: 20px;
}

.template-card__tags {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.more-tag {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.template-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.template-footer__tip {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
}
</style>
