import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { _deploy as deployDevice } from '@device-manager-ui/api/instance'
import {
  _deploy as deployProduct,
  addProduct,
  getStoragList,
  queryList as queryGatewayDetailList,
  queryProductList,
  saveProductMetadata,
} from '@device-manager-ui/api/product'
import { device } from '@device-manager-ui/assets'
import { onlyMessage } from '@jetlinks-web/utils'
import { registerDevice } from '../../../../api/device'
import type { RegisterDeviceResponse } from '../../../../api/device'

const AGENT_ACCESS_PROVIDER = 'agent-media-device-gateway'
const ACCESS_UNAVAILABLE_MESSAGE = '当前项目暂未开通网关接入能力，请联系管理员'
const MIN_LOADING_DURATION = 3000
const DEFAULT_STORE_POLICY = 'none'

interface ProductItem {
  id?: string
  name?: string
  state?: unknown
  metadata?: string
  accessId?: string
  accessName?: string
  accessProvider?: string
  messageProtocol?: string
  transportProtocol?: string
  protocolName?: string
}

interface GatewayItem {
  id?: string
  name?: string
  provider?: string
  protocol?: string
  transport?: string
  protocolDetail?: {
    name?: string
  }
  transportDetail?: {
    metadata?: string | Record<string, unknown>
  }
}

interface StoragePolicyItem {
  id?: string
}

const getQueryValue = (value: unknown) => {
  return Array.isArray(value) ? String(value[0] || '').trim() : String(value || '').trim()
}

const isReqSuccess = (resp: any) => {
  return !!(resp && (resp.success || resp.status === 200))
}

const unwrapList = (resp: any): any[] => {
  const result = resp?.result
  if (Array.isArray(result)) {
    return result
  }
  if (Array.isArray(result?.data)) {
    return result.data
  }
  return []
}

const resolveResultId = (result: any) => {
  if (!result) {
    return ''
  }
  if (Array.isArray(result)) {
    return resolveResultId(result[0])
  }
  if (typeof result === 'string' || typeof result === 'number') {
    return String(result)
  }
  return String(result.id || '')
}

const firstText = (...values: unknown[]) => {
  for (const value of values) {
    const text = String(value || '').trim()
    if (text) {
      return text
    }
  }
  return ''
}

const resolveStorePolicy = (policies: StoragePolicyItem[]) => {
  // 后端会把 none 放在列表首位，扫码自动建产品时优先使用真实存储策略。
  return firstText(
    policies.find((item) => firstText(item?.id) && firstText(item.id) !== DEFAULT_STORE_POLICY)?.id,
    DEFAULT_STORE_POLICY,
  )
}

const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

const waitMinimumLoading = async (startedAt: number) => {
  const remain = MIN_LOADING_DURATION - (Date.now() - startedAt)
  if (remain > 0) {
    await sleep(remain)
  }
}

const isEnabledState = (state: unknown) => {
  if (state === 1 || state === '1' || state === true || state === 'true' || state === 'enabled') {
    return true
  }
  if (state && typeof state === 'object') {
    const value = (state as { value?: unknown }).value
    return value === 1 || value === '1' || value === true || value === 'true' || value === 'enabled'
  }
  return false
}

