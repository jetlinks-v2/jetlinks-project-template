<template>

  <DefineEmptyCreate1>
    <j-permission-button type="primary" hasPermission="link/Type:add" @click="addNetwork">
      <template #icon><AIcon type="PlusOutlined" /></template>
      {{ $t('Network.index.041705-2') }}
    </j-permission-button>
  </DefineEmptyCreate1>
  <DefineEmptyCreate2>
    <j-permission-button
      v-if="showAddBtn"
      type="primary"
      hasPermission="link/Protocol:add"
      :disabled="id !== ':id'"
      @click="addProtocol"
    >
      <template #icon><AIcon type="PlusOutlined" /></template>
      {{ $t('Network.index.041705-2') }}
    </j-permission-button>
  </DefineEmptyCreate2>
  <div v-bind="$attrs">
    <a-steps :current="stepCurrent">
      <a-step v-for="item in steps" :key="item" disabled :title="item" />
    </a-steps>
    <div class="steps-content">
      <div v-if="current === 0" class="steps-box">
        <div class="alert">
          <AIcon type="InfoCircleOutlined" />
          {{ $t('Network.index.041705-0') }}
        </div>
        <div class="search">
          <a-input-search allowClear :placeholder="$t('Network.index.041705-1')" style="width: 300px" @search="networkSearch" />
          <EmptyCreateButton1 />
        </div>
        <j-scrollbar height="480">
          <a-row v-if="networkList.length > 0" :gutter="[24, 24]" style="width: 100%">
            <a-col v-for="item in networkList" :key="item.id" :span="8">
              <AccessCard
                :checked="networkCurrent"
                :data="{
                  ...item,
                  description: item.description ? item.description : descriptionList[provider.id],
                  type: 'network',
                }"
                @checkedChange="checkedChange"
              >
                <template #other>
                  <div class="other">
                    <a-tooltip placement="top" :title="addressesTip(item.addresses)">
                      <div v-for="address in (item.addresses || []).slice(0, 1)" :key="address.address" class="item">
                        <a-badge :status="getColor(address)" :text="address.address" />
                        <span v-if="(item.addresses || []).length > 1">
                          {{ $t('Network.index.041705-3') }}{{ item.addresses.length }}{{ $t('Network.index.041705-4') }}
                        </span>
                      </div>
                    </a-tooltip>
                  </div>
                </template>
              </AccessCard>
            </a-col>
          </a-row>
          <CloudEmpty v-else style="margin-top: 10%" :description="$t('Network.index.041705-5')" :search="networkKeyword" :search-name="$t('ComprehensiveHome.index.926510-25')">
            <EmptyCreateButton1 />
          </CloudEmpty>
        </j-scrollbar>
      </div>

      <div v-else-if="current === 1" class="steps-box">
        <div class="alert">
          <AIcon type="InfoCircleOutlined" />
          {{ $t('Network.index.041705-6') }}
        </div>
        <div class="search">
          <a-input-search allowClear :placeholder="$t('Network.index.041705-1')" style="width: 300px" @search="protocolSearch" />
          <EmptyCreateButton2 v-if="showAddBtn" />
        </div>
        <j-scrollbar height="480">
          <a-row v-if="protocolList.length > 0" :gutter="[24, 24]" style="width: 100%">
            <a-col v-for="item in protocolList" :key="item?.id" :span="8">
              <AccessCard
                :checked="protocolCurrent"
                :data="{ ...item, type: 'protocol' }"
                :disabled="!showAddBtn || id !== ':id'"
                @checkedChange="protocolChange"
              />
            </a-col>
          </a-row>
          <CloudEmpty v-else style="margin-top: 10%" :description="$t('Network.index.041705-5')" :search="protocolKeyword" :search-name="$t('EmptyCreate.names.protocols')">
            <EmptyCreateButton2 />
          </CloudEmpty>
        </j-scrollbar>
      </div>

      <div v-else class="steps-box">
        <div class="card-last" :style="`max-height:${clientHeight > 900 ? 750 : clientHeight * 0.7}px`">
          <a-row :gutter="[24, 24]">
            <a-col :span="12">
              <title-component :data="$t('Network.index.041705-7')" />
              <a-form :model="formData" layout="vertical">
                <a-form-item :label="$t('Network.index.041705-8')" v-bind="validateInfos.name">
                  <I18nTextField
                    v-model:value="formData.name"
                    v-model:i18nMessages="formData.i18nMessages"
                    field="name"
                    :label="$t('Network.index.041705-8')"
                    :i18n-max-length="64"
                    allowClear
                    :placeholder="$t('Network.index.041705-9')"
                  />
                </a-form-item>
                <a-form-item :label="$t('saasRuntime.accessConfig.gatewayBizKey')" v-bind="validateInfos.bizKey">
                  <a-input
                    v-model:value="formData.bizKey"
                    allowClear
                    :maxlength="128"
                    :placeholder="$t('saasRuntime.accessConfig.gatewayBizKeyPlaceholder')"
                  />
                </a-form-item>
                <a-form-item :label="$t('Network.index.041705-10')" v-bind="validateInfos.description">
                  <I18nTextField
                    v-model:value="formData.description"
                    v-model:i18nMessages="formData.i18nMessages"
                    field="description"
                    :label="$t('Network.index.041705-10')"
                    show-count
                    :maxlength="200"
                    :placeholder="$t('Network.index.041705-11')"
                    :rows="4"
                    textarea
                  />
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="12">
              <div class="preview">
                <h1>{{ $t('Network.index.041705-12') }}</h1>
                <p>{{ provider.name }}</p>
                <p>{{ provider.description }}</p>
                <div v-if="getNetworkCurrent.length">
                  <h1>{{ $t('Network.index.041705-13') }}</h1>
                  <p v-for="item in getNetworkCurrent" :key="item.address">
                    <a-badge :status="getColor(item)" :text="item.address" />
                  </p>
                </div>
                <template v-if="!isAgent">
                  <h1>{{ $t('Network.index.041705-14') }}</h1>
                  <p>{{ currentProtocol.name }}</p>
                  <p v-if="config.document">
                    <JMarkdown :source="config.document" />
                  </p>
                  <div v-if="config.routes && config.routes.length > 0">
                    <h1>
                      {{ accessData.provider === 'mqtt-server-gateway' || accessData.provider === 'mqtt-client-gateway' ? 'topic' : $t('Network.index.041705-15') }}
                    </h1>
                    <a-table
                      bordered
                      :columns="config.id === 'MQTT' ? columnsMQTT : columnsHTTP"
                      :data-source="config.routes || []"
                      :pagination="false"
                      :rowKey="uuid"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.dataIndex === 'stream'">
                          {{ getStream(record) }}
                        </template>
                      </template>
                    </a-table>
                  </div>
                </template>
              </div>
            </a-col>
          </a-row>
        </div>
      </div>
    </div>
    <div class="steps-action">
      <a-button v-if="type === 'child-device' ? current > 1 : current > 0" class="action-btn" @click="prev">
        {{ $t('Network.index.041705-16') }}
      </a-button>
      <j-permission-button
        v-if="current === 2 && view === 'false'"
        type="primary"
        class="action-btn"
        :hasPermission="`link/AccessConfig:${id === ':id' ? 'add' : 'update'}`"
        :loading="loading"
        @click="saveData"
      >
        {{ $t('Network.index.041705-17') }}
      </j-permission-button>
      <a-button v-if="[0, 1].includes(current)" type="primary" @click="next">
        {{ $t('Network.index.041705-18') }}
      </a-button>
    </div>
  </div>
