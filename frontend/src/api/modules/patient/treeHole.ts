import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export const treeHoleApi = {
  getActive: () =>
    request.get<any, ApiResponse<any>>('/patient/tree-hole/active'),

  canViewArchive: () =>
    request.get<any, ApiResponse<any>>('/patient/tree-hole/can-view-archive'),

  getArchive: () =>
    request.get<any, ApiResponse<any>>('/patient/tree-hole/archive'),

  add: (data: any) =>
    request.post<any, ApiResponse<any>>('/patient/tree-hole/add', data),

  delete: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/patient/tree-hole/delete/${id}`),

  view: (id: number) =>
    request.get<any, ApiResponse<any>>(`/patient/tree-hole/view/${id}`)
}
