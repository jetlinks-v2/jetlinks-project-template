<template>
  <j-page-container>
    <FullPage :fixed="false">
      <a-card :bordered="false">
        <a-row :gutter="[24, 24]" class="content-row">
          <a-col :span="14">
            <TitleComponent :data="tText('product.add.title', '新增产品')" />
            <a-form layout="vertical" :model="form" :rules="rules" ref="formRef">
              <a-form-item :label="tText('product.add.createMode', '新增方式')" name="createMode">
                <a-radio-group v-model:value="form.createMode">
                  <a-radio-button value="template">{{ tText('product.add.createMode.template', '标准品类') }}</a-radio-button>
                  <a-radio-button value="custom" :disabled="isFromDeviceSave">{{ tText('product.add.createMode.custom', '自定义品类') }}</a-radio-button>
                </a-radio-group>
              </a-form-item>

              <a-form-item
                v-if="isTemplateMode"
                :label="tText('product.add.template', '产品模板')"
                name="templateId"
              >
                <a-input
                  :value="selectedTemplate?.name"
                  :placeholder="tText('product.add.template.placeholder', '请选择产品模板')"
                  readonly
                  @click="openTemplatePicker"
                >
                  <template #suffix>
                    <AIcon type="UnorderedListOutlined" />
                  </template>
                </a-input>
              </a-form-item>

              <a-row type="flex">
                <a-col flex="180px">
                  <a-form-item name="photoUrl">
                    <pro-upload v-model="form.photoUrl" :accept="imageTypes.toString()" />
                  </a-form-item>
                </a-col>
                <a-col flex="auto">
                  <!-- <a-form-item name="id" :validateFirst="true">
                    <template #label>
                      <span>ID</span>
                      <a-tooltip :title="$t('Save.index.912481-2')">
                        <AIcon type="QuestionCircleOutlined" style="margin-left: 2px" />
                      </a-tooltip>
                    </template>
                    <a-input v-model:value="form.id" :placeholder="$t('Save.index.912481-3')" />
                  </a-form-item> -->
                  <a-form-item :label="$t('Save.index.912481-4')" name="name">
                    <I18nTextField
                      v-model:value="form.name"
                      v-model:i18nMessages="form.i18nMessages"
                      field="name"
                      :label="$t('Save.index.912481-4')"
                      :i18n-max-length="64"
                      :placeholder="$t('Save.index.912481-5')"
                    />
                  </a-form-item>
                  <a-form-item :label="$t('Save.index.912481-6')" name="classifiedId">
                <a-tree-select
                  showSearch
                  v-model:value="form.classifiedId"
                  :placeholder="$t('Save.index.912481-7')"
                  :tree-data="treeList"
                  @change="onClassifiedChange"
                  allow-clear
                  :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
                  :filterTreeNode="(v, option) => filterSelectNode(v, option, 'name')"
                />
              </a-form-item>
                </a-col>
              </a-row>

              <a-form-item :label="$t('Save.index.912481-8')" name="deviceType">
                <j-card-select v-model:value="form.deviceType" :options="deviceList" @change="changeDeviceType">
                  <template #itemRender="{ node }">
                    <div class="select-item">
                      <div>
                        <span>{{ node.label }}</span>
                        <a-tooltip :title="node.tooltip">
                          <AIcon type="QuestionCircleOutlined" style="margin-left: 2px" />
                        </a-tooltip>
                      </div>
                      <img :src="node.iconUrl" alt="" />
                    </div>
                  </template>
                </j-card-select>
              </a-form-item>

              <a-form-item :label="$t('Save.index.912481-9')" name="describe">
                <I18nTextField
                  :maxlength="200"
                  showCount
                  :auto-size="{ minRows: 4, maxRows: 5 }"
                  v-model:value="form.describe"
                  v-model:i18nMessages="form.i18nMessages"
                  field="describe"
                  :label="$t('Save.index.912481-9')"
                  :placeholder="$t('Save.index.912481-10')"
                  textarea
                />
              </a-form-item>

              <template v-if="isTemplateMode && accessMetadata.length">
                <a-form-item :label="tText('product.add.accessConfig', '接入配置')" class="config-title-item">
                  <span />
                </a-form-item>
                <div v-for="(group, groupIndex) in accessMetadata" :key="group.name || groupIndex">
                  <div class="group-name" v-if="group?.name">{{ group.name }}</div>
                  <a-form-item
                    v-for="item in group?.properties || []"
                    :key="item.property"
                    :name="['configuration', item.property]"
                    :rules="getConfigRules(item)"
                  >
                    <template #label>
                      <div>
                        {{ item.name }}
                        <a-tooltip v-if="item.description" :title="item.description">
                          <AIcon type="QuestionCircleOutlined" />
                        </a-tooltip>
                      </div>
                    </template>
                    <j-value-item
                      :itemType="item.type.type"
                      v-model:modelValue="form.configuration[item.property]"
                      :options="getConfigOptions(item)"
                    />
                  </a-form-item>
                </div>
              </template>

              <a-form-item>
                <a-space>
                  <template v-if="isFromDeviceSave">
                    <a-button @click="cancelAndBack">{{ $t('Save.index.912481-1') }}</a-button>
                    <a-button type="primary" :loading="loading" @click="submitAndEnable">{{ tText('saas.product.add.saveAndEnable', '保存并启用') }}</a-button>
                  </template>
                  <template v-else>
                    <a-button @click="goBack">{{ $t('Save.index.912481-1') }}</a-button>
                    <a-button type="primary" :loading="loading" @click="submitData">{{ $t('Save.index.912481-0') }}</a-button>
                  </template>
                </a-space>
              </a-form-item>
            </a-form>
          </a-col>

          <a-col :span="10" class="doc-col">
            <div class="doc">
              <div class="doc-header">
                <a-tabs
                  v-if="isTemplateMode && selectedTemplate"
                  v-model:activeKey="previewTab"
                  size="small"
                  class="doc-switch-tabs"
                >
                  <a-tab-pane key="access" :tab="tText('product.add.accessDetail', '接入方式详情')" />
                  <a-tab-pane key="document" :tab="tText('product.add.deviceDocument', '设备接入说明')" />
                </a-tabs>
              </div>
              <div class="doc-content" :class="{ 'hide-access-extra': hideAccessInterfaceAndMetadata && previewTab === 'access' }">
                <template v-if="isTemplateMode && selectedTemplate">
                  <template v-if="previewTab === 'access'">
                    <div class="section">
                      <div class="section-title">{{ tText('product.add.description', '说明') }}</div>
                      <div class="desc">{{ selectedTemplate.description }}</div>
                    </div>
                    <div class="section">
                      <div class="section-title">{{ tText('product.add.accessMethod', '接入方式') }}</div>
                      <div>{{ selectedTemplate.accessName || '-' }}</div>
                      <div class="desc">
                        {{ tText('product.add.gatewayType', '网关类型：') }}{{ selectedTemplate.accessProvider || '-' }}
                      </div>
                    </div>
                    <div class="section">
                      <div class="section-title">{{ tText('product.add.authDescription', '认证说明') }}</div>
                      <div v-if="accessDocumentHtml" v-html="accessDocumentHtml"></div>
                      <CloudEmpty v-else :description="tText('product.add.authDescription.empty', '暂无认证说明')" />
                    </div>
                    <div class="section">
                      <div class="section-title">{{ tText('product.add.connectionInfo', '连接信息') }}</div>
                      <template v-if="accessAddresses.length">
                        <div v-for="item in accessAddresses" :key="item.address">
                          <a-badge :color="item.health === -1 ? 'red' : 'green'" :text="item.address" />
                        </div>
                      </template>
                      <CloudEmpty v-else :description="tText('product.add.connectionInfo.empty', '暂无连接信息')" />
                    </div>
                    <div class="section">
                      <div class="section-title">{{ isMqtt ? 'topic' : tText('product.add.interfaceInfo', '接口信息') }}</div>
                      <a-table
                        v-if="accessRoutes.length"
                        :columns="isMqtt ? topicColumns : httpColumns"
                        :data-source="accessRoutes"
                        :pagination="false"
                        size="small"
                        :scroll="{ y: 220 }"
                      >
                        <template #bodyCell="{ text, column, record }">
                          <template v-if="column?.key === 'stream'">{{ getStream(record) }}</template>
                          <template v-else>
                            <a-tooltip :title="text">
                              <div class="ellipsis">{{ text || '-' }}</div>
                            </a-tooltip>
                          </template>
                        </template>
                      </a-table>
                      <CloudEmpty v-else :description="tText('product.add.topicOrInterface.empty', '暂无 topic/接口信息')" />
                    </div>
                    <div class="section">
                      <div class="section-title">{{ tText('product.add.metadata', '物模型') }}</div>
                      <a-tabs v-if="hasMetadata" size="small">
                        <template #rightExtra>
                          <a-button type="link" @click="metadataVisible = true">
                            {{ tText('product.add.metadata.view', '查看物模型') }}
                          </a-button>
                        </template>
                        <a-tab-pane key="properties" :tab="`${tText('product.add.metadata.properties', '属性')}(${propertyTableData.length})`">
                          <a-table :columns="propertyColumns" :data-source="propertyTableData" :pagination="false" size="small" />
                        </a-tab-pane>
                        <a-tab-pane key="functions" :tab="`${tText('product.add.metadata.functions', '功能')}(${functionTableData.length})`">
                          <a-table :columns="functionColumns" :data-source="functionTableData" :pagination="false" size="small" />
                        </a-tab-pane>
                        <a-tab-pane key="events" :tab="`${tText('product.add.metadata.events', '事件')}(${eventTableData.length})`">
                          <a-table :columns="eventColumns" :data-source="eventTableData" :pagination="false" size="small">
                            <template #bodyCell="{ text, column }">
                              <template v-if="column?.key === 'level'">
                                <a-tooltip :title="text">
                                  <div class="ellipsis">{{ EventLevel[text] }}</div>
                                </a-tooltip>
                              </template>
                              <template v-else>
                                <a-tooltip :title="text">
                                  <div class="ellipsis">{{ text || '-' }}</div>
                                </a-tooltip>
                              </template>
                            </template>
                          </a-table>
                        </a-tab-pane>
                        <a-tab-pane key="tags" :tab="`${tText('product.add.metadata.tags', '标签')}(${tagTableData.length})`">
                          <a-table :columns="tagColumns" :data-source="tagTableData" :pagination="false" size="small" />
                        </a-tab-pane>
                      </a-tabs>
                      <CloudEmpty v-else :description="tText('product.add.metadata.empty', '暂无可查看的物模型')" />
                    </div>
                  </template>
                  <template v-else>
                    <a-spin :spinning="templateDocumentLoading">
                      <div v-if="templateDocumentHtml" v-html="templateDocumentHtml" class="markdown-body"></div>
                      <CloudEmpty v-else :description="tText('product.add.deviceDocument.empty', '暂无设备接入说明')" />
                    </a-spin>
                  </template>
                </template>
                <p v-else-if="isTemplateMode">{{ tText('product.add.template.selectTip', '请选择标准品类后查看详情') }}</p>
                <p v-else>
                  {{ tText('product.add.customModeTip', '当前为自定义品类，产品接入方式将在后续产品配置中设置。') }}
                </p>
              </div>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </FullPage>

    <TemplatePicker v-if="templatePickerVisible" @cancel="templatePickerVisible = false" @submit="handleTemplateSelect" />
    <a-modal
      v-model:open="metadataVisible"
      :title="tText('product.add.metadata.modalTitle', '模板物模型')"
      width="900px"
      :footer="null"
    >
      <JsonView :value="parsedMetadata" :height="520" />
    </a-modal>
  </j-page-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Rule } from 'ant-design-vue/es/form'
