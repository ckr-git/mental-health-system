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
}
