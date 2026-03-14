import request from '@/utils/request'
import type { UserInfo, MentalResource, ApiResponse, PageResult } from '@/types'

export const adminApi = {
  // 用户管理
  getUsers: (params: { pageNum: number; pageSize: number; role?: string; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<UserInfo>>>('/admin/users', { params }),
  toggleUserStatus: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/users/${id}/toggle-status`),
  deleteUser: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/users/${id}`),
  getUserStatistics: () =>
    request.get<any, ApiResponse<Record<string, number>>>('/admin/users/statistics'),

  // 医生管理
  approveDoctor: (id: number, approve: boolean) =>
    request.post<any, ApiResponse<string>>(`/admin/doctors/${id}/approve`, null, { params: { approve } }),
  updateDoctor: (id: number, data: Partial<UserInfo>) =>
    request.put<any, ApiResponse<string>>(`/admin/doctors/${id}`, data),
  getDoctorStatistics: (id: number) =>
    request.get<any, ApiResponse<Record<string, any>>>(`/admin/doctors/${id}/statistics`),

  // 资源管理
  addResource: (data: Partial<MentalResource>) =>
    request.post<any, ApiResponse<string>>('/admin/resources', data),
  updateResource: (data: MentalResource) =>
    request.put<any, ApiResponse<string>>(`/admin/resources/${data.id}`, data),
  deleteResource: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/resources/${id}`),
  getResourceStatistics: () =>
    request.get<any, ApiResponse<any>>('/admin/resources/statistics'),
  getResources: (params: any) =>
    request.get<any, ApiResponse<any>>('/admin/resources', { params }),
  updateResourceStatus: (id: number, data: { status: number }) =>
    request.put<any, ApiResponse<string>>(`/admin/resources/${id}/status`, data),

  // 消息中心
  getMessageStatistics: () =>
    request.get<any, ApiResponse<Record<string, any>>>('/admin/messages/statistics'),
  getNotifications: (params: { pageNum: number; pageSize: number; notificationType?: string; sendStatus?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/messages/notifications', { params }),
  createNotification: (data: any) =>
    request.post<any, ApiResponse<string>>('/admin/messages/notifications', data),
  updateNotification: (id: number, data: any) =>
    request.put<any, ApiResponse<string>>(`/admin/messages/notifications/${id}`, data),
  deleteNotification: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/messages/notifications/${id}`),
  sendNotification: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/notifications/${id}/send`),

  // 用户反馈
  getFeedback: (params: { pageNum: number; pageSize: number; feedbackType?: string; status?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/messages/feedback', { params }),
  getFeedbackDetail: (id: number) =>
    request.get<any, ApiResponse<any>>(`/admin/messages/feedback/${id}`),
  replyFeedback: (id: number, reply: string) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/feedback/${id}/reply`, { reply }),
  updateFeedbackStatus: (id: number, status: number) =>
    request.put<any, ApiResponse<string>>(`/admin/messages/feedback/${id}/status`, { status }),
  deleteFeedback: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/messages/feedback/${id}`),

  // 系统警报
  getAlerts: (params: { pageNum: number; pageSize: number; alertType?: string; level?: string; status?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/messages/alerts', { params }),
  getAlertDetail: (id: number) =>
    request.get<any, ApiResponse<any>>(`/admin/messages/alerts/${id}`),
  createAlert: (data: any) =>
    request.post<any, ApiResponse<string>>('/admin/messages/alerts', data),
  handleAlert: (id: number, handleNote: string, status: number) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/alerts/${id}/handle`, { handleNote, status }),
  deleteAlert: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/messages/alerts/${id}`),

  // 公告管理
  getAnnouncements: (params: { pageNum: number; pageSize: number; announcementType?: string; status?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/messages/announcements', { params }),
  getAnnouncementDetail: (id: number) =>
    request.get<any, ApiResponse<any>>(`/admin/messages/announcements/${id}`),
  createAnnouncement: (data: any) =>
    request.post<any, ApiResponse<string>>('/admin/messages/announcements', data),
  updateAnnouncement: (id: number, data: any) =>
    request.put<any, ApiResponse<string>>(`/admin/messages/announcements/${id}`, data),
  publishAnnouncement: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/announcements/${id}/publish`),
  unpublishAnnouncement: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/announcements/${id}/unpublish`),
  toggleAnnouncementPin: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/messages/announcements/${id}/toggle-pin`),
  deleteAnnouncement: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/messages/announcements/${id}`),

  // 预约管理
  getAppointmentStatistics: () =>
    request.get<any, ApiResponse<Record<string, any>>>('/admin/appointments/statistics'),
  getAppointments: (params: { pageNum: number; pageSize: number; doctorId?: number; patientId?: number; status?: number; appointmentType?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/appointments', { params }),
  getAppointmentDetail: (id: number) =>
    request.get<any, ApiResponse<any>>(`/admin/appointments/${id}`),
  createAppointment: (data: any) =>
    request.post<any, ApiResponse<string>>('/admin/appointments', data),
  updateAppointment: (id: number, data: any) =>
    request.put<any, ApiResponse<string>>(`/admin/appointments/${id}`, data),
  confirmAppointment: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/appointments/${id}/confirm`),
  cancelAppointment: (id: number, reason: string) =>
    request.post<any, ApiResponse<string>>(`/admin/appointments/${id}/cancel`, { reason }),
  completeAppointment: (id: number) =>
    request.post<any, ApiResponse<string>>(`/admin/appointments/${id}/complete`),
  deleteAppointment: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/appointments/${id}`),
  getDoctorAppointmentStatistics: (doctorId: number) =>
    request.get<any, ApiResponse<Record<string, any>>>(`/admin/appointments/doctor/${doctorId}/statistics`),
  getPatientAppointmentStatistics: (patientId: number) =>
    request.get<any, ApiResponse<Record<string, any>>>(`/admin/appointments/patient/${patientId}/statistics`),
  getAppointmentTrend: (days: number = 7) =>
    request.get<any, ApiResponse<Record<string, any>>>('/admin/appointments/trend', { params: { days } }),

  // 系统设置
  getAllSettings: () =>
    request.get<any, ApiResponse<any[]>>('/admin/settings'),
  getSettingsByGroup: () =>
    request.get<any, ApiResponse<Record<string, any[]>>>('/admin/settings/grouped'),
  getSettingByKey: (key: string) =>
    request.get<any, ApiResponse<any>>(`/admin/settings/${key}`),
  updateSetting: (key: string, value: string) =>
    request.put<any, ApiResponse<string>>(`/admin/settings/${key}`, { value }),
  createSetting: (data: any) =>
    request.post<any, ApiResponse<string>>('/admin/settings', data),
  deleteSetting: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/settings/${id}`),

  // 患者分配审核
  getPendingAssignments: (params: { pageNum: number; pageSize: number; requestType?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/patient-assignments/pending', { params }),
  approveAssignment: (requestId: number, data?: { adminNote?: string }) =>
    request.post<any, ApiResponse<string>>(`/admin/patient-assignments/${requestId}/approve`, data || {}),
  rejectAssignment: (requestId: number, data: { adminNote: string }) =>
    request.post<any, ApiResponse<string>>(`/admin/patient-assignments/${requestId}/reject`, data)
}