import { onlyMessage, getToken } from '@jetlinks-web/utils'
import { TOKEN_KEY } from '@jetlinks-web/constants'
import { filterSelectNode } from '@jetlinks-web-core/utils'
import { isInput } from '@device-manager-ui/utils/utils'
import { useMenuStore } from '@jetlinks-web-core/store'
import { useRoute } from 'vue-router'
import { useTabSaveSuccessBack } from '@jetlinks-web-core/hooks'
import { device } from '@device-manager-ui/assets'
import { category, queryProductId, addProduct, getConfigView, queryList, _deploy } from '@device-manager-ui/api/product'
import TemplatePicker from './TemplatePicker.vue'
import { I18nTextField } from '@jetlinks-web-core/components'
import type { TemplateItem } from '../../../Template/typings'
import { getTemplateDetail } from '../../../../api/template'
import { marked } from 'marked'
import JsonView from '@device-manager-ui/views/device/components/Metadata/Base/DetailModal/JsonView.vue'
import { ensureVisualizationDashboardProject } from '@device-manager-ui/utils/dashboardProject';

const { t: $t } = useI18n()
const menuStore = useMenuStore()
const route = useRoute()
const { onBack } = useTabSaveSuccessBack()
const tText = (key: string, zh: string) => {
  const text = String($t(key) || '')
  return text && text !== key ? text : zh
}

