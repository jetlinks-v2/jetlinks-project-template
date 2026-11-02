import { request } from '@jetlinks-web/core'

export interface TenantResourceDetailResponse {
  id?: string
  name?: string
  metrics?: unknown[]
  capabilities?: unknown[]
}

/** 查询当前项目租户已开通资源的额度与用量，项目上下文由请求层自动注入。 */
export const getCurrentProjectResourceUsage_api = (): Promise<TenantResourceDetailResponse[]> => (
  // 错误由页面统一展示，保留请求层的登录失效与身份验证处理。
  request.get('/tenant/me/limits', undefined, { hiddenError: true }) as Promise<TenantResourceDetailResponse[]>
)
