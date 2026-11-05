<template>
  <j-page-container>
    <a-spin :spinning="loading">
      <div v-if="type && id === ':id'">
        <Provider :dataSource="dataSource" @onClick="goProviders" />
      </div>
      <FullPage v-else :fixed="false">
        <a-card :bordered="false">
          <div class="go-back" v-if="id === ':id'">
            <a @click="goBack">{{ $t('Detail.index.308483-0') }}</a>
          </div>
          <template v-if="showType === 'network'">
            <Plugin
              v-if="provider.id === 'plugin_gateway'"
              :bindProduct="bindProduct"
              :data="data"
              :provider="provider"
            />
            <GateWay
              v-else-if="provider.id === 'collector-gateway'"
              :bindProduct="bindProduct"
              :data="data"
              :provider="provider"
            />
            <Composite
              v-else-if="provider.id === 'composite-device-gateway'"
              :data="data"
              :otherProvider="compositeProvider"
            />
            <RuntimeNetwork
              v-else
              :bindProduct="bindProduct"
              :data="data"
              :provider="provider"
            />
          </template>
          <Media v-else-if="showType === 'media'" :bindProduct="bindProduct" :provider="provider" :data="data" />
          <Channel v-else-if="showType === 'channel'" :bindProduct="bindProduct" :provider="provider" :data="data" />
          <Edge v-else-if="showType === 'edge'" :bindProduct="bindProduct" :provider="provider" :data="data" />
          <Cloud v-else-if="showType === 'cloud'" :bindProduct="bindProduct" :provider="provider" :data="data" />
        </a-card>
      </FullPage>
    </a-spin>
  </j-page-container>
</template>

<script lang="ts" setup name="SaasRuntimeAccessConfigDetail">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import RuntimeNetwork from '../components/RuntimeNetwork/index.vue'
import Provider from '@device-manager-ui/views/link/AccessConfig/components/Provider/index.vue'
import Media from '@device-manager-ui/views/link/AccessConfig/components/Media/index.vue'
import Channel from '@device-manager-ui/views/link/AccessConfig/components/Channel/index.vue'
import Edge from '@device-manager-ui/views/link/AccessConfig/components/Edge/index.vue'
import GateWay from '@device-manager-ui/views/link/AccessConfig/components/Edge/geteway.vue'
import Cloud from '@device-manager-ui/views/link/AccessConfig/components/Cloud/index.vue'
import Plugin from '@device-manager-ui/views/link/AccessConfig/components/Plugin/index.vue'
import Composite from '@device-manager-ui/views/link/AccessConfig/components/Composite/index.vue'
import { detail, getProviders } from '@device-manager-ui/api/link/accessConfig'
import { queryProductList } from '@device-manager-ui/api/product'
import { accessConfigTypeFilter } from '@jetlinks-web-core/utils'

const { t: $t } = useI18n()
const route = useRoute()
const id = route.params.id as string

const dataSource = ref<any[]>([])
const type = ref(false)
const loading = ref(true)
const provider = ref<Record<string, any>>({})
const data = ref<Record<string, any>>({})
const showType = ref('')
const bindProduct = ref(false)

const TypeMap = new Map([
  ['fixed-media', 'media'],
  ['gb28181-2016', 'media'],
  ['onvif', 'media'],
  ['media-plugin', 'media'],
  ['OneNet', 'cloud'],
  ['OneNet-platform', 'cloud'],
  ['Ctwing', 'cloud'],
  ['modbus-tcp', 'channel'],
  ['opc-ua', 'channel'],
  ['official-edge-gateway', 'edge'],
  ['edge-child-device', 'edge'],
  ['network', 'network'],
  ['agent-device-gateway', 'network'],
  ['agent-media-device-gateway', 'network'],
])

const DataMap = new Map()
DataMap.set('fixed-media', { type: 'media', title: $t('Detail.index.308483-1') })
DataMap.set('gb28181-2016', { type: 'media', title: $t('Detail.index.308483-1') })
DataMap.set('onvif', { type: 'media', title: $t('Detail.index.308483-1') })
DataMap.set('media-plugin', { type: 'media', title: $t('Detail.index.308483-1') })
DataMap.set('OneNet', { type: 'cloud', title: $t('Detail.index.308483-2') })
DataMap.set('OneNet-platform', { type: 'cloud', title: $t('Detail.index.308483-2') })
DataMap.set('Ctwing', { type: 'cloud', title: $t('Detail.index.308483-2') })
DataMap.set('modbus-tcp', { type: 'channel', title: $t('Detail.index.308483-3') })
DataMap.set('opc-ua', { type: 'channel', title: $t('Detail.index.308483-3') })
DataMap.set('official-edge-gateway', { type: 'edge', title: $t('Detail.index.308483-4') })
DataMap.set('edge-child-device', { type: 'edge', title: $t('Detail.index.308483-4') })
DataMap.set('network', { type: 'network', title: $t('Detail.index.308483-5') })
DataMap.set('agent-device-gateway', { type: 'network', title: $t('Detail.index.308483-6') })
DataMap.set('agent-media-device-gateway', { type: 'network', title: $t('Detail.index.308483-6') })

