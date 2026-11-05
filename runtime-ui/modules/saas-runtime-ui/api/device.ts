import { request } from '@jetlinks-web/core'

export interface RegisterDevicePayload {
  scanToken: string
  productId: string
  device: {
    name: string
    description: string
  }
}

export type RegisterDeviceResult =
  | string
  | number
  | { id?: string }
  | Array<string | number | { id?: string }>

export const registerDevice = (data: RegisterDevicePayload) =>
  request.post<RegisterDeviceResult>(`/device/asset/access/_register`, data)

export type RegisterDeviceResponse = Awaited<ReturnType<typeof registerDevice>> & {
  message?: string
}
