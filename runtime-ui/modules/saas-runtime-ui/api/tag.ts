import { request } from '@jetlinks-web/core'

// 标签分类
export const queryTagCategoryNoPaging = (data: any) =>
  request.post(`/tag/category/_query/no-paging`, data)

export const saveTagCategory = (data: any) => request.patch(`/tag/category`, data)

export const deleteTagCategory = (id: string) => request.remove(`/tag/category/${id}`)

// 标签
export const queryTagTree = (data: any) =>
  request.post(`/tag/_query/tree`, {
    paging: false,
    sorts: [{ name: 'sortIndex', order: 'asc' }],
    ...data,
  })

export const saveTag = (data: any) => request.patch(`/tag`, data)

export const deleteTag = (id: string) => request.remove(`/tag/${id}`)

// 查询目标对象已绑定标签（单个）
export const getTagsByTarget = (targetType: string, targetId: string) =>
  request.get(`/tag/target/${targetType}/${targetId}`)

// 保存绑定（覆盖）
export const bindTagsByTarget = (targetType: string, targetId: string, tagIds: string[]) =>
  request.put(`/tag/target/${targetType}/${targetId}`, tagIds)
