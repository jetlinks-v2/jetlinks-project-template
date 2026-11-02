<!-- 产品模板设备接入 -->
<template>
  <div
    v-if="access.id === undefined || null"
    style="margin-top: 20%; transform: translateY(-50%)"
  >
    <j-empty>
      <template #description>
        <span v-if="permissionStore.hasPermission('device/Template:update')">
          {{ $t('template.detail.access.emptyTip') || '请先' }}<a-button type="link" @click="showModal">{{ $t('template.detail.access.selectGateway') || '选择' }}</a-button>{{ $t('template.detail.access.emptyTipAction') || '设备接入网关，用以提供设备接入能力' }}
        </span>
        <span v-else>{{ $t('template.detail.access.notConfigured') || '当前模板未配置设备接入方式' }}</span>
      </template>
    </j-empty>
  </div>
  <div v-else>
    <a-row :gutter="24">
      <a-col :span="12">
        <Title :data="$t('template.detail.access.accessMethod') || '接入方式'">
          <template #extra>
            <j-permission-button
              style="margin: 0 0 0 20px"
              type="primary"
              size="small"
              :tooltip="{
                title: tooltip,
              }"
              :disabled="checkDisabled"
              ghost
              @click="showDevice"
              hasPermission="device/Template:update"
            >
              {{ $t('template.detail.access.change') || '更换' }}
            </j-permission-button>
          </template>
        </Title>
        <div>
          <div>
            {{ access?.name }}
          </div>
          <div>
            {{
              access?.description ||
              dataSource.find((item) => item?.id === access?.provider)
                ?.description
            }}
          </div>
        </div>
        <template v-if="access?.provider !== 'composite-device-gateway'">
          <div class="item-style">
            <Title :data="$t('template.detail.access.protocol') || '通信协议'"></Title>
            <div>
              {{ access?.protocolDetail?.name }}
            </div>
            <!-- 显示md文件内容 -->
            <div v-if="config?.document" v-html="markdownToHtml"></div>
          </div>
          <div class="item-style">
            <Title :data="$t('template.detail.access.connectionInfo') || '连接信息'"></Title>
            <div v-if="access?.channelInfo?.addresses.length > 0">
              <div
                  v-for="item in access?.channelInfo?.addresses"
                  :key="item.address"
              >
                <a-badge
                    :color="item.health === -1 ? 'red' : 'green'"
                    :text="item.address"
                >
                </a-badge>
              </div>
            </div>
            <div v-else>{{ $t('template.detail.access.noData') || '暂无数据' }}</div>
          </div>
          <!--        其它接入配置        -->
          <div v-for="(i, index) in metadata">
            <Title v-if="i?.name" :data="i?.name" class="config">
              <template #extra>
                <a-tooltip :title="$t('template.detail.access.otherConfig') || '其它接入配置'">
                  <AIcon type="QuestionCircleOutlined" style="margin-left: 2px"/>
                </a-tooltip>
              </template>
            </Title>
            <a-form ref="formRef" :model="formData.data" layout="vertical">
              <a-form-item
                  :name="item.property"
                  v-for="item in i?.properties || []"
                  :key="item"
                  :rules="getRules(item)"
              >
                <template #label>
                  <div>
                    {{ item.name }}
                    <a-tooltip v-if="item.description" :title="item.description">
                      <AIcon type="QuestionCircleOutlined"/>
                    </a-tooltip>
                  </div>
                </template>
                <j-value-item
                    :itemType="item.type.type"
                    v-model:modelValue="formData.data[item.property]"
                    :options="getOptions(item)"
                ></j-value-item>
              </a-form-item>
            </a-form>
          </div>
        </template>
        <Title :data="$t('template.detail.access.storagePolicy') || '存储策略'">
          <template #extra>
            <a-tooltip :title="$t('template.detail.access.storagePolicyTip') || '设备数据存储策略'">
              <AIcon type="QuestionCircleOutlined" style="margin-left: 2px" />
            </a-tooltip>
          </template>
        </Title>
        <a-form layout="vertical">
          <a-form-item>
            <a-select ref="select" v-model:value="form.storePolicy">
              <a-select-option
                v-for="(item, index) in storageList"
                :key="index"
                :value="item.id"
                >{{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <j-permission-button
          type="primary"
          @click="submitDevice"
          hasPermission="device/Template:update"
          :loading="submitLoading"
          >{{ $t('template.detail.access.save') || '保存' }}
        </j-permission-button>
      </a-col>
      <a-col :span="12" v-if="config?.routes && config?.routes?.length > 0">
        <div class="info">
          <div>
            <div style="font-weight: 600; margin: 0 0 10px 0">
              {{
                access?.provider === "mqtt-server-gateway" ||
                access?.provider === "mqtt-client-gateway"
                  ? ($t('template.detail.access.topic') || 'topic')
                  : ($t('template.detail.access.address') || '接入地址')
              }}
            </div>
            <a-table
              :columns="config.id === 'MQTT' ? columnsMQTT : columnsHTTP"
              :data-source="config?.routes"
              :pagination="false"
              :scroll="{ y: 500 }"
            >
              <template #bodyCell="{ text, column, record }">
                <template v-if="column?.key === 'topic'">
                  <a-tooltip placement="topLeft" :title="text">
                    <div class="ellipsis-style">
                      {{ text }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="column?.key === 'stream'">
                  <div>{{ getStream(record) }}</div>
                </template>
                <template v-if="column.key === 'description'">
                  <a-tooltip placement="topLeft" :title="text">
                    <div class="ellipsis-style">
                      {{ text }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="column?.key === 'address'">
                  <a-tooltip placement="topLeft" :title="text">
                    <div class="ellipsis-style">
                      {{ text }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="column?.key === 'example'">
                  <a-tooltip placement="topLeft" :title="text">
                    <div class="ellipsis-style">
                      {{ text }}
                    </div>
                  </a-tooltip>
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
  <!-- 选择设备 -->
  <AccessModal
    v-if="visible"
    :product-id="templateStore.current.id"
    :deviceType="templateStore.current.deviceType"
    :accessId="accessId"
    :providersList="dataSource"
    @cancel="visible = false"
    @submit="checkAccess"
  />
</template>

<script lang="ts" setup name="TemplateAccessConfig">
import { useTemplateStore } from "../../../../../store/template";
import type { ConfigMetadata } from "../typings";
import Title from "@device-manager-ui/views/device/Product/Detail/Title/index.vue";
import { useAuthStore } from "@jetlinks-web-core/store";
import {
  getProviders,
  queryList,
  getConfigView,
  // getConfigMetadata,
  getStoragList,
} from "@device-manager-ui/api/product";
import { updateTemplate } from "../../../../../api/template";

import { marked } from "marked";
import type { TableColumnType } from "ant-design-vue";
import { map } from "lodash-es";
import AccessModal from "@device-manager-ui/views/device/Product/Detail/DeviceAccess/accessModal.vue";
import {onlyMessage} from "@jetlinks-web-core/utils/comm";
import { useI18n } from 'vue-i18n';

const { t: $t } = useI18n();
const route = useRoute();
const templateStore = useTemplateStore();
const formRef = ref([]);
const permissionStore = useAuthStore();
const render = new marked.Renderer();
marked.setOptions({
  renderer: render,
  gfm: true,
  pedantic: false,
  snaitize: false,
});
const visible = ref<boolean>(false);
const access = ref<Record<string, any>>({});
const accessId = ref<string>(templateStore.current.accessId);
const config = ref<any>({});
const metadata = ref<ConfigMetadata[]>([{ properties: [] }]);
const dataSource = ref<string[]>([]);
const storageList = ref<any[]>([]);
const markdownToHtml = shallowRef("");

//存储数据
const form = reactive<Record<string, any>>({
  storePolicy: templateStore.current?.storePolicy || "none",
});
// 表单数据
const formData = reactive<Record<string, any>>({
  data: templateStore.current?.configuration || {},
});

const submitLoading = ref(false);
const tooltip = ref('');
const checkDisabled = ref(false);

/**
 * 显示弹窗
 */
const showModal = () => {
  visible.value = true;
};

/**
 * 打开设备弹窗
 */
const showDevice = () => {
  visible.value = true;
};

//获取物模型下拉选项
const getOptions = (i: any) => {
  if (i.type.type === "enum") {
    return (i.type?.elements || []).map((item) => {
      return {
        label: item?.text,
        value: item?.value,
      };
    });
  } else if (i.type.type === "boolean") {
    return [
      {
        label: i.type?.falseText,
        value: i.type?.falseValue,
      },
      {
        label: i.type?.trueText,
        value: i.type?.trueValue,
      },
    ];
  }
  return undefined;
};

let columnsMQTT = ref(<TableColumnType>[]);
const ColumnsMQTT = [
  {
    title: "topic",
    dataIndex: "topic",
    key: "topic",
    ellipsis: true,
  },
  {
    title: "上下行",
    dataIndex: "stream",
    key: "stream",
    ellipsis: true,
    align: "center",
    width: 100,
    scopedSlots: { customRender: "stream" },
  },
  {
    title: "说明",
    dataIndex: "description",
    key: "description",
    ellipsis: true,
  },
];
const columnsHTTP = ref(<TableColumnType>[]);
const ColumnsHTTP = [
  {
    title: "地址",
    dataIndex: "address",
    key: "address",
    ellipsis: true,
  },
  {
    title: "示例",
    dataIndex: "example",
    key: "example",
    ellipsis: true,
  },
  {
    title: "说明",
    dataIndex: "description",
    key: "description",
    ellipsis: true,
  },
];

/**
 * 获取上下行数据
 */
const getStream = (record: any) => {
  const list = [];
  if (record?.upstream) {
    list.push("上行");
  }
  if (record?.downstream) {
    list.push("下行");
  }
  return `${list.join(",")}`;
};

/**
 * 查询接入方式
 */
const queryAccessDetail = async (id: string) => {
  const res = await queryList({
    terms: [
      {
        column: "id",
        value: id,
      },
    ],
  }).then((res: any) => {
    if (res.status === 200) {
      access.value = res.result.data[0];
    }
  });
};

const handleColumns = () => {
  const Group = {
    title: "分组",
    dataIndex: "group",
    key: "group",
    ellipsis: true,
    align: "center",
    width: 100,
    customCell: (record: any, rowIndex: number) => {
      const obj = {
        children: record,
        rowSpan: 0,
      };
      const list = config.value?.routes || [];

      const arr = list.filter((res: any) => res.group === record.group);

      const isRowIndex =
        rowIndex === 0 || list[rowIndex - 1].group !== record.group;
      isRowIndex && (obj.rowSpan = arr.length);

      return obj;
    },
  };
  columnsMQTT.value = [Group, ...ColumnsMQTT];
  columnsHTTP.value = [Group, ...ColumnsHTTP];
};

/**
 * 查询协议信息
 */
const getConfigDetail = (
  messageProtocol: string,
  transportProtocol: string,
) => {
  getConfigView(messageProtocol, transportProtocol).then((resp) => {
    if (resp.status === 200) {
      config.value = resp.result;
      handleColumns();
      if (config.value?.document) {
        markdownToHtml.value = marked(config.value.document);
      }
    }
  });
};

const getRules = (item: any) => {
  const rules = [];
  if (item?.type?.expands?.required) {
    rules.push({
      required: true,
      message: `${
        item.type.type === "enum" || item.type.type === "boolean"
          ? "请选择"
          : "请输入"
      }${item.name}`,
    });
  }
  if (item?.type?.expands?.maxLength) {
    rules.push({
      max: item.type.expands.maxLength,
      message: `最多可输入${item.type.expands.maxLength}个字符`,
    });
  }
  return rules;
};

/**
 * 查询保存数据信息
 */
const getData = async (accessId?: string) => {
  const _accessId = accessId || templateStore.current?.accessId;
  // if (templateStore.current?.id) {
  //   getConfigMetadata(templateStore.current?.id).then((resp: any) => {
  //     metadata.value = resp?.result || [
  //       {
  //         properties: [],
  //       },
  //     ];
  //     // 流传输模式 初始为udp模式
  //     if (metadata.value.length) {
  //       metadata.value = metadata.value.map((i: any) => {
  //         i.properties = i?.properties.filter((item: any) => {
  //           if (
  //             item.name === "流传输模式" &&
  //             (!templateStore.current?.configuration ||
  //               !templateStore.current?.configuration.hasOwnProperty(
  //                 item.property,
  //               ))
  //           ) {
  //             formData.data[item.property] = item.type.expands?.defaultValue;
  //           }
  //           return item.name !== "流传输模式";
  //         });
  //         return i;
  //       });
  //     }
  //   });
  // }
  if (_accessId) {
    queryAccessDetail(_accessId);
    getConfigDetail(
      templateStore.current?.messageProtocol || "",
      templateStore.current?.transportProtocol || "",
    );
  }
  getStoragList().then((resp: any) => {
    if (resp.success) {
      storageList.value = resp.result;
      form.storePolicy = templateStore.current?.storePolicy ||resp.result[0]?.id || 'none';
    }
  });
};

const checkAccess = async (data: any) => {
  visible.value = false;
  accessId.value = data.access.id;
  access.value = data.access;
  config.value = data.access?.transportDetail || {};
  metadata.value = data.metadata || [
    {
      properties: [],
    },
  ];
  if (metadata.value.length) {
    metadata.value = metadata.value.map((i: any) => {
      i.properties = i?.properties.filter((item: any) => {
        if (
          item.name === "流传输模式" &&
          (!templateStore.current?.configuration ||
            !templateStore.current?.configuration.hasOwnProperty(item.property))
        ) {
          formData.data[item.property] = item.type.expands?.defaultValue;
        }
        return item.name !== "流传输模式";
      });
      return i;
    });
  }
  handleColumns();
  markdownToHtml.value = config.value?.document
    ? marked(config.value.document)
    : "";
};

/**
 * 获取协议类型名称
 */
const getProvidersList = async () => {
  const res: any = await getProviders();
  dataSource.value = res.result;
};

/**
 * 保存设备接入
 */
const submitDevice = async () => {
  const allValidate = formRef.value.map((item: any) => {
    return item?.validate();
  });
  const res = await Promise.all(allValidate);
  if (!res) return;
  const values = { storePolicy: form.storePolicy, ...formData.data };
  const id = templateStore.current?.id;
  updateAccessData(id, values);
};

const updateAccessData = async (id: string, values: any) => {
  const { storePolicy, ...extra } = values;

  // 更新模板
  const templateObj = {
    ...templateStore.current,
    transportProtocol: access.value?.transport,
    protocolName: access.value?.protocolDetail?.name,
    accessId: access.value?.id,
    accessName: access.value?.name,
    accessProvider: access.value?.provider,
    messageProtocol: access.value?.protocol,
    configuration: { ...extra },
    storePolicy: storePolicy,
  };

  submitLoading.value = true;
  const resp = await updateTemplate(templateObj).finally(() => {
    submitLoading.value = false;
  });

  if (resp.status === 200) {
    onlyMessage($t('template.detail.access.saveSuccess') || '保存成功');
    templateStore.refresh(id);
  }
};

onMounted(() => {
  getProvidersList();
  getData();
});

watch(
  () => templateStore.current,
  () => {
    if (templateStore.current?.id) {
      getData();
    }
  },
);
</script>

<style scoped lang="less">
.item-style {
  margin: 16px 0;
}

.ellipsis-style {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.info {
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
}
</style>