</template>

<script lang="ts" setup name="SaasRuntimeAccessNetwork">
import { createReusableTemplate } from '@vueuse/core'

import { useI18n } from 'vue-i18n'
import AccessCard from '@device-manager-ui/views/link/AccessConfig/components/AccessCard/index.vue'
import { I18nTextField } from '@jetlinks-web-core/components'
import { useRuntimeAccessNetwork } from './useRuntimeAccessNetwork'

const props = defineProps({
  provider: {
    type: Object,
    default: () => ({}),
  },
  data: {
    type: Object,
    default: () => ({}),
  },
  bindProduct: {
    type: Boolean,
    default: false,
  },
})

const { t: $t } = useI18n()
const {
  accessData,
  addNetwork,
  addProtocol,
  addressesTip,
  checkedChange,
  clientHeight,
  columnsHTTP,
  columnsMQTT,
  config,
  current,
  currentProtocol,
  descriptionList,
  formData,
  getColor,
  getNetworkCurrent,
  getStream,
  id,
  isAgent,
  loading,
  networkCurrent,
  networkList,
  networkSearch,
  networkKeyword,
  next,
  prev,
  protocolChange,
  protocolCurrent,
  protocolList,
  protocolSearch,
  protocolKeyword,
  saveData,
  showAddBtn,
  stepCurrent,
  steps,
  type,
  uuid,
  validateInfos,
  view,
} = useRuntimeAccessNetwork(props as any, $t)

const [DefineEmptyCreate1, EmptyCreateButton1] = createReusableTemplate()
const [DefineEmptyCreate2, EmptyCreateButton2] = createReusableTemplate()
</script>

<style lang="less" scoped>
.steps-content {
  margin-top: 20px;
}

.steps-box {
  min-height: 400px;
}

.card-last {
  padding-right: 5px;
  overflow-x: hidden;
  overflow-y: auto;
}

.steps-action {
  width: 100%;
  margin-top: 24px;
}

.action-btn {
  margin-right: 8px;
}

.alert {
  height: 40px;
  padding-left: 10px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 40px;
  background-color: #f6f6f6;
}

.search {
  display: flex;
  justify-content: space-between;
  margin: 15px 0;
}

.preview {
  height: 60vh;
  overflow: auto;
}

.other {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 20px;
}

.item {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
