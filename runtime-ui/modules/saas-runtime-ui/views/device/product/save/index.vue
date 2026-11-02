<script setup name="ProductSave">
import { useMenuStore } from '@jetlinks-web-core/store'
import { useI18n } from "vue-i18n";

const { t: $t } = useI18n();
const menuStore = useMenuStore()

const handleAdd = () => {
  menuStore.jumpPage('device/Product/Add', {})
}
</script>

<template>
  <j-permission-button
      type="primary"
      key="add"
      @click="handleAdd"
      hasPermission="device/Product:add"
  >
    <template #icon
    ><AIcon type="PlusOutlined"
    /></template>
    {{ $t('Instance.index.133466-0') }}
  </j-permission-button>
</template>

<style scoped lang="less">

</style>
