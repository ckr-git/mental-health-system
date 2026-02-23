import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export const doctorChatApi = {
  getChatList: () =>
    request.get<any, ApiResponse<any[]>>('/doctor/chat/list'),

  getMessages: (sessionId: number, params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>(`/doctor/chat/messages/${sessionId}`, { params }),

  sendMessage: (data: { sessionId: number; targetUserId: number; content: string; type: string }) =>
    request.post<any, ApiResponse<any>>('/doctor/chat/send', data),

  markAsRead: (sessionId: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/chat/read/${sessionId}`),

  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, ApiResponse<string>>('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
