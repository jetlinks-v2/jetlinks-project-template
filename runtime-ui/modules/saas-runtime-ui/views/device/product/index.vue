<template>
  <j-permission-button hasPermission="device/Product:add" @click="handleAdd">
    {{ $t('template.product.index.add') }}
  </j-permission-button>
</template>

<script setup lang="ts">
import { useMenuStore } from '@jetlinks-web-core/store'

const menuStore = useMenuStore()

const handleAdd = () => {
  menuStore.jumpPage('device/Product/Add', {})
}
</script>
