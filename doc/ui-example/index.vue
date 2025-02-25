<script setup name="Example">
import {
  getUserList_api,
  changeUserStatus_api,
  deleteUser_api,
} from '@/api/system/user';
import { onlyMessage } from '@jetlinks-web/utils'
import { useI18n } from 'vue-i18n';
import Save from  './Edit.vue'

const { t: $t } = useI18n(); // 国际化
const permission = 'example/Auth' // 权限

/**
 * 表格列,搜素项
 * @property {boolean} hideInTable - true表示该字段只用于搜索,不在表格中显示
 * @property {object} search - 搜索配置,如果有此属性表示该字段可搜索
 */
const columns = [
  {
    title: $t('Menu.index.599742-5'),
    dataIndex: 'code',
    key: 'code',
    ellipsis: true,
    fixed: 'left',
    search: { // 搜索项
      type: 'string',
      componentProps: {
        placeholder: $t('Menu.index.599742-6'),
      },
    },
    width: 300,
  },
  {
    title: $t('Menu.index.599742-7'),
    dataIndex: 'name',
    key: 'name',
    scopedSlots: true,
    search: {
      type: 'string',
      componentProps: {
        placeholder: $t('Menu.index.599742-8'),
      },
    },
  },
  {
    title: $t('Menu.index.599742-9'),
    dataIndex: 'url',
    key: 'url',
    ellipsis: true,
    search: {
      type: 'string',
      componentProps: {
        placeholder: $t('Menu.index.599742-10'),
      },
    },
  },
  {
    title: $t('Menu.index.599742-11'),
    dataIndex: 'sortIndex',
    key: 'sortIndex',
    ellipsis: true,
    search: {
      type: 'number',
      componentProps: {
        placeholder: $t('Menu.index.599742-12'),
      },
    },
    width: 80,
  },
  {
    title: $t('Menu.index.599742-13'),
    dataIndex: 'describe',
    key: 'describe',
    ellipsis: true,
  },
  {
    title: $t('Menu.index.599742-14'),
    dataIndex: 'createTime',
    key: 'createTime',
    hideInTable: true, // 不出现在表格
    search: {
      type: 'date',
    },
    width: 200,
    scopedSlots: true,
  },
  {
    title: $t('Menu.index.599742-15'),
    dataIndex: 'action',
    key: 'action',
    fixed: 'right',
    scopedSlots: true,
    width: 140,
  },
]
const queryParams = ref({ terms: [] })
const tableRef = ref({}) // 表格实例
const handleSearch = (e) => {
  queryParams.value = e
}
const recordCache = ref()
const visible = ref(false)

const onAdd = (record) => {
  recordCache.value = record
  visible.value = true
}

const onDelete = (id) => {
  deleteUser_api(id).then(() => {
    onlyMessage($t('User.index.673867-18'));
    tableRef.value.refresh(); // 刷新表格
  });
}

const onChangeStatus = ({id, status} ) => {
  const params = {
    status: status === 0 ? 1 : 0,
    id,
  };
  changeUserStatus_api(params).then(() => {
    onlyMessage($t('User.index.673867-18'));
    tableRef.value.refresh();
  });
}

</script>

<template>
  <j-page-container>
    <!--  高级搜索  -->
    <pro-search
      target="category"
      :columns="columns"
      @search="handleSearch"
    />
    <FullPage>
      <!--  列表    -->
      <j-pro-table
        mode="TABLE"
        ref="tableRef"
        :columns="columns"
        :request="getUserList_api"
        :params="queryParams"
        :defaultParams="{
                        sorts: [
                            { name: 'createTime', order: 'desc' }
                        ],
                    }"
      >
        <template #headerLeftRender>
          <j-permission-button
            :hasPermission="`${permission}:add`"
            type="primary"
            @click="onAdd('add')"
          >
            <AIcon type="PlusOutlined" />{{ $t('User.index.673867-0') }}
          </j-permission-button>
        </template>
        <template #name="slotProps">
          {{ slotProps?.i18nName || slotProps?.name }}
        </template>
        <template #action="slotProps">
          <a-space :size="16">
            <!--     编辑       -->
            <j-permission-button
              :hasPermission="`${permission}:update`"
              type="link"
              :tooltip="{
                                    title: $t('User.index.673867-3'),
                                }"
              @click="onAdd(slotProps)"
            >
              <AIcon type="EditOutlined" />
            </j-permission-button>
            <!--    启用/禁用     -->
            <j-permission-button
              :hasPermission="`${permission}:action`"
              type="link"
              :tooltip="{
                                    title: `${
                                        slotProps.status ? $t('User.index.673867-2') : $t('User.index.673867-4')
                                    }`,
                                }"
              :popConfirm="{
                                    title: $t('User.index.673867-5', [slotProps.status ? $t('User.index.673867-2') : $t('User.index.673867-4')]),
                                    onConfirm: () =>
                                        onChangeStatus(slotProps),
                                }"
            >
              <AIcon
                :type="
                                        slotProps.status
                                            ? 'StopOutlined'
                                            : 'PlayCircleOutlined'
                                    "
              />
            </j-permission-button>
            <!--    删除        -->
            <j-permission-button
              type="link"
              :hasPermission="`${permission}:delete`"
              :tooltip="{ title: $t('Menu.index.599742-3') }"
              danger
              style="padding: 0"
              :popConfirm="{
                  title: $t('Menu.index.599742-4'),
                  onConfirm: () => onDelete(slotProps.id),
                }"
            >
              <AIcon type="DeleteOutlined" />
            </j-permission-button>
          </a-space>
        </template>
      </j-pro-table>
    </FullPage>
    <Save v-if="visible" :data="recordCache" :type="recordCache ? 'edit' : 'add'" />
  </j-page-container>
</template>

<style scoped lang="less">

</style>
