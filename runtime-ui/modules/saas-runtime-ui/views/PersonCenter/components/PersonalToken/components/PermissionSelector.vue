<template>
  <div class="permission-selector">
    <PermissionList
        v-model:value="selectedPermissions"
        selected-init-type="fullaccess"
        :disabled="disabled"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import PermissionList from './PermissionList.vue'
import type { PermissionScope } from './data'
const props = defineProps({
  value: {
    type: Array as PropType<PermissionScope[]>,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:value'])

// 使用表单的受控值，避免挂载时回填旧授权覆盖完整查询得到的最新全量权限。
const selectedPermissions = computed({
  get: () => props.value,
  set: (value: PermissionScope[]) => emit('update:value', value),
})

</script>

<style lang="less" scoped>
.permission-selector {
  position: relative;
  width: 100%;
}
</style>