const isFromDeviceSave = computed(() => !!route.query?.sourceId || String(route.query?.fromDeviceSave || '') === 'true')

const formRef = ref()
const loading = ref(false)
const metadataVisible = ref(false)
const templatePickerVisible = ref(false)
const previewTab = ref('access')
const treeList = ref<Record<string, any>[]>([])
const selectedTemplate = ref<TemplateItem>()
const accessDetail = ref<Record<string, any>>({})
const accessRoutes = ref<any[]>([])
const accessMetadata = ref<any[]>([])
const accessConfigDetail = ref<Record<string, any>>({})
const templateDocumentLoading = ref(false)
const templateDocumentContent = ref('')
const hideAccessInterfaceAndMetadata = true

const imageTypes = reactive(['image/jpeg', 'image/png', 'image/jfif', 'image/pjp'])
const deviceList = ref([
  { label: $t('Save.index.912481-11'), value: 'device', iconUrl: device.deviceType1, tooltip: $t('Save.index.912481-12') },
  {
    label: $t('Save.index.912481-13'),
    value: 'childrenDevice',
    iconUrl: device.deviceType2,
    tooltip: $t('Save.index.912481-14'),
  },
  { label: $t('Save.index.912481-15'), value: 'gateway', iconUrl: device.deviceType3, tooltip: $t('Save.index.912481-16') },
])

