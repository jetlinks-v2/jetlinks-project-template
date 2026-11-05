/**
 * 产品模板类型定义
 */

export interface DeviceType {
  text: string
  value: string
}

export type ConfigProperty = {
  property: string;
  name: string;
  description: string;
  type: {
    name: string;
    id: string;
    type: string;
    elements?: any[];
    expands?: any;
    trueText?: string;
    trueValue?: any;
    falseText?: string;
    falseValue?: any;
  };
  scopes: any[];
};

export type ConfigMetadata = {
  name?: string;
  description?: string;
  scopes?: any[];
  properties: ConfigProperty[];
};

export interface TemplateItem {
  id: string
  name: string
  photoUrl?: string
  documentUrl?: string
  document?: string
  describe?: string
  description?: string
  i18nMessages?: Record<string, Record<string, string>>
  i18nName?: string
  i18nDescription?: string
  classifiedId?: string
  classifiedName?: string
  deviceType?: DeviceType
  accessId?: string
  accessName?: string
  accessProvider?: string
  messageProtocol?: string
  transportProtocol?: string
  protocolName?: string
  metadata: string
  configuration?: Record<string, any>
  storePolicy?: string
  state: {
    value: string;
    text: string;
  };
  creatorId?: string
  creatorName?: string
  createTime?: number
  modifierId?: string
  modifierName?: string
  modifyTime?: number
}

export interface TemplateTagItem {
  id: string
  name: string
}

export interface TemplateDetailItem extends TemplateItem {
  tags?: TemplateTagItem[]
}

export interface QueryParams {
  pageIndex: number
  pageSize: number
  terms?: Array<{
    column: string
    value: any
    termType?: string
  }>
  sorts?: Array<{
    name: string
    order: 'asc' | 'desc'
  }>
}

export interface TemplateFormData {
  id?: string
  name: string
  description?: string
  documentUrl?: string
  document?: string
  classifiedId?: string
  metadata?: string
  accessId?: string
  accessProvider?: string
  transportProtocol?: string
}
