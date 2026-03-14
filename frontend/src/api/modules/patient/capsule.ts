import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export const capsuleApi = {
  create: (data: { letterType: string; title?: string; content: string; unlockType: string; unlockDate?: string; unlockConditions?: string }) =>
    request.post<any, ApiResponse<string>>('/patient/time-capsule/create', data),

  getList: (params: { pageNum: number; pageSize: number; status?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/patient/time-capsule/list', { params }),

  getDetail: (id: number) =>
    request.get<any, ApiResponse<any>>(`/patient/time-capsule/detail/${id}`),

  unlock: (id: number) =>
    request.post<any, ApiResponse<any>>(`/patient/time-capsule/unlock/${id}`),

  getUnlockable: () =>
    request.get<any, ApiResponse<any[]>>('/patient/time-capsule/unlockable'),

  delete: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/patient/time-capsule/${id}`),

  reply: (id: number, data: { replyContent: string }) =>
    request.post<any, ApiResponse<any>>(`/patient/time-capsule/reply/${id}`, data),

  getRecommend: () =>
    request.get<any, ApiResponse<any>>('/patient/time-capsule/recommend')
}
