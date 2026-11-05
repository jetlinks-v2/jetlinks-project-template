<script setup lang="ts">
import { useMenuStore } from '@jetlinks-web-core/store'
import { getToken } from '@jetlinks-web/utils'
import Logo from '@saas-runtime-ui/assets/Sentinel.png'

const menuStore = useMenuStore()
const owner = 'sentinel'

const onJumpPage = () => {
  const fromUrl = window.location.href

  window.location.href =  `https://sentinel-ai.jetlinks.cn/#/?token=${getToken()}&from=${fromUrl}`
}
</script>

<template>
  <a-tooltip title="Sentinel AI" >
    <img :src="Logo" style="width: 20px;cursor: pointer;" v-if="menuStore.hasOwnerMenu(owner)" @click="onJumpPage" />
  </a-tooltip>
</template>

<style scoped lang="less">

</style>
