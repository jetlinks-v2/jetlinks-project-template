/**
 * 产品模板API
 */
import { request } from '@jetlinks-web/core'
import type { TemplateItem } from '../views/device/Template/typings'

/**
 * 查询模板列表（分页）
 */
export const queryTemplateList = (data: any) =>
  request.post('/device/template/_query', data)

export const queryTemplateDetailList = (data: any) =>
  request.post('/device/template/detail/_query', data)

export const queryTemplateDetailListNoPaging = (data: any) =>
  request.post('/device/template/detail/_query/no-paging', data)

/**
 * 查询模板列表（不分页）
 */
export const queryNoPaging = (data: any) =>
  request.post('/device/template/_query/no-paging?paging=false', data)

/**
 * 根据ID获取模板详情
 */
export const getTemplateDetail = (id: string) =>
  request.get<TemplateItem>(`/device/template/${id}`)

/**
 * 创建模板
 */
export const createTemplate = (data: any) =>
  request.post('/device/template', data)

/**
 * 更新模板
 */
export const updateTemplate = (data: any) =>
  request.put(`/device/template/${data.id}`, data)

/**
 * 删除模板
 */
export const deleteTemplate = (id: string) =>
  request.remove(`/device/template/${id}`)

/**
 * 启用模板
 */
export const deployTemplate = (id: string) =>
  request.put(`/device/template/${id}`, { id, state: "enabled" })

/**
 * 禁用模板
 */
export const undeployTemplate = (id: string) =>
  request.put(`/device/template/${id}`, { id, state: "disabled" })

/**
 * 基于模板创建产品
 */
export const createProductFromTemplate = (templateId: string, productData: any) =>
  request.post('/device-product', { ...productData, templateId })

/**
 * 保存模板物模型
 */
export const saveTemplateMetadata = (data: Record<string, unknown>) =>
  request.patch('/device/template', data)

/**
 * 更新模板文档内容
 */
export const updateTemplateDocument = (id: string, document: string) =>
  request.put(`/device/template/${id}`, { id, document })
