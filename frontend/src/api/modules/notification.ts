import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface UserNotificationDTO {
  id: number
  category: string
  priority: string
  title: string
  content: string
  actionType?: string
  actionPayload?: string
  sourceType?: string
  sourceId?: number
  readStatus: number
  createTime: string
}

export const notificationApi = {
  getList: (params: { pageNum?: number; pageSize?: number; category?: string; readStatus?: number }) =>
    request.get<any, ApiResponse<PageResult<UserNotificationDTO>>>('/notifications', { params }),

  markRead: (id: number) =>
    request.post<any, ApiResponse<void>>(`/notifications/${id}/read`),

  markAllRead: () =>
    request.post<any, ApiResponse<void>>('/notifications/read-all'),

  getUnreadCount: () =>
    request.get<any, ApiResponse<number>>('/notifications/unread-count'),
}
