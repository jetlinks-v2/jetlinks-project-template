<template>
  <div class="form-card">
    <div class="selected-product">
      <img class="selected-product__image" :src="product?.photoUrl || device.deviceProduct" :alt="product?.name" />
      <div class="selected-product__content">
        <div class="selected-product__label">{{ tText('mobile.product.list.selected', '已选产品') }}</div>
        <div class="selected-product__name">
          <j-ellipsis>{{ product?.name || '-' }}</j-ellipsis>
        </div>
      </div>
    </div>

    <a-form ref="formRef" layout="vertical" :model="formData" :rules="rules">
      <a-form-item name="name" :label="tText('mobile.product.list.device.name', '网关名称')">
        <a-input
          :value="formData.name"
          :maxlength="64"
          :placeholder="tText('mobile.product.list.device.name.placeholder', '请输入网关名称')"
          @update:value="updateField('name', $event)"
        />
      </a-form-item>

      <a-form-item name="describe" :label="tText('mobile.product.list.device.describe', '网关描述')">
        <a-textarea
          :value="formData.describe"
          :maxlength="200"
          show-count
          :auto-size="{ minRows: 4, maxRows: 6 }"
          :placeholder="tText('mobile.product.list.device.describe.placeholder', '请输入网关描述')"
          @update:value="updateField('describe', $event)"
        />
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import type { Rule } from 'ant-design-vue/es/form'
import type { PropType } from 'vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { device } from '@device-manager-ui/assets'

interface ProductCardItem {
  id: string
  name?: string
  photoUrl?: string
}

interface DeviceFormState {
  name: string
  describe: string
}

const emit = defineEmits(['update:formData'])

const props = defineProps({
  product: {
    type: Object as PropType<ProductCardItem | undefined>,
    default: undefined,
  },
  formData: {
    type: Object as PropType<DeviceFormState>,
    required: true,
  },
})

const { t: $t } = useI18n()
const formRef = ref()

const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const rules: Record<string, Rule[]> = {
  name: [
    { required: true, message: tText('mobile.product.list.device.name.placeholder', '请输入网关名称'), trigger: 'blur' },
    { max: 64, message: tText('mobile.product.list.device.name.max', '名称最多 64 个字符'), trigger: 'change' },
  ],
  describe: [{ max: 200, message: tText('mobile.product.list.device.describe.max', '描述最多 200 个字符'), trigger: 'change' }],
}

const updateField = (key: keyof DeviceFormState, value: string) => {
  emit('update:formData', {
    ...props.formData,
    [key]: value,
  })
}

const validate = async () => {
  await formRef.value?.validate()
}

defineExpose({ validate })
</script>

<style scoped lang="less">
.form-card {
  padding: 16px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.selected-product {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8fbff;
  border: 1px solid rgba(22, 119, 255, 0.16);
  border-radius: 14px;
}

.selected-product__image {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 14px;
  background: #f3f4f6;
  flex-shrink: 0;
}

.selected-product__content {
  flex: 1;
  min-width: 0;
}

.selected-product__label {
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.selected-product__name {
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  word-break: break-all;
}
</style>
