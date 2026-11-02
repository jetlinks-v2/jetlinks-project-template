import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Form } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import { onlyMessage } from '@jetlinks-web/utils'
import { useTabSaveSuccess, useTabSaveSuccessBack } from '@jetlinks-web-core/hooks'
import {
  getChildConfigView,
  getConfigView,
  getNetworkComponentList,
  getNetworkList,
  getProtocolList,
  save,
  update,
} from '@device-manager-ui/api/link/accessConfig'
import {
  ColumnsHTTP,
  ColumnsMQTT,
  descriptionList,
  NetworkTypeMapping,
  ProtocolMapping,
} from '@device-manager-ui/views/link/AccessConfig/data'

type Translate = (key: string, ...args: any[]) => string
type RuntimeAccessNetworkProps = {
  provider: Record<string, any>
  data: Record<string, any>
  bindProduct: boolean
}

function uuid() {
  return Math.random().toString(16).slice(2)
}

export function useRuntimeAccessNetwork(props: RuntimeAccessNetworkProps, $t: Translate) {
  const route = useRoute()
  const id = route.params.id as string
  const view = route.query.view as string
  const type = props.provider.channel
  const isAgent = ['agent-device-gateway', 'agent-media-device-gateway'].includes(props.provider.id)

  const current = ref(0)
  const stepCurrent = ref(0)
  const steps = ref<string[]>([])
  const networkList = ref<any[]>([])
  const allNetworkList = ref<any[]>([])
  const protocolList = ref<any[]>([])
  const allProtocolList = ref<any[]>([])
  const networkCurrent = ref('')
  const protocolCurrent = ref('')
  const config = ref<Record<string, any>>({})
  const columnsMQTT = ref<TableColumnType[]>([])
  const columnsHTTP = ref<TableColumnType[]>([])
  const loading = ref(false)
  const formData = reactive({
    name: '',
    description: '',
    bizKey: '',
    i18nMessages: {} as Record<string, Record<string, string>>,
  })

  const { resetFields, validate, validateInfos } = Form.useForm(
    formData,
    reactive({
      name: [
        { required: true, message: $t('Network.index.041705-9'), trigger: 'blur' },
        { max: 64, message: $t('Network.index.041705-20'), trigger: 'blur' },
      ],
      bizKey: [{ max: 128, message: $t('saasRuntime.accessConfig.gatewayBizKeyMaxLength'), trigger: 'blur' }],
      description: [{ max: 200, message: $t('Network.index.041705-21') }],
    }),
  )

  const { onOpen: onTypeOpen } = useTabSaveSuccess('link/Type/Detail', {
    onSuccess(value) {
      if (value.success) {
        networkCurrent.value = value.result.id
        void queryNetworkList(props.provider?.id, networkCurrent.value || '')
      }
    },
  })

  const { onOpen: onProtocolOpen } = useTabSaveSuccess('link/Protocol', {
    onSuccess(value) {
      if (value.success) {
        protocolCurrent.value = value.result?.id
        void queryProtocolList(props.provider?.id)
      }
    },
  })
  const { onBack } = useTabSaveSuccessBack()

  const showAddBtn = computed(() => route.query.view === 'false' && !props.bindProduct)
  const clientHeight = document.body.clientHeight
  const accessData = computed(() => props.data || {})
  const currentProtocol = computed(() => protocolList.value.find((item) => item.id === protocolCurrent.value) || {})
  const getNetworkCurrent = computed(() => {
    return networkList.value.find((item) => item.id === networkCurrent.value)?.addresses || []
  })
  const transport = computed(() => {
    if (props.provider?.id === 'child-device') return 'Gateway'
    if (isAgent) {
      const network = allNetworkList.value.find((item) => item.id === networkCurrent.value)
      if (network?.type === 'HTTP_SERVER') return 'HTTP'
      if (network?.type === 'MQTT_SERVER') return 'MQTT'
    }
    return ProtocolMapping.get(props.provider.id)
  })

  // 本地过滤与空态共用已生效关键词，输入草稿不影响空态。
  const networkKeyword = ref('')
  const protocolKeyword = ref('')

  async function queryNetworkList(idValue: string, include: string, data = {}) {
    const networkType = NetworkTypeMapping.get(idValue)
    const resp = networkType instanceof Array
      ? await getNetworkComponentList({ networkTypes: networkType }, include)
      : await getNetworkList(networkType, include, data)
    if (resp.status === 200) {
      networkList.value = resp.result
      allNetworkList.value = resp.result
      networkKeyword.value = ''
    }
  }

  async function queryProtocolList(idValue: string, params = {}) {
    const resp: any = await getProtocolList(ProtocolMapping.get(idValue), {
      ...params,
      'sorts[0].name': 'createTime',
      'sorts[0].order': 'desc',
    })
    if (resp.status === 200) {
      protocolList.value = resp.result
      allProtocolList.value = resp.result
      protocolKeyword.value = ''
    }
  }

  function addNetwork() {
    onTypeOpen({ type: NetworkTypeMapping.get(props.provider?.id) || '' })
  }

  function addProtocol() {
    onProtocolOpen({ save: true })
  }

  function checkedChange(value: string) {
    networkCurrent.value = value
  }

  function protocolChange(value: string) {
    if (!props.data.id) protocolCurrent.value = value
  }

  function networkSearch(value: string) {
    const keyword = value.trim().toLowerCase()
    networkKeyword.value = keyword
    networkList.value = keyword
      ? allNetworkList.value.filter((item) => item.name?.toLowerCase().includes(keyword))
      : allNetworkList.value
  }

  function protocolSearch(value: string) {
    const keyword = value.trim().toLowerCase()
    protocolKeyword.value = keyword
    protocolList.value = keyword
      ? allProtocolList.value.filter((item) => item.name?.toLowerCase().includes(keyword))
      : allProtocolList.value
  }

  function getColor(item: any) {
    return item.health === -1 ? 'error' : 'processing'
  }

  function getStream(record: any) {
    if (record.upstream && record.downstream) return $t('Network.index.041705-22')
    if (record.upstream) return $t('Network.index.041705-23')
    if (record.downstream) return $t('Network.index.041705-24')
    return ''
  }

  function addressesTip(data: any[] = []) {
    return data.map((item) => item.address).join(' ')
  }

  async function saveData() {
    try {
      const values = await validate()
      const params = {
        ...props.data,
        ...values,
        i18nMessages: formData.i18nMessages,
        protocol: protocolCurrent.value,
        channel: 'network',
        channelId: networkCurrent.value,
        provider: props.provider.id,
        transport: transport.value,
      }
      if (route.query.provider) {
        onBack({
          ...params,
          channelInfo: networkList.value.find((item) => item.id === networkCurrent.value),
          protocolDetail: protocolList.value.find((item) => item.id === protocolCurrent.value),
        })
        return
      }
      loading.value = true
      const resp = id === ':id' ? await save(params) : await update({ ...params, id })
      loading.value = false
      if (resp.status === 200) {
        onlyMessage($t('Network.index.041705-25'), 'success')
        history.back()
        onBack(resp)
      }
    } catch {
      loading.value = false
    }
  }

  async function next() {
    if (current.value === 0) {
      if (!networkCurrent.value) {
        onlyMessage($t('Network.index.041705-26'), 'error')
      } else if (isAgent) {
        current.value = 2
        protocolCurrent.value = props.provider.id
      } else {
        await queryProtocolList(props.provider.id)
        current.value += 1
      }
      return
    }
    if (current.value !== 1) return
    if (!protocolCurrent.value) {
      onlyMessage($t('Network.index.041705-27'), 'error')
      return
    }
    const resp = type !== 'child-device'
      ? await getConfigView(protocolCurrent.value, ProtocolMapping.get(props.provider.id))
      : await getChildConfigView(protocolCurrent.value)
    if (resp.status === 200) {
      config.value = resp.result || {}
      current.value += 1
      const groupColumn = buildGroupColumn()
      columnsMQTT.value = [groupColumn, ...ColumnsMQTT] as TableColumnType[]
      columnsHTTP.value = [groupColumn, ...ColumnsHTTP] as TableColumnType[]
    }
  }

  function buildGroupColumn() {
    return {
      title: $t('Network.index.041705-28'),
      dataIndex: 'group',
      key: 'group',
      ellipsis: true,
      align: 'center',
      width: 100,
      customCell: (record: any, rowIndex: number) => {
        const list = config.value?.routes || []
        const arr = list.filter((res: any) => res.group === record.group)
        const isRowIndex = rowIndex === 0 || list[rowIndex - 1].group !== record.group
        return { children: record, rowSpan: isRowIndex ? arr.length : 0 }
      },
    }
  }

  function prev() {
    current.value = isAgent ? 0 : current.value - 1
  }

  function initSteps() {
    if (isAgent) {
      steps.value = [$t('Network.index.041705-13'), $t('Network.index.041705-19')]
      current.value = 0
      return
    }
    steps.value = type !== 'child-device'
      ? [$t('Network.index.041705-13'), $t('Network.index.041705-14'), $t('Network.index.041705-19')]
      : [$t('Network.index.041705-14'), $t('Network.index.041705-19')]
    current.value = type !== 'child-device' ? 0 : 1
  }

  onMounted(() => {
    initSteps()
    if (props.data?.id) {
      networkCurrent.value = props.data.channelId
      protocolCurrent.value = props.data.protocol
      formData.name = props.data.name
      formData.description = props.data.description
      formData.bizKey = props.data.bizKey || ''
      formData.i18nMessages = props.data.i18nMessages || {}
      if (props.data.provider !== 'child-device') void queryNetworkList(props.provider.id, networkCurrent.value)
      else void queryProtocolList(props.provider.id)
      return
    }
    if (!props.provider?.id) return
    if (type !== 'child-device') void queryNetworkList(props.provider.id, '')
    else void queryProtocolList(props.provider.id)
  })

  watch(current, (value) => {
    stepCurrent.value = type === 'child-device' ? value - 1 : value
  }, { immediate: true })

  return {
    accessData,
    addNetwork,
    addProtocol,
    addressesTip,
    checkedChange,
    clientHeight,
    columnsHTTP,
    columnsMQTT,
    config,
    current,
    currentProtocol,
    descriptionList,
    formData,
    getColor,
    getNetworkCurrent,
    getStream,
    id,
    isAgent,
    loading,
    networkCurrent,
    networkList,
    networkSearch,
    networkKeyword,
    next,
    prev,
    protocolChange,
    protocolCurrent,
    protocolList,
    protocolSearch,
    protocolKeyword,
    resetFields,
    saveData,
    showAddBtn,
    stepCurrent,
    steps,
    type,
    uuid,
    validateInfos,
    view,
  }
}
