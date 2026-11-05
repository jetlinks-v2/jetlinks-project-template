<template>
  <div class="tree">
    <div class="header">
      <div class="title">标签</div>
      <j-permission-button type="primary" :hasPermission="'system/Tag:add'" :disabled="!categoryId" @click="openAddRoot">
        新增标签
      </j-permission-button>
    </div>

    <a-alert v-if="!categoryId" message="请先选择左侧标签分类" type="info" show-icon />

    <a-tree
      v-else
      :tree-data="treeData"
      :fieldNames="{ title: 'name', key: 'id', children: 'children' }"
      blockNode
      defaultExpandAll
    >
      <template #title="item">
        <div class="treeItem">
          <span @click="() => {}">{{ item.data.name }}</span>
          <div class="actions" @click.stop>
            <j-permission-button type="link" :hasPermission="'system/Tag:update'" @click="openEdit(item.data)">编辑</j-permission-button>
            <j-permission-button
              type="link"
              :hasPermission="'system/Tag:delete'"
              danger
              :popConfirm="{ title: '确认删除该标签？', onConfirm: () => remove(item.data.id) }"
            >
              删除
            </j-permission-button>
            <j-permission-button type="link" :hasPermission="'system/Tag:add'" @click="openAddChild(item.data)">新增子级</j-permission-button>
          </div>
        </div>
      </template>
    </a-tree>

    <TagModal ref="modalRef" @refresh="load" />
  </div>
</template>

<script setup lang="ts">
import { onlyMessage } from '@jetlinks-web/utils'
import { queryTagTree, deleteTag } from '../../../../api/tag'
import TagModal from './TagModal.vue'

const props = defineProps<{ categoryId: string }>()

const treeData = ref<any[]>([])
const modalRef = ref<any>()

const load = async () => {
  if (!props.categoryId) {
    treeData.value = []
    return
  }
  const res: any = await queryTagTree({
    terms: [{ column: 'categoryId', value: props.categoryId }],
  })
  if (res.status === 200) {
    treeData.value = res.result || []
  }
}

watch(
  () => props.categoryId,
  () => {
    load()
  },
  { immediate: true }
)

const openAddRoot = () => {
  if (!props.categoryId) return
  modalRef.value?.open?.('add', { categoryId: props.categoryId, parentId: '' })
}

const openAddChild = (parent: any) => {
  modalRef.value?.open?.('add', { categoryId: props.categoryId, parentId: parent.id })
}

const openEdit = (row: any) => {
  modalRef.value?.open?.('update', row)
}

const remove = async (id: string) => {
  const res: any = await deleteTag(id)
  if (res.status === 200) {
    onlyMessage('删除成功')
    await load()
  } else {
    onlyMessage('删除失败', 'error')
  }
}
</script>

<style lang="less" scoped>
.tree {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.treeItem {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 8px;
}

</style>
