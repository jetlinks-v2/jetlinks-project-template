<template>

  <DefineEmptyCreate>
    <j-permission-button
      v-if="!data?.id"
      class="create-product-btn"
      hasPermission="device/Product:add"
      @click="openCreateProduct"
    >
      <template #icon><AIcon type="PlusOutlined" /></template>
      创建产品
    </j-permission-button>
  </DefineEmptyCreate>
  <a-modal v-bind="$attrs"
    :maskClosable="false"
    width="650px"
    :open="true"
    :title="!!data?.id ? $t('Save.index.902471-0') : $t('Save.index.902471-1')"
    @ok="handleSave"
    @cancel="handleCancel"
    :confirmLoading="loading"
  >
    <div style="margin-top: 10px">
      <a-form
        layout="vertical"
        ref="formRef"
        :model="modelRef"
      >
        <a-row type="flex">
          <a-col flex="180px">
            <a-form-item name="photoUrl">
              <ProUpload
                accept="image/jpeg,image/png"
                v-model="modelRef.photoUrl"
              />
            </a-form-item>
          </a-col>
          <a-col flex="auto">
            <a-form-item
              name="id"
              :rules="[
                {
                  pattern: /^[a-zA-Z0-9_\-]+$/,
                  message: $t('Save.index.902471-2')
                },
                {
                  max: 64,
                  message: $t('Save.index.902471-3')
                },
                {
                  validator: vailId,
                  trigger: 'blur'
                }
              ]"
            >
              <template #label>
                <span>
                  ID
                  <a-tooltip :title="$t('Save.index.902471-4')">
                    <AIcon
                      type="QuestionCircleOutlined"
                      style="margin-left: 2px"
                    />
                  </a-tooltip>
                </span>
              </template>
              <a-input
                v-model:value="modelRef.id"
                :placeholder="$t('Save.index.902471-5')"
                :disabled="!!data?.id || !showId"
              />
            </a-form-item>
            <a-form-item
              :label="$t('Save.index.902471-6')"
              name="name"
              :rules="[
                {
                  required: true,
                  message: $t('Save.index.902471-7')
                },
                {
                  max: 64,
                  message: $t('Save.index.902471-3')
                }
              ]"
            >
              <I18nTextField
                v-model:value="modelRef.name"
                v-model:i18nMessages="modelRef.i18nMessages"
                field="name"
                :label="$t('Save.index.902471-6')"
                :i18n-max-length="64"
                :placeholder="$t('Save.index.902471-7')"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          name="productId"
          :rules="[
            {
              required: true,
              message: $t('Save.index.902471-8')
            }
          ]"
        >
          <template #label>
            <span>
              {{ $t('Save.index.902471-9') }}
              <a-tooltip :title="$t('Save.index.902471-10')">
                <AIcon
                  type="QuestionCircleOutlined"
                  style="margin-left: 2px"
                />
              </a-tooltip>
            </span>
          </template>
          <div class="product-select-with-create">
            <a-select
              class="product-select"
              showSearch
              v-model:value="modelRef.productId"
              :disabled="!!data?.id"
              :placeholder="$t('Save.index.902471-11')"
              option-filter-prop="label"
              @change="onChange"
            >
              <a-select-option
                :value="item.id"
                v-for="item in productList"
                :key="item.id"
                :label="item.name"
              >
                {{ item.name }}
              </a-select-option>
            </a-select>
            <EmptyCreateButton v-if="!data?.id" />
          </div>
          <CloudEmpty v-if="!data?.id && !productList.length" class="product-empty-tip" :search-name="$t('dialogs.ProductChooseDialog.926510-1')"><template #description>
            暂无已启用产品，请先创建并启用产品
          </template>
            <EmptyCreateButton />
          </CloudEmpty>
        </a-form-item>
        <RegistryComponent
          code="instanceSave"
          v-model:value="modelRef.configuration.type"
          :productId="modelRef.productId"
          :masterProductId="modelRef.masterProductId"
          :type="modelRef.configuration.type"
          :disabled="!!data?.id"
        >
        </RegistryComponent>
        <a-form-item
          :label="$t('Save.index.902471-12')"
          name="describe"
          :rules="[
            {
              max: 200,
              message: $t('Save.index.902471-13')
            }
          ]"
        >
          <I18nTextField
            v-model:value="modelRef.describe"
            v-model:i18nMessages="modelRef.i18nMessages"
            field="describe"
            :label="$t('Save.index.902471-12')"
            :placeholder="$t('Save.index.902471-14')"
            showCount
            :maxlength="200"
            textarea
          />
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { createReusableTemplate } from '@vueuse/core'

import { queryNoPagingPost } from '@device-manager-ui/api/product'
import { isExists, update } from '@device-manager-ui/api/instance'
import { onlyMessage } from '@jetlinks-web/utils'
import { device } from '@device-manager-ui/assets'
import { useI18n } from 'vue-i18n'
import { isInput } from '@device-manager-ui/utils/utils'
import { moduleRegistry } from '@jetlinks-web-core/utils/module-registry'
import { deviceCloudSave } from '@device-manager-ui/api/instance'
import { ensureVisualizationDashboardProject } from '@device-manager-ui/utils/dashboardProject'
import {isSaaS} from "@jetlinks-web-core/utils/consts";
import { useTabSaveSuccess } from '@jetlinks-web-core/hooks'
import Driver from 'driver.js'
import 'driver.js/dist/driver.min.css'
import { I18nTextField } from '@jetlinks-web-core/components'

const { t: $t } = useI18n()

const emit = defineEmits(['close', 'save'])
const props = defineProps({
  data: {
    type: Object,
    default: undefined
  },
  showId: {
    type: Boolean,
    default: true
  }
})
const productList = ref<Record<string, any>[]>([])
const loading = ref<boolean>(false)

