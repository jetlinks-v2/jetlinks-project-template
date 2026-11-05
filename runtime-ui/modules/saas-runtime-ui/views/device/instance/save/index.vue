<script setup name="InstanceSave">
import Save from './SaveModal.vue'
import {useI18n} from "vue-i18n";

const { t: $t } = useI18n();
const visible = ref(false)

const emit = defineEmits(['close', 'save']);
const props = defineProps({
  onSave: Function,
})

const handleAdd = () => {
  visible.value = true
}

const handleClose = () => {
  visible.value = false
}

const _onSave = () => {
  props.onSave()
  visible.value = false
}
</script>

<template>
  <j-permission-button
      type="primary"
      key="add"
      @click="handleAdd"
      hasPermission="device/Instance:add"
  >
    <template #icon
    ><AIcon type="PlusOutlined"
    /></template>
    {{ $t('Instance.index.133466-0') }}
  </j-permission-button>
<Save v-if="visible" :showId="false" @close="handleClose" @save="_onSave" />
</template>

<style scoped lang="less">

</style>