const compositeProvider = computed(() => {
  const arr: any[] = []
  dataSource.value.forEach((group) => {
    group.list?.forEach((item: any) => {
      if (item.type === 'network' && !['composite-device-gateway', 'plugin_gateway', 'agent-device-gateway', 'agent-media-device-gateway'].includes(item.id)) {
        arr.push(item)
      }
    })
  })
  return arr
})

const goProviders = (param: any) => {
  if (!param) return
  showType.value = param.type
  provider.value = param
  type.value = false
}

const goBack = () => {
  provider.value = {}
  type.value = true
}

function getTypeList(result: any[]) {
  const groups: Record<string, any[]> = { media: [], network: [], cloud: [], channel: [], edge: [], agent: [] }
  result.forEach((item) => {
    if (['fixed-media', 'gb28181-2016', 'onvif', 'media-plugin'].includes(item.id)) {
      item.type = 'media'
      groups.media.push(item)
    } else if (['OneNet', 'Ctwing', 'OneNet-platform'].includes(item.id)) {
      item.type = 'cloud'
      groups.cloud.push(item)
    } else if (['modbus-tcp', 'opc-ua', 'collector-gateway'].includes(item.id)) {
      item.type = 'channel'
      if (item.id === 'collector-gateway') groups.channel.push(item)
    } else if (['official-edge-gateway', 'edge-child-device'].includes(item.id)) {
      item.type = 'edge'
      groups.edge.push(item)
    } else if (['agent-device-gateway', 'agent-media-device-gateway'].includes(item.id)) {
      item.type = 'network'
      groups.agent.push(item)
    } else {
      item.type = 'network'
      groups.network.push(item)
    }
  })
  return [
    groups.network.length && { list: groups.network, title: $t('Detail.index.308483-5') },
    groups.media.length && { list: groups.media, title: $t('Detail.index.308483-1') },
    groups.cloud.length && { list: groups.cloud, title: $t('Detail.index.308483-2') },
    groups.channel.length && { list: groups.channel, title: $t('Detail.index.308483-3') },
    groups.edge.length && { list: groups.edge, title: $t('Detail.index.308483-4') },
    groups.agent.length && { list: groups.agent, title: $t('Detail.index.308483-6') },
  ].filter(Boolean)
}

async function queryProviders() {
  const resp: any = await getProviders()
  if (resp.status !== 200) return
  const _data = resp.result || []
  dataSource.value = getTypeList(accessConfigTypeFilter(_data as any[]))
  if (route.query.save && route.query?.type) {
    const typeValue = String(route.query.type)
    goProviders(dataSource.value.find((f: any) => f.title === DataMap.get(typeValue)?.title)?.list?.find((f: any) => f.id === typeValue))
  }
}

async function checkBindProduct(_id: string) {
  const resp = await queryProductList({
    paging: false,
    terms: [{ column: 'accessId', termType: 'eq', value: _id }],
  })
  if (resp.success && resp.result?.total) bindProduct.value = true
}

async function getProvidersData() {
  if (id !== ':id' && !route.query.provider) {
    void checkBindProduct(id)
    const response: any = await getProviders()
    if (response.status === 200) {
      dataSource.value = getTypeList(accessConfigTypeFilter((response.result || []) as any[]))
      const resp: any = await detail(id)
      if (resp.status === 200) {
        provider.value = response.result.find((item: any) => item?.id === resp.result.provider) || {}
        resp.result.type = TypeMap.get(resp.result.provider) || TypeMap.get('network')
        showType.value = resp.result.type
        data.value = resp.result
        type.value = false
      }
    }
  } else {
    type.value = route.query.provider ? false : true
    if (!route.query.data) await queryProviders()
  }
  loading.value = false
}

watch(() => route.query.provider, (val) => {
  if (!val) return
  const _data = JSON.parse(String(val))
  type.value = false
  showType.value = _data.type
  provider.value = _data
}, { immediate: true })

watch(() => route.query.data, (val) => {
  if (val) data.value = JSON.parse(String(val))
}, { immediate: true })

onMounted(() => {
  loading.value = true
  void getProvidersData()
})
</script>

<style lang="less" scoped>
.go-back {
  margin: 0 0 20px 0;
}
</style>
