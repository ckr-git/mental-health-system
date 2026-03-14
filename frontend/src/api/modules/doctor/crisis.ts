import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface CrisisAlertDTO {
  id: number
  userId: number
  doctorId?: number
  riskEventId: number
  alertLevel: string
  alertStatus: string
  title: string
  summary: string
  evidenceJson?: string
  slaDeadline: string
  acknowledgedBy?: number
  acknowledgedAt?: string
  resolvedAt?: string
  resolutionNote?: string
  createTime: string
}

export const crisisApi = {
  getAlerts: (params: { pageNum?: number; pageSize?: number; status?: string; level?: string }) =>
    request.get<any, ApiResponse<PageResult<CrisisAlertDTO>>>('/doctor/crisis-alerts', { params }),

  getAlert: (id: number) =>
    request.get<any, ApiResponse<CrisisAlertDTO>>(`/doctor/crisis-alerts/${id}`),

  acknowledge: (id: number, note?: string) =>
    request.post<any, ApiResponse<void>>(`/doctor/crisis-alerts/${id}/acknowledge`, { note }),

  resolve: (id: number, note?: string) =>
    request.post<any, ApiResponse<void>>(`/doctor/crisis-alerts/${id}/resolve`, { note }),
}