const form = reactive({
  createMode: 'template' as 'template' | 'custom',
  id: undefined as string | undefined,
  name: '',
  classifiedId: undefined as string | undefined,
  classifiedName: '',
  deviceType: '',
  describe: undefined as string | undefined,
  i18nMessages: {} as Record<string, Record<string, string>>,
  photoUrl: device.deviceProduct,
  templateId: undefined as string | undefined,
  configuration: {} as Record<string, any>,
})

const isTemplateMode = computed(() => form.createMode === 'template')
const isMqtt = computed(() =>
  ['mqtt-server-gateway', 'mqtt-client-gateway'].includes(String(selectedTemplate.value?.accessProvider || '')),
)
const accessDocumentHtml = computed(() =>
  accessConfigDetail.value?.document ? marked(String(accessConfigDetail.value.document)) : '',
)
const templateDocumentHtml = computed(() =>
  templateDocumentContent.value ? marked(String(templateDocumentContent.value)) : '',
)
const accessAddresses = computed(() => accessDetail.value?.channelInfo?.addresses || [])

const parsedMetadata = computed(() => safeParseObject(selectedTemplate.value?.metadata))
const metadataLists = computed(() => {
  const current: any = parsedMetadata.value || {}
  return {
    properties: current.properties || [],
    functions: current.functions || [],
    events: current.events || [],
    tags: current.tags || [],
  }
})
const hasMetadata = computed(
  () =>
    metadataLists.value.properties.length +
      metadataLists.value.functions.length +
      metadataLists.value.events.length +
      metadataLists.value.tags.length >
    0,
)

const EventLevel = {
  ordinary: $t('DetailModal.EventModal.518397-8'),
  warn: $t('DetailModal.EventModal.518397-9'),
  urgent: $t('DetailModal.EventModal.518397-10')
}

