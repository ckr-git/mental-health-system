import request from '@/utils/request'
import type { UserInfo, ApiResponse, PageResult } from '@/types'

export const userApi = {
  getProfile: () =>
    request.get<any, ApiResponse<UserInfo>>('/user/profile'),

  updateProfile: (data: Partial<UserInfo>) =>
    request.put<any, ApiResponse<string>>('/user/profile', data),

  getStats: () =>
    request.get<any, ApiResponse<any>>('/user/stats'),

  uploadAvatar: (formData: FormData) =>
    request.post<any, ApiResponse<{ url: string }>>('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),

  getDoctors: (params?: { pageNum?: number; pageSize?: number; specialty?: string; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<UserInfo>>>('/patient/doctors', { params }),

  getMyDoctor: () =>
    request.get<any, ApiResponse<any>>('/patient/my-doctor'),

  getMyConsultation: () =>
    request.get<any, ApiResponse<any>>('/patient/my-consultation'),

  getConsultationHistory: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/patient/consultation-history', { params }),

  startConsultation: (data: { message: string }) =>
    request.post<any, ApiResponse<any>>('/patient/consultation/start', data)
}
