<template>
  <div class="mobile-product-save">
    <div class="page-header">
      <a-button type="link" class="back-button" @click="goBack()">
        <template #icon>
          <AIcon type="ArrowLeftOutlined" />
        </template>
        {{ tText('mobile.product.save.back', '返回') }}
      </a-button>
      <div class="page-title">{{ tText('mobile.product.save.title', '新增产品') }}</div>
      <span class="page-placeholder" />
    </div>

    <div class="form-card">
      <a-form
        ref="formRef"
        layout="vertical"
        :model="form"
        :rules="rules"
      >
<!--        <a-form-item name="photoUrl" :label="tText('mobile.product.save.photo', '产品图片')">-->
<!--          <pro-upload-->
<!--            v-model="form.photoUrl"-->
<!--            :accept="imageTypes.toString()"-->
<!--          />-->
<!--        </a-form-item>-->

        <a-form-item :label="$t('Save.index.912481-4')" name="name">
          <a-input
            v-model:value="form.name"
            :placeholder="$t('Save.index.912481-5')"
          />
        </a-form-item>

        <a-form-item :label="tText('mobile.product.save.access.label', '接入方式')" name="accessId">
          <a-select
            v-model:value="form.accessId"
            show-search
            allow-clear
            :options="accessOptions"
            :placeholder="tText('mobile.product.save.access.placeholder', '请选择接入方式')"
            :filter-option="filterAccessOption"
            @change="onAccessChange"
          />
        </a-form-item>

        <a-form-item :label="tText('mobile.product.save.storePolicy.label', '存储策略')" name="storePolicy">
          <a-select
            v-model:value="form.storePolicy"
            :options="storageOptions"
            :placeholder="tText('mobile.product.save.storePolicy.placeholder', '请选择存储策略')"
          />
        </a-form-item>

        <a-form-item :label="$t('Save.index.912481-9')" name="describe">
          <a-textarea
            v-model:value="form.describe"
            :maxlength="200"
            showCount
            :auto-size="{ minRows: 4, maxRows: 6 }"
            :placeholder="$t('Save.index.912481-10')"
          />
        </a-form-item>
      </a-form>
    </div>

    <div class="submit-bar">
      <a-button block type="primary" :loading="loading" @click="submitData">
        {{ tText('mobile.product.save.submit', '保存产品') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Rule } from 'ant-design-vue/es/form'

import { _deploy, addProduct, getStoragList, queryGatewayList } from '@device-manager-ui/api/product'
import { device } from '@device-manager-ui/assets'
import { useMenuStore } from '@jetlinks-web-core/store/menu'
import { onlyMessage } from '@jetlinks-web/utils'

const { t: $t } = useI18n()
const menuStore = useMenuStore()
const route = useRoute()

const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const getQueryValue = (value: unknown) => {
  if (Array.isArray(value)) return String(value[0] || '')
  return value ? String(value) : ''
}

const formRef = ref()
const loading = ref(false)
const accessList = ref<Record<string, any>[]>([])
const storageList = ref<Record<string, any>[]>([])
const imageTypes = reactive(['image/jpeg', 'image/png', 'image/jfif', 'image/pjp'])
const isReqSuccess = (resp: any) => !!(resp && (resp.success || resp.status === 200))

const scanToken = computed(() => getQueryValue(route.query.t))
const projectId = computed(() => getQueryValue(route.query.projectId))
const projectName = computed(() => getQueryValue(route.query.projectName))
const returnUrl = computed(() => getQueryValue(route.query.returnUrl))
const currentProductId = computed(() => getQueryValue(route.query.productId))
const accessOptions = computed(() =>
  accessList.value.map((item) => ({
    label: item.name,
    value: item.id,
  })),
)
const storageOptions = computed(() =>
  storageList.value.map((item) => ({
    label: item.name,
    value: item.id,
  })),
)

const form = reactive({
  name: '',
  accessId: undefined as string | undefined,
  accessName: '',
  accessProvider: '',
  messageProtocol: '',
  transportProtocol: '',
  protocolName: '',
  describe: undefined as string | undefined,
  photoUrl: device.deviceProduct,
  storePolicy: '',
})

const rules: Record<string, Rule[]> = {
  name: [
    { required: true, message: $t('Save.index.912481-5'), trigger: 'blur' },
    { max: 64, message: tText('mobile.product.save.name.max', '名称最多 64 个字符'), trigger: 'change' },
  ],
  accessId: [{ required: true, message: tText('mobile.product.save.access.placeholder', '请选择接入方式'), trigger: 'change' }],
  storePolicy: [{ required: true, message: tText('mobile.product.save.storePolicy.placeholder', '请选择存储策略'), trigger: 'change' }],
  describe: [{ max: 200, message: tText('mobile.product.save.describe.max', '说明最多 200 个字符'), trigger: 'change' }],
}

const buildBackQuery = (productId?: string) => {
  const query: Record<string, string> = {}
  if (scanToken.value) query.t = scanToken.value
  if (projectId.value) query.projectId = projectId.value
  if (projectName.value) query.projectName = projectName.value
  if (returnUrl.value) query.returnUrl = returnUrl.value
  if (productId) query.productId = productId
  return query
}

const filterAccessOption = (input: string, option: Record<string, any>) =>
  String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase())