const propertyTableData = computed(() =>
  metadataLists.value.properties.map((item: any, index: number) => ({
    key: `p-${item.id || index}`,
    index: index + 1,
    id: item.id || '-',
    name: item.name || '-',
    dataType: item?.valueType?.type || item?.type?.type || '-',
  })),
)
const functionTableData = computed(() =>
  metadataLists.value.functions.map((item: any, index: number) => ({
    key: `f-${item.id || index}`,
    index: index + 1,
    id: item.id || '-',
    name: item.name || '-',
    async: item?.async ? tText('product.add.boolean.yes', '是') : tText('product.add.boolean.no', '否'),
  })),
)
const eventTableData = computed(() =>
  metadataLists.value.events.map((item: any, index: number) => ({
    key: `e-${item.id || index}`,
    index: index + 1,
    id: item.id || '-',
    name: item.name || '-',
    level: item?.expands?.level || item?.level || '-',
  })),
)
const tagTableData = computed(() =>
  metadataLists.value.tags.map((item: any, index: number) => ({
    key: `t-${item.id || index}`,
    index: index + 1,
    id: item.id || '-',
    name: item.name || '-',
    dataType: item?.valueType?.type || item?.type?.type || '-',
  })),
)

const propertyColumns = [
  { title: tText('product.add.table.index', '序号'), dataIndex: 'index', key: 'index', width: 70 },
  { title: tText('product.add.table.id', '标识'), dataIndex: 'id', key: 'id', ellipsis: true },
  { title: tText('product.add.table.name', '名称'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: tText('product.add.table.dataType', '数据类型'), dataIndex: 'dataType', key: 'dataType', ellipsis: true },
]
const functionColumns = [
  { title: tText('product.add.table.index', '序号'), dataIndex: 'index', key: 'index', width: 70 },
  { title: tText('product.add.table.id', '标识'), dataIndex: 'id', key: 'id', ellipsis: true },
  { title: tText('product.add.table.name', '名称'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: tText('product.add.table.async', '是否异步'), dataIndex: 'async', key: 'async', ellipsis: true },
]
const eventColumns = [
  { title: tText('product.add.table.index', '序号'), dataIndex: 'index', key: 'index', width: 70 },
  { title: tText('product.add.table.id', '标识'), dataIndex: 'id', key: 'id', ellipsis: true },
  { title: tText('product.add.table.name', '名称'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: tText('product.add.table.level', '事件级别'), dataIndex: 'level', key: 'level', ellipsis: true },
]
const tagColumns = [
  { title: tText('product.add.table.index', '序号'), dataIndex: 'index', key: 'index', width: 70 },
  { title: tText('product.add.table.id', '标识'), dataIndex: 'id', key: 'id', ellipsis: true },
  { title: tText('product.add.table.name', '名称'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: tText('product.add.table.dataType', '数据类型'), dataIndex: 'dataType', key: 'dataType', ellipsis: true },
]

const topicColumns = [
  { title: 'topic', dataIndex: 'topic', key: 'topic', ellipsis: true },
  { title: tText('product.add.table.stream', '上下行'), dataIndex: 'stream', key: 'stream', ellipsis: true, width: 90 },
  { title: tText('product.add.description', '说明'), dataIndex: 'description', key: 'description', ellipsis: true },
]
const httpColumns = [
  { title: tText('product.add.table.address', '地址'), dataIndex: 'address', key: 'address', ellipsis: true },
  { title: tText('product.add.table.example', '示例'), dataIndex: 'example', key: 'example', ellipsis: true },
  { title: tText('product.add.description', '说明'), dataIndex: 'description', key: 'description', ellipsis: true },
]

const validateInput = async (_rule: Rule, value: string) => {
  if (value) {
    if (!isInput(value)) return Promise.reject($t('Save.index.912481-17'))
    const res = await queryProductId(value)
    if (res.success && res.result) return Promise.reject($t('Save.index.912481-18'))
  }
  return Promise.resolve()
}
const validateTemplate = async () => {
  if (isTemplateMode.value && !form.templateId) return Promise.reject(tText('product.add.template.placeholder', '请选择产品模板'))
  return Promise.resolve()
}
const rules = reactive({
  createMode: [{ required: true, message: tText('product.add.createMode.required', '请选择新增方式'), trigger: 'change' }],
  templateId: [{ validator: validateTemplate, trigger: 'change' }],
  id: [
    { validator: validateInput, trigger: 'blur' },
    { max: 64, message: $t('Save.index.912481-20'), trigger: 'change' },
  ],
  name: [
    { required: true, message: $t('Save.index.912481-5'), trigger: 'blur' },
    { max: 64, message: $t('Save.index.912481-20'), trigger: 'change' },
  ],
  deviceType: [{ required: true, message: $t('Save.index.912481-8'), trigger: 'change' }],
  describe: [{ max: 200, message: $t('Save.index.912481-21'), trigger: 'blur' }],
})

const safeParseObject = (value: any): Record<string, any> => {
  if (!value) return {}
  if (typeof value === 'object') return value
  if (typeof value !== 'string') return {}
  try {
    return JSON.parse(value)
  } catch {
    try {
      return JSON.parse(value.replace(/\"fileType\":/g, '"bodyType":'))
    } catch {
      return {}
    }
  }
}

const clearAccessPreview = () => {
  accessDetail.value = {}
  accessRoutes.value = []
  accessMetadata.value = []
  accessConfigDetail.value = {}
  templateDocumentContent.value = ''
  previewTab.value = 'access'
}

const loadTemplateDocument = async (url?: string, rawDocument?: string) => {
  templateDocumentContent.value = rawDocument || ''
  if (!url) return
  templateDocumentLoading.value = true
  try {
    const response = await fetch(url, {
      headers: {
        [TOKEN_KEY]: getToken(),
      },
    })
    if (!response.ok) {
      throw new Error(`load document failed: ${response.status}`)
    }
    templateDocumentContent.value = await response.text()
  } catch (error) {
    if (!templateDocumentContent.value) {
      onlyMessage(tText('product.add.deviceDocument.loadFailed', '设备接入说明加载失败'), 'warning')
    }
  } finally {
    templateDocumentLoading.value = false
  }
}

watch(
  () => form.createMode,
  (mode) => {
    if (isFromDeviceSave.value && mode === 'custom') {
      form.createMode = 'template'
      return
    }
    if (mode === 'custom') {
      form.templateId = undefined
      selectedTemplate.value = undefined
      form.configuration = {}
      clearAccessPreview()
    } else {
      form.classifiedId = undefined
      form.classifiedName = ''
    }
  },
)

const loadCategory = async () => {
  const resp = await category({ paging: false })
  if (resp.success) treeList.value = normalizeTreeData(resp.result || [])
}

function normalizeTreeData(items: Record<string, any>[] = []): Record<string, any>[] {
  return items.map(({ children, ...item }) => ({
    ...item,
    key: item.id,
    children: children?.length ? normalizeTreeData(children) : undefined,
  }))
}

const findClassifiedNameById = (id?: string): string => {
  if (!id) return ''
  const findName = (nodes: any[]): string => {
    for (const item of nodes || []) {
      if (String(item.id) === String(id)) return item.name || ''
      if (item.children?.length) {
        const childName = findName(item.children)
        if (childName) return childName
      }
    }
    return ''
  }
  return findName(treeList.value as any[])
}

const onClassifiedChange = (value: string) => {
  form.classifiedName = value ? findClassifiedNameById(value) : ''
}

const changeDeviceType = (value: Array<string>) => {
  form.deviceType = value?.[0] || ''
}
const openTemplatePicker = () => (templatePickerVisible.value = true)
const isReqSuccess = (resp: any) => !!(resp && (resp.success || resp.status === 200))

const normalizeProtocolConfigs = (result: any, transport?: string) => {
  const groups: any[] = []
  if (!result) return groups
  if (Array.isArray(result.configs)) {
    result.configs.forEach((item: any) => {
      const properties = item?.properties || item?.configs?.properties || []
      if (properties.length) groups.push({ name: item?.name || tText('product.add.config.defaultGroup', '接入参数'), properties })
    })
    if (groups.length) return groups
  }
  if (result?.configs?.properties?.length) {
    return [{ name: result?.configs?.name || tText('product.add.config.defaultGroup', '接入参数'), properties: result.configs.properties }]
  }
  if (Array.isArray(result?.transports) && transport) {
    const current = result.transports.find(
      (item: any) => String(item?.id || item?.transport || '').toUpperCase() === String(transport).toUpperCase(),
    )
    if (current?.configs?.properties?.length) {
      return [{ name: current?.configs?.name || tText('product.add.config.defaultGroup', '接入参数'), properties: current.configs.properties }]
    }
  }
  return groups
}

const filterProductScopedConfigs = (groups: any[] = []) => {
  return groups
    .map((group: any) => ({
      ...group,
      properties: (group?.properties || []).filter((item: any) => {
        const scopes = item?.scopes || []
        return Array.isArray(scopes) && scopes.includes('product')
      }),
    }))
    .filter((group: any) => (group?.properties || []).length > 0)
}

const queryTemplateAccessDetail = async (template: TemplateItem) => {
  clearAccessPreview()
  if (!template?.accessId) return
  const [gatewayResp, protocolResp] = await Promise.all([
    queryList({ terms: [{ column: 'id', value: template.accessId }] }).catch(() => ({})),
    template.messageProtocol && template.transportProtocol
      ? getConfigView(template.messageProtocol, template.transportProtocol).catch(() => ({}))
      : Promise.resolve({}),
  ])
  if (isReqSuccess(gatewayResp) && gatewayResp?.result?.data?.length) {
    accessDetail.value = gatewayResp.result.data[0] || {}
  }
  if (isReqSuccess(protocolResp)) {
    accessConfigDetail.value = protocolResp.result || {}
    accessRoutes.value = accessConfigDetail.value?.routes || []
    accessMetadata.value = filterProductScopedConfigs(
      normalizeProtocolConfigs(accessConfigDetail.value, template.transportProtocol),
    )
  }
}

const handleTemplateSelect = async (template: TemplateItem) => {
  const detailResp: any = await getTemplateDetail(template.id).catch(() => ({}))
  const currentTemplate =
    isReqSuccess(detailResp) && detailResp?.result ? ({ ...template, ...detailResp.result } as TemplateItem) : ({ ...template } as TemplateItem)
  if (!treeList.value?.length) {
    await loadCategory()
  }
  const templateClassifiedId = currentTemplate.classifiedId ? String(currentTemplate.classifiedId) : undefined
  const matchedClassifiedName = templateClassifiedId ? findClassifiedNameById(templateClassifiedId) : ''

  selectedTemplate.value = currentTemplate
  form.templateId = currentTemplate.id
  form.id = undefined
  form.classifiedId = matchedClassifiedName ? templateClassifiedId : undefined
  form.classifiedName = matchedClassifiedName || ''
  form.configuration = safeParseObject(currentTemplate.configuration)
  templatePickerVisible.value = false
  await Promise.all([queryTemplateAccessDetail(currentTemplate), loadTemplateDocument(currentTemplate.documentUrl, currentTemplate.document)])
}

const getConfigOptions = (item: any) => {
  if (item?.type?.type === 'enum') return (item?.type?.elements || []).map((e: any) => ({ label: e?.text, value: e?.value }))
  if (item?.type?.type === 'boolean') {
    return [
      { label: item?.type?.falseText, value: item?.type?.falseValue },
      { label: item?.type?.trueText, value: item?.type?.trueValue },
    ]
  }
  return undefined
}
const getConfigRules = (item: any) => {
  const ruleList: any[] = []
  if (item?.type?.expands?.required) {
    ruleList.push({
      required: true,
      message: `${item?.type?.type === 'enum' || item?.type?.type === 'boolean' ? tText('product.add.config.select', '请选择') : tText('product.add.config.input', '请输入')}${item?.name}`,
    })
  }
  if (item?.type?.expands?.maxLength) {
    ruleList.push({
      max: item.type.expands.maxLength,
      message: tText('product.add.config.maxLength', '最多可输入{length}个字符').replace('{length}', String(item.type.expands.maxLength)),
    })
  }
  return ruleList
}

const getStream = (record: any) => {
  const list: string[] = []
  if (record?.upstream) list.push(tText('product.add.stream.upstream', '上行'))
  if (record?.downstream) list.push(tText('product.add.stream.downstream', '下行'))
  return list.join(',')
}

const goBack = () => menuStore.jumpPage('device/Product', {})
const backToDeviceCreate = async (data: any) => {
  if (window.opener && !window.opener.closed) {
    window.opener.focus()
  }
  const handled = await onBack(data)
  if (handled) {
    return
  }
  if (window.opener && !window.opener.closed) {
    window.close()
    return
  }
  if (window.history.length > 1) {
    window.history.back()
    return
  }
  window.close()
}
const cancelAndBack = async () => {
  await backToDeviceCreate({
    success: false
  })
}

const buildPayload = () => {
  const payload: Record<string, any> = {
    id: form.id || undefined,
    name: form.name,
    photoUrl: form.photoUrl,
    deviceType: form.deviceType,
    describe: form.describe,
    i18nMessages: form.i18nMessages,
    classifiedId: form.classifiedId,
    classifiedName: form.classifiedName,
  }
  if (isTemplateMode.value) {
    if (!selectedTemplate.value) {
      throw new Error(tText('product.add.template.placeholder', '请选择产品模板'))
    }
    Object.assign(payload, {
      templateId: selectedTemplate.value.id,
      accessId: selectedTemplate.value.accessId,
      accessName: selectedTemplate.value.accessName,
      accessProvider: selectedTemplate.value.accessProvider,
      messageProtocol: selectedTemplate.value.messageProtocol,
      transportProtocol: selectedTemplate.value.transportProtocol,
      protocolName: selectedTemplate.value.protocolName,
      metadata: selectedTemplate.value.metadata,
      configuration: form.configuration,
      storePolicy: selectedTemplate.value.storePolicy,
    })
  }
  return payload
}

const ensureProductDashboardProject = async (productId: string) => {
  try {
    await ensureVisualizationDashboardProject({
      entityId: productId,
      projectName: String(form.name || ''),
      groupId: productId,
      groupName: String(form.name || ''),
      configuration: {
        initDataConfigured: true,
        productDashboard: true,
        productId,
      },
    });
  } catch (e) {
    console.warn($t('device.ProductSave.101005-0'), e);
    onlyMessage($t('device.ProductSave.101005-1'), 'warning');
  }
}

const submitData = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    const resp = await addProduct(buildPayload())
    loading.value = false
    if (resp.success) {
      // 新增产品成功后，同步创建产品仪表盘项目（失败不阻断）
      if (resp.result?.id) {
        await ensureProductDashboardProject(String(resp.result.id));
      }
      onlyMessage($t('Save.index.912481-22'))
      goBack()
    } else {
      onlyMessage($t('Save.index.912481-23'), 'error')
    }
  } catch (err: any) {
    loading.value = false
    if (err?.message) {
      onlyMessage(err.message, 'warning')
    }
  }
}

const submitAndEnable = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    const resp = await addProduct(buildPayload())
    if (!resp.success) {
      onlyMessage($t('Save.index.912481-23'), 'error')
      loading.value = false
      return
    }
    const productId = resp?.result?.id
    if (productId) {
      await _deploy(productId)
      // 新增产品成功后，同步创建产品仪表盘项目（失败不阻断）
      await ensureProductDashboardProject(productId);
    }
    loading.value = false
    onlyMessage($t('Save.index.912481-22'))
    await backToDeviceCreate({
      success: true,
      result: {
        id: productId
      }
    })
  } catch (err: any) {
    loading.value = false
    if (err?.message) {
      onlyMessage(err.message, 'warning')
    }
  }
}

loadCategory()
</script>

<style scoped lang="less">
.select-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.content-row {
  align-items: stretch;
}

.doc-col {
  display: flex;
}

.doc {
  flex: 1;
  min-height: 720px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.doc-switch-tabs {
  flex: 1;
  min-width: 0;
}

.doc-switch-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
  justify-content: flex-start;
}

.doc-switch-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.doc-switch-tabs :deep(.ant-tabs-tab) {
  padding: 0 0 8px;
  font-size: 16px;
  font-weight: 500;
}

.doc-switch-tabs :deep(.ant-tabs-tab + .ant-tabs-tab) {
  margin-left: 24px;
}

.doc-switch-tabs :deep(.ant-tabs-ink-bar) {
  height: 3px;
}

.doc-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.hide-access-extra > .section:nth-of-type(5),
.hide-access-extra > .section:nth-of-type(6) {
  display: none;
}

.section {
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.desc {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
}

.group-name {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
  margin-bottom: 6px;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.config-title-item {
  margin-bottom: 8px;
}

.markdown-body {
  min-height: 100%;
  overflow: auto;
  padding-right: 2px;
}
</style>
