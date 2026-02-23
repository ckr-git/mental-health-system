import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export const roomApi = {
  getDecorations: () =>
    request.get<any, ApiResponse<any[]>>('/patient/room/decorations'),

  getConfigs: () =>
    request.get<any, ApiResponse<any[]>>('/patient/room/configs'),

  getRecommended: () =>
    request.get<any, ApiResponse<any[]>>('/patient/room/recommended'),

  checkUnlock: () =>
    request.post<any, ApiResponse<any>>('/patient/room/check-unlock'),

  addToRoom: (data: { decorationType: string; x: number; y: number }) =>
    request.post<any, ApiResponse<string>>('/patient/room/add', data),

  updatePosition: (id: number, data: { x?: number; y?: number; z?: number; scale?: number; rotation?: number }) =>
    request.put<any, ApiResponse<string>>(`/patient/room/position/${id}`, data),

  removeDecoration: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/patient/room/${id}`),

  interact: (id: number) =>
    request.post<any, ApiResponse<any>>(`/patient/room/interact/${id}`)
}

export const themeApi = {
  getConfig: () =>
    request.get<any, ApiResponse<any>>('/patient/theme/config'),

  checkUnlock: () =>
    request.post<any, ApiResponse<any>>('/patient/theme/check-unlock'),

  switchTheme: (themeName: string) =>
    request.post<any, ApiResponse<string>>('/patient/theme/switch', { themeName }),

  getUnlockedThemes: () =>
    request.get<any, ApiResponse<any>>('/patient/theme/unlocked'),

  toggleLight: () =>
    request.post<any, ApiResponse<any>>('/patient/theme/toggle-light'),

  updateSettings: (settings: any) =>
    request.put<any, ApiResponse<string>>('/patient/theme/settings', settings)
}
