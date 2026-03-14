import request from '@/utils/request'
import type { SymptomRecord, ApiResponse, PageResult } from '@/types'

export const symptomApi = {
  addRecord: (data: SymptomRecord) =>
    request.post<any, ApiResponse<string>>('/patient/mood-diary/add', data),

  getList: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<SymptomRecord>>>('/patient/mood-diary/list', { params }),

  getDetail: (id: number) =>
    request.get<any, ApiResponse<SymptomRecord>>(`/patient/mood-diary/detail/${id}`),

  updateRecord: (data: SymptomRecord) =>
    request.put<any, ApiResponse<string>>(`/patient/mood-diary/status/${data.id}`, { status: data.status }),

  deleteRecord: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/patient/mood-comment/${id}`),

  getRecent: (limit: number = 7) =>
    request.get<any, ApiResponse<SymptomRecord[]>>('/patient/mood-diary/recent', { params: { limit } })
}
