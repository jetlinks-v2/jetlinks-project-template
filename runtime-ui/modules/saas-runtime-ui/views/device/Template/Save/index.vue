<template>
  <a-modal v-model:open="visible" :title="title" :width="800" @ok="handleOk" @cancel="handleCancel">
    <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical">
      <a-row type="flex">
        <a-col flex="180px">
          <a-form-item name="photoUrl">
            <pro-upload v-model="formData.photoUrl" :accept="imageTypes.toString()" />
          </a-form-item>
        </a-col>
        <a-col flex="auto">
          <a-form-item label="名称" name="name">
            <I18nTextField
              v-model:value="formData.name"
              v-model:i18nMessages="formData.i18nMessages"
              field="name"
              label="名称"
              :i18n-max-length="64"
              :placeholder="$t('template.form.name.placeholder') || '请输入模板名称'"
              :maxlength="64"
            />
          </a-form-item>
          <a-form-item label="所属分类" name="classifiedId">
            <a-tree-select
              v-model:value="formData.classifiedId"
              :tree-data="treeList"
              :placeholder="$t('template.form.category.placeholder') || '请选择所属分类'"
              allow-clear
              :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
              :filterTreeNode="(v, option) => filterSelectNode(v, option, 'name')"
              @change="valueChange"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="标签">
        <div class="tag-bind">
          <a-row v-for="(row, index) in tagRows" :key="row.key" :gutter="16" style="margin-bottom: 12px">
            <a-col :span="10">
              <a-select
                v-model:value="row.categoryId"
                placeholder="请选择标签分类"
                :options="tagCategoryOptions"
                allow-clear
                @change="() => onCategoryChange(row)"
              />
            </a-col>
            <a-col :span="12">
              <a-tree-select
                v-model:value="row.tagId"
                placeholder="请选择标签"
                :tree-data="row.tagTree"
                :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
                :disabled="!row.categoryId"
                allow-clear
                show-search
                :filterTreeNode="(v, option) => filterSelectNode(v, option, 'name')"
              />
            </a-col>
            <a-col :span="2" style="text-align: right">
              <a-button type="link" danger @click="removeTagRow(index)">删除</a-button>
            </a-col>
          </a-row>
          <div class="tag-bind__header">
            <div />
            <a-button type="link" @click="addTagRow">新增</a-button>
          </div>
        </div>
      </a-form-item>

      <a-form-item label="说明" name="description">
        <I18nTextField
          v-model:value="formData.description"
          v-model:i18nMessages="formData.i18nMessages"
          field="description"
          label="说明"
          :placeholder="$t('template.form.description.placeholder') || '请输入说明'"
          :rows="4"
          :maxlength="200"
          textarea
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts" name="TemplateSave">
import { nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { device } from '@device-manager-ui/assets'
import { category } from '@device-manager-ui/api/product'
import { filterSelectNode, encodeQuery } from '@jetlinks-web-core/utils'
import { onlyMessage } from '@jetlinks-web-core/utils/comm'
import type { TemplateItem } from '../typings'
import { createTemplate, getTemplateDetail, updateTemplate } from '../../../../api/template'
import { bindTagsByTarget, getTagsByTarget, queryTagCategoryNoPaging, queryTagTree } from '../../../../api/tag'
import { I18nTextField } from '@jetlinks-web-core/components'

interface Props {
  isAdd: number
  type?: string
  title: string
}

type TagRow = {
  key: string
  categoryId?: string
  tagId?: string
  tagTree: any[]
}

const props = defineProps<Props>()
const emit = defineEmits(['success'])
const { t: $t } = useI18n()

const imageTypes = reactive(['image/jpeg', 'image/png', 'image/jfif', 'image/pjp'])
const visible = ref(false)
const formRef = ref()
const treeList = ref<Record<string, any>[]>([])
const tagCategoryOptions = ref<{ label: string; value: string }[]>([])
const tagRows = ref<TagRow[]>([])

const tagType = 'device'
const formData = reactive<Partial<TemplateItem>>({
  name: '',
  classifiedId: undefined,
  classifiedName: '',
  description: '',
  i18nMessages: {},
  document: undefined,
  photoUrl: device.deviceProduct,
})

const rules = {
  name: [
    { required: true, message: $t('template.form.name.required') || '请输入模板名称', trigger: 'blur' as const },
    { max: 64, message: $t('template.form.name.maxLength') || '名称长度不能超过64个字符', trigger: 'blur' as const },
  ],
}

const addTagRow = () => {
  tagRows.value.push({ key: `${Date.now()}-${Math.random()}`, categoryId: undefined, tagId: undefined, tagTree: [] })
}

const removeTagRow = (index: number) => {
  tagRows.value.splice(index, 1)
}

const loadTagCategories = async () => {
  const res: any = await queryTagCategoryNoPaging({
    paging: false,
    sorts: [{ name: 'sortIndex', order: 'asc' }],
    terms: [{ column: 'type', termType: 'eq', value: tagType }],
  })
  if (res.status === 200) {
    tagCategoryOptions.value = (res.result || []).map((item: any) => ({ label: item.name, value: item.id }))
  }
}

const onCategoryChange = async (row: TagRow) => {
  row.tagTree = []
  if (!row.categoryId) return
  const res: any = await queryTagTree({ terms: [{ column: 'categoryId', value: row.categoryId }] })
  if (res.status === 200) {
    row.tagTree = res.result || []
  }
}

const loadCategoryTree = async () => {
  const res = await category(encodeQuery({ sorts: { sortIndex: 'asc' } }))
  if (res.status === 200) {
    treeList.value = normalizeTreeData(res.result || [])
  }
}

function normalizeTreeData(items: Record<string, any>[] = []): Record<string, any>[] {
  return items.map(({ children, ...item }) => ({
    ...item,
    key: item.id,
    children: children?.length ? normalizeTreeData(children) : undefined,
  }))
}

const loadDetail = async (id: string) => {
  const res = await getTemplateDetail(id)
  if (res.status === 200 && res.result) {
    Object.assign(formData, res.result)
  }
}

const loadTagBind = async (id: string) => {
  const res: any = await getTagsByTarget('device-template', id)
  if (res.status === 200) {
    tagRows.value = (res.result || []).map((item: any) => ({
      key: `${Date.now()}-${Math.random()}`,
      categoryId: item.categoryId,
      tagId: item.id,
      tagTree: [],
    }))
    await Promise.all(tagRows.value.map((row) => onCategoryChange(row)))
    if (!tagRows.value.length) nextTick(() => addTagRow())
  }
}

const resetForm = () => {
  formData.id = undefined
  formData.name = ''
  formData.classifiedId = undefined
  formData.classifiedName = ''
  formData.description = ''
  formData.i18nMessages = {}
  formData.document = undefined
  formData.photoUrl = device.deviceProduct
  tagRows.value = []
  formRef.value?.clearValidate()
}

const show = async (data?: Partial<TemplateItem>) => {
  visible.value = true
  await loadCategoryTree()
  await loadTagCategories()
  if (props.isAdd === 2 && data?.id) {
    await loadDetail(data.id)
    await loadTagBind(data.id)
  } else {
    resetForm()
  }
}

const handleOk = async () => {
  await formRef.value?.validate()
  const api = props.isAdd === 1 ? createTemplate : updateTemplate
  const res: any = await api(formData)
  if (res.status === 200) {
    const templateId = formData.id || res.result?.id
    if (templateId) {
      const tagIds = tagRows.value.map((item) => item.tagId).filter(Boolean) as string[]
      await bindTagsByTarget('device-template', templateId, tagIds)
    }
    onlyMessage(props.isAdd === 1 ? ($t('template.message.createSuccess') || '新增成功') : ($t('template.message.updateSuccess') || '更新成功'), 'success')
    visible.value = false
    emit('success')
  }
}

const handleCancel = () => {
  visible.value = false
  resetForm()
}

const valueChange = (value: string, label: string[]) => {
  formData.classifiedName = label?.[0]
}

watch(visible, (v) => {
  if (!v) tagRows.value = []
})

defineExpose({ show })
</script>

<style scoped lang="less">
.tag-bind__header {
  display: flex;
  justify-content: space-between;
}
</style>
