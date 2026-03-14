import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export const commentApi = {
  add: (data: { diaryId: number; commentType: string; content: string }) =>
    request.post<any, ApiResponse<string>>('/patient/mood-comment/add', data),

  getList: (diaryId: number, params?: { pageNum?: number; pageSize?: number }) =>
    request.get<any, ApiResponse<any[]>>(`/patient/mood-comment/list/${diaryId}`, { params }),

  like: (commentId: number, interactions: string[]) =>
    request.put<any, ApiResponse<string>>(`/patient/mood-comment/interaction/${commentId}`, { interactions }),

  delete: (commentId: number) =>
    request.delete<any, ApiResponse<string>>(`/patient/mood-comment/${commentId}`)
}
