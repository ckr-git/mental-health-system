import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export const chatApi = {
  getChatList: () =>
    request.get<any, ApiResponse<any[]>>('/patient/chat/list'),

  createChat: (doctorId: string) =>
    request.post<any, ApiResponse<any>>('/patient/chat/create', { doctorId }),

  getMessages: (sessionId: number, params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>(`/patient/chat/messages/${sessionId}`, { params }),

  sendMessage: (data: { sessionId: number; targetUserId: number; content: string; type: string }) =>
    request.post<any, ApiResponse<any>>('/patient/chat/send', data),

  markAsRead: (sessionId: number) =>
    request.post<any, ApiResponse<string>>(`/patient/chat/read/${sessionId}`),

  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, ApiResponse<string>>('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
