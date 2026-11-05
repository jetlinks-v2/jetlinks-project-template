<template>
  <div class="template-document">
    <div class="template-document__toolbar">
      <div />
      <a-space v-if="!editing">
        <j-permission-button type="primary" hasPermission="device/Template:update" @click="startEdit">
          <template #icon><AIcon type="EditOutlined" /></template>
          {{ $t('template.document.edit') || '编辑文档' }}
        </j-permission-button>
      </a-space>
      <a-space v-else>
        <a-button @click="cancelEdit">{{ $t('template.document.cancel') || '取消' }}</a-button>
        <a-button type="primary" :loading="saving" :disabled="!documentDirty" @click="saveDocument">
          {{ $t('template.document.save') || '保存' }}
        </a-button>
      </a-space>
    </div>

    <MarkdownEditor
      v-if="editing"
      v-model="documentDraft"
      :rows="20"
      :placeholder="$t('template.document.placeholder') || '请输入设备接入文档，支持 Markdown 格式'"
    />

    <MarkdownEditor
      v-else
      :model-value="currentDocument"
      :rows="20"
      :disabled="true"
      :empty-description="$t('template.document.empty') || '暂无文档内容，点击编辑添加'"
    />
  </div>
</template>

<script setup lang="ts" name="TemplateDocument">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import MarkdownEditor from '@jetlinks-web-core/components/MarkdownEditor'
import { onlyMessage } from '@jetlinks-web-core/utils/comm'
import { useTemplateStore } from '../../../../../store/template'
import { getTemplateDetail, updateTemplateDocument } from '../../../../../api/template'

const { t: $t } = useI18n()
const route = useRoute()
const templateStore = useTemplateStore()

const editing = ref(false)
const saving = ref(false)
const currentDocument = ref('')
const documentDraft = ref('')
const documentDirty = computed(() => documentDraft.value !== currentDocument.value)

const loadDocument = async () => {
  const id = route.params.id as string
  if (!id) return
  const res = await getTemplateDetail(id)
  if (res.status === 200 && res.result) {
    currentDocument.value = res.result.document || ''
  }
}

const startEdit = () => {
  documentDraft.value = currentDocument.value
  editing.value = true
}

const cancelEdit = () => {
  documentDraft.value = ''
  editing.value = false
}

const saveDocument = async () => {
  const id = route.params.id as string
  if (!id) return
  saving.value = true
  try {
    await updateTemplateDocument(id, documentDraft.value)
    currentDocument.value = documentDraft.value
    editing.value = false
    templateStore.refresh(id)
    onlyMessage($t('template.message.success') || '操作成功', 'success')
  } catch {
    onlyMessage($t('template.message.failed') || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(loadDocument)
</script>

<style scoped lang="less">
.template-document__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
