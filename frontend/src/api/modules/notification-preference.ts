import request from '@/utils/request'

// ===== 通知偏好 =====

export const getNotificationPreferences = () =>
  request.get('/user/notification-preferences')

export const saveNotificationPreference = (data: {
  category: string; channelCode: string; enabled: boolean;
  quietStart?: string; quietEnd?: string; minPriority?: string; coalesceMinutes?: number
}) => request.post('/user/notification-preferences', data)

export const initDefaultPreferences = () =>
  request.post('/user/notification-preferences/init-defaults')

export const acknowledgeNotification = (notificationId: number) =>
  request.post(`/user/notifications/${notificationId}/acknowledge`)
