export const tabs = {
  Info: defineAsyncComponent(() => import('./BasicInfo/index.vue')),
  Metadata: defineAsyncComponent(() => import('../../components/Metadata/index.vue')),
  Device: defineAsyncComponent(() => import('./DeviceAccess/index.vue')),
  Document: defineAsyncComponent(() => import('./Document/index.vue')),
};