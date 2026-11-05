<template>
  <div class="category">
    <a-input v-model:value="searchValue" placeholder="搜索分类" @pressEnter="load" @change="onSearchChange">
      <template #suffix>
        <AIcon type="SearchOutlined" @click="load" />
      </template>
    </a-input>

    <div class="controls">
      <j-permission-button block type="primary" :hasPermission="'system/Tag:add'" @click="openAdd">
        新增分类
      </j-permission-button>
    </div>

    <div class="listBox">
      <a-list v-if="listData.length" :data-source="listData" :split="false" size="small">
        <template #renderItem="{ item }">
          <a-list-item class="listItem" :class="{ active: item.id === selectedId }" @click="select(item.id)">
            <div class="itemText">
              <j-ellipsis>{{ item.name }}</j-ellipsis>
            </div>
            <div class="actions" @click.stop>
              <j-permission-button type="link" :hasPermission="'system/Tag:update'" @click="openEdit(item)">
                编辑
              </j-permission-button>
              <j-permission-button
                type="link"
                danger
                :hasPermission="'system/Tag:delete'"
                :popConfirm="{ title: '确认删除该分类？', onConfirm: () => remove(item.id) }"
              >
                删除
              </j-permission-button>
            </div>
          </a-list-item>
        </template>
      </a-list>
      <CloudEmpty v-else style="margin-top: 100px" />
    </div>

    <CategoryModal ref="modalRef" @refresh="load" />
  </div>
</template>

<script setup lang="ts">
import { onlyMessage } from '@jetlinks-web/utils'
import { queryTagCategoryNoPaging, deleteTagCategory } from '../../../../api/tag'
import CategoryModal from './CategoryModal.vue'

const emit = defineEmits<{
  (e: 'change', id: string): void
}>()

const searchValue = ref<string>('')
const listData = ref<any[]>([])
const selectedId = ref<string>('')

const modalRef = ref<any>()

const load = async () => {
  const terms = searchValue.value
    ? [{ terms: [{ column: 'name', termType: 'like', value: `%${searchValue.value}%` }] }]
    : undefined

  const res: any = await queryTagCategoryNoPaging({
    paging: false,
    terms,
    sorts: [{ name: 'sortIndex', order: 'asc' }],
  })

  if (res.status === 200) {
    listData.value = res.result || []
    if (!selectedId.value && listData.value.length) {
      select(listData.value[0].id)
    }
  }
}

const onSearchChange = () => {
  if (!searchValue.value) load()
}

const select = (id: string) => {
  selectedId.value = id
  emit('change', id)
}

const openAdd = () => {
  modalRef.value?.open?.('add')
}

const openEdit = (row: any) => {
  modalRef.value?.open?.('update', row)
}

const remove = async (id: string) => {
  const res: any = await deleteTagCategory(id)
  if (res.status === 200) {
    onlyMessage('删除成功')
    if (selectedId.value === id) {
      selectedId.value = ''
      emit('change', '')
    }
    await load()
  } else {
    onlyMessage('删除失败', 'error')
  }
}

onMounted(() => load())
</script>

<style lang="less" scoped>
.category {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.controls {
  width: 100%;
}

.listBox {
  flex: 1;
  overflow: auto;
}

.listItem {
  cursor: pointer;
  padding: 8px 0;

  &.active {
    background: #f5f5f5;
  }

  .itemText {
    flex: 1;
    min-width: 0;
    padding-right: 8px;
  }
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
