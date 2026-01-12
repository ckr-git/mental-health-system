import request from '@/utils/request'
import type {
  LoginForm,
  RegisterForm,
  UserInfo,
  SymptomRecord,
  MentalResource,
  AssessmentReport,
  AIConversation,
  ApiResponse,
  PageResult
} from '@/types'

// 认证相关
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

  changePhone: (data: { newPhone: string; code: string }) =>
    request.post<any, ApiResponse<string>>('/user/change-phone', data)
}

// 用户相关
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

  getDoctors: (params?: { pageNum?: number; pageSize?: number }) =>
    request.get<any, ApiResponse<PageResult<UserInfo>>>('/patient/doctors', { params }),

  // 我的医生
  getMyDoctor: () =>
    request.get<any, ApiResponse<any>>('/patient/my-doctor'),

  getMyConsultation: () =>
    request.get<any, ApiResponse<any>>('/patient/my-consultation'),

  // 咨询相关
  getConsultationHistory: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/patient/consultation-history', { params }),

  startConsultation: (data: { message: string }) =>
    request.post<any, ApiResponse<any>>('/patient/consultation/start', data)
}

// 情绪日记相关（新接口）
export const symptomApi = {
  addRecord: (data: SymptomRecord) => 
    request.post<any, ApiResponse<string>>('/patient/mood-diary/add', data),
  
  getList: (params: { pageNum: number; pageSize: number }) => 
    request.get<any, ApiResponse<PageResult<SymptomRecord>>>('/patient/mood-diary/list', { params }),
  
  getDetail: (id: number) => 
    request.get<any, ApiResponse<SymptomRecord>>(`/patient/mood-diary/detail/${id}`),
  
  updateRecord: (data: SymptomRecord) => 
    request.put<any, ApiResponse<string>>(`/patient/mood-diary/status/${data.id}`, { status: data.status }),
  
  deleteRecord: (id: number) => 
    request.delete<any, ApiResponse<string>>(`/patient/mood-comment/${id}`),
  
  getRecent: (limit: number = 7) => 
    request.get<any, ApiResponse<SymptomRecord[]>>('/patient/mood-diary/recent', { params: { limit } })
}

// 心情留言相关
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

// 时光信箱相关
export const capsuleApi = {
  create: (data: { letterType: string; title?: string; content: string; unlockType: string; unlockTime?: string; unlockConditions?: string[] }) => 
    request.post<any, ApiResponse<string>>('/patient/time-capsule/create', data),
  
  getList: (params: { pageNum: number; pageSize: number; status?: string }) => 
    request.get<any, ApiResponse<PageResult<any>>>('/patient/time-capsule/list', { params }),
  
  getDetail: (id: number) => 
    request.get<any, ApiResponse<any>>(`/patient/time-capsule/detail/${id}`),
  
  unlock: (id: number) => 
    request.post<any, ApiResponse<any>>(`/patient/time-capsule/unlock/${id}`),
  
  getUnlockable: () => 
    request.get<any, ApiResponse<any[]>>('/patient/time-capsule/unlockable'),
  
  delete: (id: number) => 
    request.delete<any, ApiResponse<string>>(`/patient/time-capsule/${id}`)
}

// 心理资源相关
export const resourceApi = {
  getList: (params: { pageNum: number; pageSize: number; type?: string; category?: string; keyword?: string }) => 
    request.get<any, ApiResponse<PageResult<MentalResource>>>('/public/resources', { params }),
  
  getDetail: (id: number) => 
    request.get<any, ApiResponse<MentalResource>>(`/public/resources/${id}`),
  
  getHot: (limit: number = 10) => 
    request.get<any, ApiResponse<MentalResource[]>>('/public/resources/hot', { params: { limit } }),
  
  like: (id: number) => 
    request.post<any, ApiResponse<string>>(`/patient/resources/${id}/like`),
  
  download: (id: number) => 
    request.post<any, ApiResponse<string>>(`/patient/resources/${id}/download`)
}

// 评估报告相关
export const reportApi = {
  getList: (params: { pageNum: number; pageSize: number }) => 
    request.get<any, ApiResponse<PageResult<AssessmentReport>>>('/patient/reports', { params }),
  
  getDetail: (id: number) => 
    request.get<any, ApiResponse<AssessmentReport>>(`/patient/reports/${id}`),
  
  getRecent: (limit: number = 5) => 
    request.get<any, ApiResponse<AssessmentReport[]>>('/patient/reports/recent', { params: { limit } })
}

