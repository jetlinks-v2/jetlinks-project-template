import { request } from '@jetlinks-web/core'

export type TenantDomainCertificateMode = 'uploaded' | 'automatic'
export interface TenantDomainSaveRequest {
  domain: string
  certificateMode: TenantDomainCertificateMode
}

export type ProjectTenantDomainState =
    | 'pending_dns'
    | 'awaiting_upload'
    | 'issuing'
    | 'awaiting_certificate'
    | 'registering'
    | 'ready'
    | 'failed'
    | 'deleting'

export interface ProjectTenantDomain {
  domain: string
  certificateMode: TenantDomainCertificateMode
  state: ProjectTenantDomainState
}

export interface ProjectTenantDomainWorkflow {
  domain: string
  state: ProjectTenantDomainState
  cnameValidated: boolean
  certificateUploaded: boolean
  effectivenessTested: boolean
}

export interface ProjectTenantDomainCapability {
  enabled: boolean
  providerAvailable: boolean
  cnameTarget?: string
  supportedCertificateModes: TenantDomainCertificateMode[]
}

export interface ProjectTenantDomainSaveRequest {
  domain: string
  certificateMode: TenantDomainCertificateMode
}


export const getTenantDomain = () => request.get('/tenant/domain')
export const getTenantDomainWorkflow = () => request.get('/tenant/domain/_workflow')
export const getTenantDomainCapability = () => request.get('/tenant/domain/capability')
export const saveTenantDomain = (data: TenantDomainSaveRequest) => request.put('/tenant/domain', data)
export const validateTenantDomainCname = (domain: string) => request.post('/tenant/domain/_validate-cname', { domain })
export const retryTenantDomain = () => request.post('/tenant/domain/_retry')
export const testTenantDomain = () => request.post('/tenant/domain/_test')
export const deleteTenantDomain = () => request.remove('/tenant/domain')
export const uploadTenantDomainCertificate = (certificate: File, privateKey: File) => {
  const form = new FormData()
  form.append('certificate', certificate, certificate.name)
  form.append('privateKey', privateKey, privateKey.name)
  return request.post('/tenant/domain/_upload', form)
}