const formRef = ref()

const modelRef = reactive({
  productId: undefined,
  id: undefined,
  name: '',
  describe: '',
  i18nMessages: {} as Record<string, Record<string, string>>,
  configuration: {
    type: 'local'
  },
  masterProductId: undefined,
  masterId: undefined,
  photoUrl: props.data?.devicePhotoUrl || device.deviceCard
})

const vailId = async (_: Record<string, any>, value: string) => {
  if (!props?.data?.id && value) {
    if (!isInput(value)) {
      return Promise.reject($t('Save.index.912481-17'))
    }

    const resp = await isExists(value)
    if (resp.success && resp.result) {
      return Promise.reject($t('Save.index.902471-15'))
    } else {
      return Promise.resolve()
    }
  } else {
    return Promise.resolve()
  }
}

const ensureDeviceDashboardProject = async (device: any) => {
  try {
    if ((!device?.id || !modelRef.productId) && !isSaaS) return
    const product = productList.value.find((i) => i.id === modelRef.productId)
    await ensureVisualizationDashboardProject({
      entityId: device.id,
      projectName: device.name,
      groupId: String(modelRef.productId),
      groupName: product?.name,
      configuration: {
        initDataConfigured: true,
        deviceDashboard: true,
        deviceId: device.id
      }
    })
  } catch (e) {
    console.warn($t('device.InstanceSave.101004-0'), e)
    onlyMessage($t('device.InstanceSave.101004-1'), 'warning')
  }
}

const onChange = (val: any) => {
  const item = productList.value.find((i) => i.id === val)
  modelRef.masterProductId = item.masterProductId
  modelRef.masterId = item.edgeMasterId
  modelRef.configuration.type = 'local'
  if (!props.data?.id) {
    modelRef.photoUrl = item?.photoUrl || device.deviceCard
  }
}

const guideStorageKey = 'saas-device-instance-save-guide'
const guide = new Driver({
  allowClose: true,
  doneBtnText: '我知道了',
  closeBtnText: '关闭'
})

const showGuide = () => {
  if (localStorage.getItem(guideStorageKey) === '1') {
    return
  }
  nextTick(() => {
    guide.defineSteps([
      {
        element: '.create-product-btn',
        popover: {
          className: 'driver',
          title: '创建产品引导',
          description: '创建设备前需先创建并启用一个产品',
          position: 'bottom'
        }
      }
    ])
    guide.start()
    localStorage.setItem(guideStorageKey, '1')
  })
}

const refreshProductList = (selectedProductId?: string) => {
  queryNoPagingPost({
    paging: false,
    sorts: [{ name: 'createTime', order: 'desc' }],
    terms: [
      {
        terms: [
          {
            termType: 'eq',
            column: 'state',
            value: 1
          }
        ]
      }
    ]
  }).then((resp) => {
    if (resp.status === 200) {
      productList.value = resp.result as Record<string, any>[]
      if (selectedProductId) {
        const exists = productList.value.find((item) => item.id === selectedProductId)
        if (exists) {
          modelRef.productId = selectedProductId
          onChange(selectedProductId)
        }
      }
      if (!props.data?.id && !productList.value.length) {
        showGuide()
      }
    }
  })
}

const { onOpen: onProductAddOpen } = useTabSaveSuccess('device/Product/Add', {
  onSuccess(value) {
    if (value?.result?.id) {
      refreshProductList(value.result.id)
      onlyMessage($t('Save.index.902471-16'))
      return
    }
    refreshProductList()
  }
})

const openCreateProduct = () => {
  const refreshOnFocus = () => {
    refreshProductList(modelRef.productId as string)
  }
  window.addEventListener('focus', refreshOnFocus, { once: true })
  onProductAddOpen({
    fromDeviceSave: 'true'
  }).catch(() => {
    window.removeEventListener('focus', refreshOnFocus)
  })
}

watch(
  () => props.data,
  (newValue) => {
    refreshProductList()
    Object.assign(modelRef, newValue)
    // description 和 describe 处理
    modelRef.describe = newValue?.describe || newValue?.description
  },
  { immediate: true, deep: true }
)

const handleCancel = () => {
  emit('close')
  formRef.value.resetFields()
}

const handleSave = () => {
  formRef.value
    .validate()
    .then(async (_data: any) => {
      loading.value = true
      const obj = { ...modelRef }
      if (!obj.id) {
        delete obj.id
      }
      const resp = await update(obj).finally(() => {
        loading.value = false
      })
      if (resp.success) {
        if (!props.data?.id && resp.result?.id) {
          await ensureDeviceDashboardProject(resp.result)
        }
        if (!props.data?.id && obj.configuration?.type === 'cloud' && resp.result?.id && obj.masterProductId) {
          const response = await deviceCloudSave({
            masterProductId: modelRef.masterProductId,
            deviceId: resp.result?.id,
            masterId: modelRef.masterId,
            masterDeviceName: resp.result?.name,
            masterAutoCreate: true
          }).finally(() => {
            emit('save', !props.data?.id, resp.result)
          })
          if (response.success) {
            onlyMessage($t('Save.index.902471-16'))
          }
        } else {
          onlyMessage($t('Save.index.902471-16'))
          emit('save', !props.data?.id, resp.result)
        }
      }
    })
    .catch((err: any) => {
      console.log('error', err)
    })
}

const [DefineEmptyCreate, EmptyCreateButton] = createReusableTemplate()
</script>
<style scoped lang="less">
.product-select-with-create {
  display: flex;
  gap: 8px;

  .product-select {
    flex: 1;
  }
}

.product-empty-tip {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