const loadAccessList = async () => {
  try {
    const resp: any = await queryGatewayList({ paging: false })
    if (isReqSuccess(resp)) {
      accessList.value = resp.result || []
    }
  } catch {
    onlyMessage(tText('mobile.product.save.access.error', '接入方式加载失败'), 'error')
  }
}

const loadStorageList = async () => {
  try {
    const resp: any = await getStoragList()
    if (isReqSuccess(resp)) {
      storageList.value = resp.result || []
      form.storePolicy = form.storePolicy || storageList.value[0]?.id || 'none'
    }
  } catch {
    onlyMessage(tText('mobile.product.save.storePolicy.error', '存储策略加载失败'), 'error')
    form.storePolicy = form.storePolicy || 'none'
  }
}

const onAccessChange = (value?: string) => {
  const current = accessList.value.find((item) => String(item.id) === String(value || ''))
  form.accessId = value
  form.accessName = current?.name || ''
  form.accessProvider = current?.provider || ''
  form.messageProtocol = current?.protocol || ''
  form.transportProtocol = current?.transport || ''
  form.protocolName = current?.protocolDetail?.name || ''
}

const goBack = (productId?: string) => {
  menuStore.jumpPage('scan-product/list', {
    query: buildBackQuery(productId || currentProductId.value),
  })
}

const submitData = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true

    const resp: any = await addProduct({
      name: form.name,
      accessId: form.accessId,
      accessName: form.accessName,
      accessProvider: form.accessProvider,
      messageProtocol: form.messageProtocol,
      transportProtocol: form.transportProtocol,
      protocolName: form.protocolName,
      describe: form.describe,
      photoUrl: form.photoUrl,
      deviceType: 'device',
      storePolicy: form.storePolicy || 'none',
    })

    if (isReqSuccess(resp)) {
      const productId = String(resp?.result?.id || resp?.result || '')
      let deploySuccess = true

      if (productId) {
        try {
          const deployResp: any = await _deploy(productId)
          deploySuccess = isReqSuccess(deployResp)
        } catch {
          deploySuccess = false
        }
      }

      onlyMessage($t('Save.index.912481-22'))

      if (!deploySuccess) {
        onlyMessage(
          tText('mobile.product.save.deploy.error', '产品已创建，但默认启用失败，请稍后手动启用'),
          'warning',
        )
      }

      goBack(productId)
    } else {
      onlyMessage($t('Save.index.912481-23'), 'error')
    }
  } catch {
    // Form validation errors are handled by the form itself.
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAccessList()
  loadStorageList()
})
</script>

<style scoped lang="less">
.mobile-product-save {
  min-height: 100%;
  padding: 16px 16px 88px;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3f7 100%);
}

.page-header {
  display: grid;
  grid-template-columns: 72px 1fr 72px;
  align-items: center;
  margin-bottom: 16px;
}

.back-button {
  padding-left: 0;
}

.page-title {
  color: #111827;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}

.page-placeholder {
  display: block;
}

.form-card {
  padding: 16px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.submit-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
}
</style>
