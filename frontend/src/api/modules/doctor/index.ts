import request from '@/utils/request'
import type { AssessmentReport, ApiResponse, PageResult } from '@/types'

export const doctorApi = {
  getDashboardStatistics: () =>
    request.get<any, ApiResponse<Record<string, any>>>('/doctor/dashboard/statistics'),

  getPatients: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/patients', { params }),

  getPatientDetail: (patientId: number) =>
    request.get<any, ApiResponse<any>>(`/doctor/patients/${patientId}`),

  getPatientPool: (params: { pageNum: number; pageSize: number; keyword?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/patient-pool', { params }),

  claimPatient: (patientId: number, data: { reason: string }) =>
    request.post<any, ApiResponse<string>>(`/doctor/patient-pool/claim/${patientId}`, data),

  releasePatient: (patientId: number, data: { reason: string }) =>
    request.post<any, ApiResponse<string>>(`/doctor/patients/${patientId}/release`, data),

  getDoctorRequests: (params: { pageNum: number; pageSize: number; requestType?: string; status?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/requests', { params }),

  getConsultations: (params: { pageNum: number; pageSize: number; status?: string }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/consultations', { params }),

  getConsultationDetail: (sessionId: number) =>
    request.get<any, ApiResponse<any>>(`/doctor/consultations/${sessionId}`),

  closeConsultation: (sessionId: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/consultations/${sessionId}/close`),

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

  getAppointments: (params: { pageNum: number; pageSize: number; status?: number }) =>
    request.get<any, ApiResponse<PageResult<any>>>('/doctor/appointments', { params }),

  getRecentAppointments: () =>
    request.get<any, ApiResponse<any[]>>('/doctor/appointments/recent')
}
