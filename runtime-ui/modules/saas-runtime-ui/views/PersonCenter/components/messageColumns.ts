import globalI18n from '@jetlinks-web-core/locales'
import { getTypeListNew } from '@jetlinks-web-core/api/account/notificationSubscription'
import type { ConditionFilterField } from '@jetlinks-web-core/components/ConditionFilter'

export const getMessageSearchColumns = (): ConditionFilterField[] => [
  // {
  //   title: globalI18n.global.t('SaasManager.generated.152188779a97'),
  //   dataIndex: 'topicProvider',
  //   search: {
  //     type: 'select',
  //     termFilter: ['in', 'nin'],
  //     options: async () => {
  //       const resp = await getTypeListNew('')
  //       return (resp.result || []).map((item: any) => ({
  //         label: item.name,
  //         value: item.id,
  //       }))
  //     },
  //   },
  // },
  {
    title: globalI18n.global.t('SaasManager.generated.acbc5a5d362a'),
    dataIndex: 'message',
    search: { type: 'string' },
  },
  {
    title: globalI18n.global.t('SaasManager.generated.d532c64d225b'),
    dataIndex: 'notifyTime',
    search: { type: 'date' },
  },
  {
    title: globalI18n.global.t('Service.index.state'),
    dataIndex: 'state',
    search: {
      type: 'select',
      termFilter: ['in', 'nin'],
      options: [
        { label: globalI18n.global.t('SaasManager.generated.1e230aa2012d'), value: 'unread' },
        { label: globalI18n.global.t('SaasManager.generated.642ec8b59683'), value: 'read' },
      ],
    },
  },
]

export const messageTableColumns = [
  {
    title: globalI18n.global.t('SaasManager.generated.152188779a97'),
    dataIndex: 'topicProvider',
    key: 'topicProvider',
    scopedSlots: true,
    ellipsis: true,
  },
  {
    title: globalI18n.global.t('SaasManager.generated.acbc5a5d362a'),
    dataIndex: 'message',
    key: 'message',
    ellipsis: true,
  },
  {
    title: globalI18n.global.t('SaasManager.generated.d532c64d225b'),
    dataIndex: 'notifyTime',
    key: 'notifyTime',
    scopedSlots: true,
    ellipsis: true,
  },
  {
    title: globalI18n.global.t('Service.index.state'),
    dataIndex: 'state',
    key: 'state',
    scopedSlots: true,
    ellipsis: true,
  },
  {
    title: globalI18n.global.t('Service.index.action'),
    dataIndex: 'action',
    key: 'action',
    scopedSlots: true,
    width: '8rem',
  },
]
