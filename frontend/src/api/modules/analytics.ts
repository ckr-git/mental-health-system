import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface OutcomeSnapshotItem {
  id: number
  patientId: number
  snapshotDate: string
  snapshotType: string
  phq9Score?: number
  gad7Score?: number
  phq9Severity?: string
  gad7Severity?: string
  avgMoodScore?: number
  avgSleepQuality?: number
  avgStressLevel?: number
  avgEnergyLevel?: number
  diaryCount: number
  appointmentCount: number
  appointmentAttended: number
  meditationMinutes: number
  assessmentCount: number
  goalsTotal: number
  goalsAchieved: number
  avgGoalProgress: number
  riskEventsCount: number
  crisisAlertsCount: number
}

export const analyticsApi = {
  // Patient
  getMyOutcomeSummary: () =>
    request.get<any, ApiResponse<any>>('/patient/outcomes/summary'),

  getMyOutcomeHistory: () =>
    request.get<any, ApiResponse<OutcomeSnapshotItem[]>>('/patient/outcomes/history'),

  // Doctor
  getPatientOutcome: (patientId: number) =>
    request.get<any, ApiResponse<any>>(`/doctor/patients/${patientId}/outcomes`),

  generateSnapshot: (patientId: number, snapshotType = 'MONTHLY') =>
    request.post<any, ApiResponse<OutcomeSnapshotItem>>(`/doctor/patients/${patientId}/outcomes/snapshot`, null, {
      params: { snapshotType }
    }),

  getPatientOutcomeHistory: (patientId: number) =>
    request.get<any, ApiResponse<OutcomeSnapshotItem[]>>(`/doctor/patients/${patientId}/outcomes/history`),

  getDoctorPerformance: () =>
    request.get<any, ApiResponse<any>>('/doctor/performance'),

  // Admin
  getSystemAnalytics: () =>
    request.get<any, ApiResponse<any>>('/admin/analytics'),

  // ===== Phase 3: 多维分析 =====

  // 患者轨迹(医生端)
  getPatientTrajectory: (patientId: number, from: string, to: string) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/patients/${patientId}/trajectory`, { params: { from, to } }),

  // 医生工作负载(管理端)
  getDoctorWorkload: (doctorId: number, from: string, to: string) =>
    request.get<any, ApiResponse<any[]>>(`/admin/analytics/doctors/${doctorId}/workload`, { params: { from, to } }),

  // 手动触发聚合
  triggerAnalyticsJob: (data: { jobScopeCode: string; targetDate: string }) =>
    request.post<any, ApiResponse<number>>('/admin/analytics/jobs', data),

  // 日记洞察(患者端)
  getDiaryInsight: (diaryId: number) =>
    request.get<any, ApiResponse<any>>(`/patient/mood-diaries/${diaryId}/insights`),

  // 日记洞察(医生端)
  getMoodInsightSummary: (patientId: number, days = 14) =>
    request.get<any, ApiResponse<any>>(`/doctor/patients/${patientId}/mood-insights/summary`, { params: { days } }),

  getMoodInsightTimeline: (patientId: number, from: string, to: string) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/patients/${patientId}/mood-insights/timeline`, { params: { from, to } }),
}
