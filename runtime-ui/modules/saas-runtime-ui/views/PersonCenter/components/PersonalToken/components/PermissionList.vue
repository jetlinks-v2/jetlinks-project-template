<template>
  <div class="permission-choose-container">
    <div class="detail-controls">
      <a-input-search
          v-model:value="searchKeyword"
          allowClear
          :disabled="loading"
          :placeholder="$t('PersonalToken.PermissionList.061384-2')"
          @search="onSearch"
          style="width: 100%"
      />
    </div>
    <a-table
        :columns="columns"
        :data-source="list"
        :pagination="false"
        :loading="loading"
        row-key="id"
        :scroll="{y: 300}"
    >
      <template #headerCell="{ column }">
        <template v-if="column.key === 'actions'">
          <div style="display: flex; align-items: center; justify-content: space-between">
            <div>{{ $t('PersonalToken.PermissionList.061384-4') }}</div>
            <a-space>
              <span style="font-weight: normal">{{ $t('PersonalToken.PermissionList.061384-5', [selectedCount]) }}</span>
            </a-space>
          </div>
        </template>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span>{{ record.name }}</span>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space
              wrap
          >
            <a-tag
                v-for="item in record.checkedOptions"
                :key="item.value"
            >
              {{ item.label }}
            </a-tag>
            <span
                v-if="!record.checkedOptions.length"
                class="empty-text"
            >
              --
            </span>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, type PropType } from 'vue'
import { handleData, type PermissionDefinition, type PermissionScope } from './data'
import { queryPermission_api } from '@jetlinks-web-core/api/system/permission'
import { onlyMessage } from '@jetlinks-web/utils'
import { useI18n } from 'vue-i18n'

const { t: $t } = useI18n();

const props = defineProps({
  value: {
    type: Array as PropType<PermissionScope[]>,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  selectedInitType: {
    type: String as PropType<'custom' | 'readonly' | 'readwrite' | 'fullaccess' | ''>,
    default: ''
  },
})

const emit = defineEmits(['update:value'])

const searchKeyword = ref('')
const list = ref<ReturnType<typeof handleData>>([])
const sourceList = ref<PermissionDefinition[]>([])
const loading = ref(false)
const initialized = ref(false)

// 计算已选择的权限数量
const selectedCount = computed(() => {
  return props.value.reduce((acc, cur) => {
    return acc + cur.actions.length
  }, 0)
})

const columns = [
  {
    title: $t('PersonalToken.PermissionList.061384-8'),
    dataIndex: 'name',
    key: 'name',
    width: 150,
  },
  {
    title: $t('PersonalToken.PermissionList.061384-9'),
    dataIndex: 'actions',
    key: 'actions',
  },
]

// 首次完整查询失败后先重试初始化，避免用搜索结果替代保存所需的全量权限。
const onSearch = async (keyword: string) => {
  if (!initialized.value) {
    await handleSearch('', true)
    if (!initialized.value || !keyword) return
  }
  await handleSearch(keyword)
}

// 新增、编辑只在首次完整查询时同步全量权限；后续搜索仅改变显示结果。
const handleSearch = async (e = '', initialize = false) => {
  if (loading.value) return
  const params: { paging: boolean; terms?: { column: string; value: string }[] } = {
    paging: false,
  }
  if (e) {
    params.terms = [{ column: 'name$like', value: `%${e}%` }]
  }
  if (initialize && !props.disabled) {
    // 完整权限加载成功前禁止提交旧权限；查看模式保留原令牌的授权。
    emit('update:value', [])
  }
  loading.value = true
  try {
    const resp = await queryPermission_api(params)
    if (!resp.success) {
      onlyMessage($t('PersonalToken.PermissionList.loadFailed'), 'error')
      return
    }
    sourceList.value = resp.result || []
    if (initialize) {
      if (!props.disabled) initializePermissions(props.selectedInitType)
      initialized.value = true
    }
  } catch {
    onlyMessage($t('PersonalToken.PermissionList.loadFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// 从完整可授权列表生成待保存范围，编辑同步最新操作而非沿用原令牌快照。
const initializePermissions = (type: string) => {
  const arr: PermissionScope[] = []
  switch (type) {
    case 'custom':
      // 自定义：不预选任何权限
      break
    case 'readonly':
      // 只读：选择所有查看权限
      sourceList.value.forEach(item => {
        if(item.actions?.some(action => action.action === 'query')) {
          arr.push({
            id: item.id,
            actions: ['query'],
          })
        }
      })
      break
    case 'readwrite':
      // 读写：选择查看、编辑、启用/禁用、导入权限
      const readwriteActions = ['query', 'save', 'enable', 'disable', 'import', 'export', 'add', 'find-geo', 'save-geo', 'bind-user', 'unbind-user']
      sourceList.value.forEach(item => {
        if(item.actions?.some(action => readwriteActions.includes(action.action))) {
          arr.push({
            id: item.id,
            actions: item.actions.filter(action => readwriteActions.includes(action.action)).map(action => action.action),
          })
        }
      })
      break
    case 'fullaccess':
      // 使用接口实际返回的全部可授权操作。
      sourceList.value.forEach(item => {
        arr.push({
          id: item.id,
          actions: (item.actions || []).map(action => action.action),
        })
      })
      break
  }
  emit('update:value', arr)
}

onMounted(() => {
  handleSearch('', true)
})

watch(
    () => [props.value, sourceList.value] as const,
    ([val1, val2]) => {
      list.value = handleData(val2, val1)
    },
    {
      deep: true,
      immediate: true
    }
)
</script>

<style lang="less" scoped>
.permission-choose-container {
  .detail-controls {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  .empty-text {
    color: rgba(0, 0, 0, 0.45);
  }
}
</style>
