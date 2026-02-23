import request from '@/utils/request'
import type { LoginForm, RegisterForm, UserInfo, ApiResponse } from '@/types'

export const authApi = {
  login: (data: LoginForm) =>
    request.post<any, ApiResponse<{ token: string; userInfo: UserInfo }>>('/auth/login', data),

  register: (data: RegisterForm) =>
    request.post<any, ApiResponse<string>>('/auth/register', data),

  logout: () =>
    request.post<any, ApiResponse<string>>('/auth/logout'),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post<any, ApiResponse<string>>('/user/change-password', data),

  sendCode: (phone: string) =>
    request.post<any, ApiResponse<string>>('/auth/send-code', { phone }),

  sendResetCode: (username: string) =>
    request.post<any, ApiResponse<string>>('/auth/send-code', { username }),

  changePhone: (data: { newPhone: string; code: string }) =>
    request.post<any, ApiResponse<string>>('/user/change-phone', data),

  resetPassword: (data: { username: string; code: string; newPassword: string }) =>
    request.post<any, ApiResponse<string>>('/auth/reset-password', data)
}
