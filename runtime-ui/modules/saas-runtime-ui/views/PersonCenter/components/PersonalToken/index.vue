<template>

  <DefineEmptyCreate>
    <a-button type="primary" @click="handleAdd">
      {{ $t('PersonalToken.index.061384-1') }}
    </a-button>
  </DefineEmptyCreate>
  <div v-bind="$attrs" class="personal-token">
    <div class="personal-token-header">
      <div style="display: flex; gap: 1.5rem; align-items: center">
        <div>
          <img src="@jetlinks-web-core/assets/personal-token/add.png" alt="" />
        </div>
        <div style="color: #1A1A1A">{{ $t('PersonalToken.index.061384-0') }}</div>
      </div>
      <EmptyCreateButton />
    </div>

    <div v-if="tokenList.length === 0" class="empty-state">
      <div class="empty-content">
        <CloudEmpty :search-name="$t('SaasManager.generated.c87894f5d8e7')">
          <EmptyCreateButton />
        </CloudEmpty>
      </div>
    </div>

    <div v-else class="token-list">
      <TokenCard
        v-for="token in tokenList"
        :key="token.id"
        :token="token"
        @view="handleView"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>

    <TokenDialog
      v-if="dialogVisible"
      :visible="dialogVisible"
      :mode="dialogMode"
      :token="selectedToken"
      @save="handleDialogOk"
      @close="dialogVisible = false"
    />

<!--    <a-modal-->
<!--      v-model:open="deleteVisible"-->
<!--      title="删除令牌"-->
<!--      @ok="confirmDelete"-->
<!--      @cancel="deleteVisible = false"-->
<!--    >-->
<!--      <p>确定要删除该令牌吗？删除后将无法恢复。</p>-->
<!--    </a-modal>-->
  </div>
</template>

<script setup>
import { createReusableTemplate } from '@vueuse/core'

import globalI18n from '@jetlinks-web-core/locales'

import { onlyMessage } from '@jetlinks-web/utils'
import TokenCard from './components/TokenCard.vue'
import TokenDialog from './components/TokenDialog.vue'
import { getCreatedPersonalTokens_api, deletePersonalToken_api } from '@jetlinks-web-core/api/account/center'
import { useRequest } from '@jetlinks-web/hooks'

const tokenList = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('add')
const selectedToken = ref(null)
const deleteVisible = ref(false)
const tokenToDelete = ref(null)

const { run, reload } = useRequest(getCreatedPersonalTokens_api, {
  defaultParams: {
    paging: false,
    sorts: [
      { name: 'createTime', order: 'desc' }
    ]
  },
  onSuccess: (res) => {
    tokenList.value = res.result || []
  }
})

const handleAdd = () => {
  selectedToken.value = null
  dialogMode.value = 'add'
  dialogVisible.value = true
}

const handleView = (token) => {
  selectedToken.value = token
  dialogMode.value = 'view'
  dialogVisible.value = true
}

const handleEdit = (token) => {
  selectedToken.value = token
  dialogMode.value = 'edit'
  dialogVisible.value = true
}

const handleDelete = async (token) => {
  const res = await deletePersonalToken_api(token.id)
  if (res.success) {
    reload()
    onlyMessage(globalI18n.global.t('Service.index.success'))
  }
}

const confirmDelete = async () => {
  // try {
  //   await deleteToken(tokenToDelete.value.id)
  //
  //   tokenList.value = tokenList.value.filter(item => item.id !== tokenToDelete.value.id)
  //   message.success('删除成功')
  //   deleteVisible.value = false
  //   tokenToDelete.value = null
  // } catch (error) {
  //   console.error('删除失败:', error)
  //   message.error('删除失败')
  // }
}

// 保留原令牌的编辑使用明确反馈；新增和轮换凭证仍沿用通用成功提示。
const handleDialogOk = async (data, revokeAccessTokens = true) => {
  reload()
  onlyMessage(globalI18n.global.t(revokeAccessTokens
    ? 'Service.index.success'
    : 'PersonalToken.TokenDialog.savedWithoutRefresh'))
  dialogVisible.value = false
  // try {
  //   if (dialogMode.value === 'add') {
  //     const response = await createToken(data)
  //     if (response.status === 200) {
  //       await loadTokenList()
  //       message.success('新增成功')
  //     }
  //   } else if (dialogMode.value === 'edit') {
  //     const response = await updateToken(selectedToken.value.id, data)
  //     if (response.status === 200) {
  //       await loadTokenList()
  //       message.success('编辑成功')
  //     }
  //   }
  //   dialogVisible.value = false
  // } catch (error) {
  //   console.error('操作失败:', error)
  //   message.error(dialogMode.value === 'add' ? '新增失败' : '编辑失败')
  // }
}


const [DefineEmptyCreate, EmptyCreateButton] = createReusableTemplate()
</script>

<style lang="less" scoped>
.personal-token {
  height: 100%;
  display: flex;
  flex-direction: column;

  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-2) var(--space-4);
    border-radius: var(--r-2);
    background: var(--jet-theme-primary-1);
    margin-bottom: var(--space-4);
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.token-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  flex: 1;
  overflow-y: auto;
  padding: var(--space-1);
}
</style>
