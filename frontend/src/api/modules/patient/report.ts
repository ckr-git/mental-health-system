import request from '@/utils/request'
import type { AssessmentReport, ApiResponse, PageResult } from '@/types'

export const reportApi = {
  getList: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<AssessmentReport>>>('/patient/reports', { params }),

  getDetail: (id: number) =>
    request.get<any, ApiResponse<AssessmentReport>>(`/patient/reports/${id}`),

  getRecent: (limit: number = 5) =>
    request.get<any, ApiResponse<AssessmentReport[]>>('/patient/reports/recent', { params: { limit } })
}