export const useScanProductAccess = () => {
  const route = useRoute()

  const formRef = ref()
  const preparing = ref(false)
  const prepared = ref(false)
  const submitting = ref(false)
  const selectedProduct = ref<ProductItem>()
  const createdDevice = ref<{ name: string }>()
  const initError = ref('')
  const submitError = ref('')

  const scanToken = computed(() => getQueryValue(route.query.t) || getQueryValue(route.query.scanToken))
  const requestProjectName = computed(() => getQueryValue(route.query.projectName))
  const projectName = computed(() => requestProjectName.value || '当前项目')
  const assetModelText = computed(() => getQueryValue(route.query.assetModel) || '--')
  const assetSerialText = computed(() => getQueryValue(route.query.serialNumber) || '--')
  const hasAssetSummary = computed(() => assetModelText.value !== '--' || assetSerialText.value !== '--')
  const headerSubtitle = computed(() => {
    if (createdDevice.value) return '网关接入完成'
    if (initError.value) return '接入异常'
    if (preparing.value) return '正在初始化…'
    return '完善网关信息'
  })
  const canSubmit = computed(() => prepared.value && !submitting.value && !!selectedProduct.value?.id)

  const deviceForm = reactive({
    name: '',
    description: '',
  })

  const rules = {
    name: [
      {
        validator: async (_rule: unknown, value: string) => {
          if (!String(value || '').trim()) {
            return Promise.reject('请输入名称')
          }
          return Promise.resolve()
        },
        trigger: 'blur',
      },
      { max: 64, message: '名称最多 64 个字符', trigger: 'change' },
    ],
    description: [
      { max: 200, message: '备注最多 200 个字符', trigger: 'change' },
    ],
  }

  const buildProductName = () => {
    const name = `${projectName.value}测试接入网关`
    return name.length > 64 ? name.slice(0, 64) : name
  }

  const loadLatestProduct = async () => {
    const resp: any = await queryProductList({
      pageIndex: 0,
      pageSize: 1,
      terms: [
        {
          terms: [
            {
              column: 'accessProvider',
              termType: 'eq',
              value: AGENT_ACCESS_PROVIDER,
            },
          ],
        },
      ],
      sorts: [{ name: 'createTime', order: 'desc' }],
    })
    if (!isReqSuccess(resp)) {
      return undefined
    }
    return unwrapList(resp)[0] as ProductItem | undefined
  }

  const loadLatestGateway = async () => {
    const resp: any = await queryGatewayDetailList({
      paging: false,
      terms: [
        {
          terms: [
            {
              column: 'provider',
              termType: 'eq',
              value: AGENT_ACCESS_PROVIDER,
            },
          ],
        },
      ],
      sorts: [{ name: 'createTime', order: 'desc' }],
    })
    if (!isReqSuccess(resp)) {
      return undefined
    }
    return unwrapList(resp).find((item) => item?.provider === AGENT_ACCESS_PROVIDER) as GatewayItem | undefined
  }

  const loadStorePolicy = async () => {
    try {
      const resp: any = await getStoragList()
      if (isReqSuccess(resp) && Array.isArray(resp.result)) {
        return resolveStorePolicy(resp.result)
      }
    } catch (error) {
      console.warn('Failed to load product storage policy:', error)
    }
    return DEFAULT_STORE_POLICY
  }

  const resolveGatewayMetadata = (gateway: GatewayItem) => {
    const source = gateway.transportDetail?.metadata
    if (!source) {
      return ''
    }
    try {
      const metadata = typeof source === 'string' ? JSON.parse(source) : source
      const productMetadata = ['functions', 'properties', 'events', 'tags'].reduce(
        (record, key) => {
          const value = (metadata as Record<string, unknown>)?.[key]
          if (Array.isArray(value)) {
            record[key] = value
          }
          return record
        },
        {} as Record<string, unknown[]>,
      )
      return Object.keys(productMetadata).length ? JSON.stringify(productMetadata) : ''
    } catch (error) {
      console.warn('Failed to parse access metadata:', error)
      return ''
    }
  }

  const createProduct = async (name: string) => {
    const resp: any = await addProduct({
      name,
      photoUrl: device.deviceProduct,
      deviceType: 'gateway',
      classifiedName: '',
    })
    if (!isReqSuccess(resp)) {
      throw new Error('接入信息初始化失败，请联系管理员')
    }

    const productId = resolveResultId(resp.result)
    if (!productId) {
      throw new Error('接入信息初始化失败，请联系管理员')
    }
    return productId
  }

  const saveProductAccess = async (productId: string, name: string, gateway: GatewayItem, storePolicy: string) => {
    const metadata = resolveGatewayMetadata(gateway)
    if (!metadata) {
      throw new Error('接入信息初始化失败，请联系管理员')
    }

    const resp: any = await saveProductMetadata({
      id: productId,
      name,
      photoUrl: device.deviceProduct,
      classifiedName: '',
      deviceType: 'gateway',
      configMetadatas: [],
      features: [],
      configuration: {},
      metadata,
      transportProtocol: firstText(gateway.transport, 'MQTT'),
      protocolName: firstText(gateway.protocolDetail?.name, gateway.name),
      accessId: gateway.id,
      accessName: gateway.name,
      accessProvider: firstText(gateway.provider, AGENT_ACCESS_PROVIDER),
      messageProtocol: firstText(gateway.protocol, gateway.provider, AGENT_ACCESS_PROVIDER),
      describe: `${projectName.value}项目测试扫码接入`,
      storePolicy,
    })

    if (!isReqSuccess(resp)) {
      throw new Error('接入信息初始化失败，请联系管理员')
    }
  }

  const deploySavedProduct = async (productId: string) => {
    const deployResp: any = await deployProduct(productId)
    if (!isReqSuccess(deployResp)) {
      throw new Error('接入信息初始化失败，请联系管理员')
    }
  }

  const createAndDeployProduct = async (gateway: GatewayItem) => {
    const name = buildProductName()
    const storePolicy = await loadStorePolicy()
    const productId = await createProduct(name)

    await saveProductAccess(productId, name, gateway, storePolicy)
    await deploySavedProduct(productId)

    return {
      id: productId,
      name,
      state: 1,
      accessId: gateway.id,
      accessName: gateway.name,
      accessProvider: firstText(gateway.provider, AGENT_ACCESS_PROVIDER),
    }
  }

  const initAccess = async () => {
    if (preparing.value) {
      return
    }

    const startedAt = Date.now()
    preparing.value = true
    prepared.value = false
    selectedProduct.value = undefined
    initError.value = ''
    submitError.value = ''

    try {
      if (!scanToken.value) {
        initError.value = '缺少扫码参数，无法继续接入'
        return
      }

      const product = await loadLatestProduct()
      if (product?.id) {
        if (!isEnabledState(product.state)) {
          initError.value = ACCESS_UNAVAILABLE_MESSAGE
          return
        }
        selectedProduct.value = product
        prepared.value = true
        return
      }

      const gateway = await loadLatestGateway()
      if (!gateway?.id) {
        initError.value = ACCESS_UNAVAILABLE_MESSAGE
        return
      }

      selectedProduct.value = await createAndDeployProduct(gateway)
      prepared.value = true
    } catch (error: any) {
      console.error('Failed to initialize scan access:', error)
      initError.value = error?.message || '接入信息初始化失败，请联系管理员'
    } finally {
      await waitMinimumLoading(startedAt)
      preparing.value = false
    }
  }

  const handleConfirm = async () => {
    if (submitting.value) {
      return
    }

    try {
      await formRef.value?.validate()
    } catch {
      return
    }

    if (!selectedProduct.value?.id) {
      submitError.value = '接入信息未初始化完成，请稍后重试'
      return
    }

    deviceForm.name = deviceForm.name.trim()
    deviceForm.description = deviceForm.description.trim()
    submitting.value = true
    submitError.value = ''

    try {
      const resp: RegisterDeviceResponse = await registerDevice({
        scanToken: scanToken.value,
        productId: selectedProduct.value.id,
        device: {
          name: deviceForm.name,
          description: deviceForm.description,
        },
      })

      if (!isReqSuccess(resp)) {
        submitError.value = resp?.message || '网关接入失败，请稍后重试'
        return
      }

      const deviceId = resolveResultId(resp.result)
      if (deviceId) {
        const deployResp: any = await deployDevice(deviceId)
        if (!isReqSuccess(deployResp)) {
          onlyMessage('网关已创建，启用失败，请稍后手动启用', 'warning')
        }
      }

      createdDevice.value = { name: deviceForm.name }
    } catch (error: any) {
      console.error('Failed to register scan device:', error)
      submitError.value = error?.message || '网关接入失败，请稍后重试'
    } finally {
      submitting.value = false
    }
  }

  const handleInputFocus = (event: FocusEvent) => {
    const target = event.target as HTMLElement | null
    if (!target) return
    // 移动端虚拟键盘弹起后，主动把焦点元素滚动到可视区中部，避免被固定底部 CTA 遮挡
    window.setTimeout(() => {
      target.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }, 220)
  }

  onMounted(() => {
    void initAccess()
  })

  return {
    assetModelText,
    assetSerialText,
    canSubmit,
    createdDevice,
    deviceForm,
    formRef,
    handleConfirm,
    handleInputFocus,
    hasAssetSummary,
    headerSubtitle,
    initAccess,
    initError,
    prepared,
    preparing,
    projectName,
    rules,
    submitError,
    submitting,
  }
}