// AI相关
export const aiApi = {
  ask: (question: string) => 
    request.post<any, ApiResponse<string>>('/patient/ai/ask', { question }),
  
  getConversations: (limit: number = 20) => 
    request.get<any, ApiResponse<AIConversation[]>>('/patient/ai/conversations', { params: { limit } }),
  
  submitFeedback: (conversationId: number, feedback: number) => 
    request.post<any, ApiResponse<string>>('/patient/ai/feedback', { conversationId, feedback }),
  
  generateReport: () => 
    request.post<any, ApiResponse<string>>('/patient/ai/generate-report')
}

// 推荐系统相关
export const recommendApi = {
  getResources: (limit: number = 10) => 
    request.get<any, ApiResponse<MentalResource[]>>('/patient/recommendations/resources', { params: { limit } }),
  
  recordBehavior: (resourceId: number, action: string, rating?: number) => 
    request.post<any, ApiResponse<string>>('/patient/recommendations/record-behavior', null, {
      params: { resourceId, action, rating }
    })
}

// 管理员相关
export const adminApi = {
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

  addResource: (data: Partial<MentalResource>) =>
    request.post<any, ApiResponse<string>>('/admin/resources/add', data),

  updateResource: (data: MentalResource) =>
    request.put<any, ApiResponse<string>>('/admin/resources/update', data),

  deleteResource: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/admin/resources/${id}`),

  // 消息中心管理
  getMessageStatistics: () =>
    request.get<any, ApiResponse<Record<string, any>>>('/admin/messages/statistics'),

  // 系统通知
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

  // 系统设置管理
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

  // 患者分配审核相关
  getPendingAssignments: (params: { pageNum: number; pageSize: number; requestType?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/admin/patient-assignments/pending', { params }),

  approveAssignment: (requestId: number, data?: { adminNote?: string }) =>
    request.post<any, ApiResponse<string>>(`/admin/patient-assignments/${requestId}/approve`, data || {}),

  rejectAssignment: (requestId: number, data: { adminNote: string }) =>
    request.post<any, ApiResponse<string>>(`/admin/patient-assignments/${requestId}/reject`, data)
}

// 医生相关
export const doctorApi = {
  // Dashboard
  getDashboardStatistics: () =>
    request.get<any, ApiResponse<Record<string, any>>>('/doctor/dashboard/statistics'),

  // Patients Management
  getPatients: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/patients', { params }),

  getPatientDetail: (patientId: number) =>
    request.get<any, ApiResponse<any>>(`/doctor/patients/${patientId}`),

  // 患者公海相关
  getPatientPool: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/patient-pool', { params }),

  claimPatient: (patientId: number, data: { reason: string }) =>
    request.post<any, ApiResponse<string>>(`/doctor/patient-pool/claim/${patientId}`, data),

  // 患者管理相关
  releasePatient: (patientId: number, data: { reason: string }) =>
    request.post<any, ApiResponse<string>>(`/doctor/patients/${patientId}/release`, data),

  // 认领/释放记录
  getDoctorRequests: (params: { pageNum: number; pageSize: number; requestType?: string; status?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/requests', { params }),

  // 在线咨询相关
  getConsultations: (params: { pageNum: number; pageSize: number; status?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/consultations', { params }),

  getConsultationDetail: (sessionId: number) =>
    request.get<any, ApiResponse<any>>(`/doctor/consultations/${sessionId}`),

  closeConsultation: (sessionId: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/consultations/${sessionId}/close`),

  // Assessment Reports
  getReports: (params: { pageNum: number; pageSize: number; patientId?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/reports', { params }),

  getReportDetail: (reportId: number) =>
    request.get<any, ApiResponse<AssessmentReport>>(`/doctor/reports/${reportId}`),

  createReport: (data: Partial<AssessmentReport>) =>
    request.post<any, ApiResponse<string>>('/doctor/reports', data),

  updateReport: (reportId: number, data: Partial<AssessmentReport>) =>
    request.put<any, ApiResponse<string>>(`/doctor/reports/${reportId}`, data),

  deleteReport: (reportId: number) =>
    request.delete<any, ApiResponse<string>>(`/doctor/reports/${reportId}`),

  // Appointments
  getAppointments: (params: { pageNum: number; pageSize: number; status?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/appointments', { params }),

  getRecentAppointments: () =>
    request.get<any, ApiResponse<any[]>>('/doctor/appointments/recent')
}

// 聊天相关
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

// 医生端聊天相关
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

// 房间装饰相关
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

// 主题相关
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
