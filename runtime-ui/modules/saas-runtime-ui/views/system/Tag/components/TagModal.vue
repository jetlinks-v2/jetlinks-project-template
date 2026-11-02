<template>
  <a-modal
    v-if="visible"
    :title="title"
    :maskClosable="false"
    destroy-on-close
    open
    :confirmLoading="loading"
    okText="确定"
    cancelText="取消"
    @ok="submit"
    @cancel="close"
  >
    <a-form layout="vertical" ref="formRef" :model="formModel" :rules="rules">
      <a-form-item label="名称" name="name">
        <a-input v-model:value="formModel.name" :maxlength="64" placeholder="请输入名称" />
      </a-form-item>
      <a-form-item label="排序" name="sortIndex">
        <a-input-number v-model:value="formModel.sortIndex" style="width: 100%" :min="1" :max="9999" :precision="0" placeholder="请输入排序" />
      </a-form-item>
      <a-form-item label="说明" name="description">
        <a-textarea v-model:value="formModel.description" show-count :maxlength="200" placeholder="请输入说明" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts" name="TagModal">
import { onlyMessage } from '@jetlinks-web/utils'
import { saveTag } from '../../../../api/tag'

const emit = defineEmits<{ (e: 'refresh'): void }>()

const visible = ref(false)
const loading = ref(false)
const title = ref('')

const formRef = ref()

const formModel = reactive({
  id: undefined,
  name: '',
  sortIndex: 1,
  description: undefined,
  categoryId: undefined,
  parentId: undefined,
})

const rules = {
  name: [{ required: true, message: '请输入名称' }, { max: 64, message: '最多64字符' }],
  sortIndex: [{ required: true, message: '请输入排序' }],
}

const open = (type: 'add' | 'update', data: any) => {
  title.value = type === 'add' ? '新增标签' : '编辑标签'
  Object.assign(formModel, {
    id: data?.id,
    name: data?.name || '',
    sortIndex: data?.sortIndex ?? 1,
    description: data?.description || '',
    categoryId: data?.categoryId,
    parentId: data?.parentId,
  })
  visible.value = true
}

const close = () => {
  visible.value = false
}

const submit = async () => {
  try {
    await formRef.value?.validate?.()
  } catch (e) {
    return
  }
  loading.value = true
  const res: any = await saveTag({ ...formModel }).finally(() => (loading.value = false))
  if (res.status === 200) {
    onlyMessage('保存成功')
    visible.value = false
    emit('refresh')
  } else {
    onlyMessage('保存失败', 'error')
  }
}

defineExpose({ open })
</script>
