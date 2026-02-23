import request from '@/utils/request'
import type { MentalResource, ApiResponse, PageResult } from '@/types'

export const resourceApi = {
  getList: (params: { pageNum: number; pageSize: number; type?: string; category?: string; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<MentalResource>>>('/public/resources', { params }),

  getDetail: (id: number) =>
    request.get<any, ApiResponse<MentalResource>>(`/public/resources/${id}`),

  getHot: (limit: number = 10) =>
    request.get<any, ApiResponse<MentalResource[]>>('/public/resources/hot', { params: { limit } }),

  like: (id: number) =>
    request.post<any, ApiResponse<string>>(`/patient/resources/${id}/like`),

  download: (id: number) =>
    request.post<any, ApiResponse<string>>(`/patient/resources/${id}/download`)
}

export const recommendApi = {
  getResources: (limit: number = 10) =>
    request.get<any, ApiResponse<MentalResource[]>>('/patient/recommendations/resources', { params: { limit } }),

  recordBehavior: (resourceId: number, action: string, rating?: number) =>
    request.post<any, ApiResponse<string>>('/patient/recommendations/record-behavior', null, {
      params: { resourceId, action, rating }
    })
}
