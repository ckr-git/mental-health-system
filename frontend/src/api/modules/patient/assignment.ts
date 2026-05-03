import request from '@/utils/request'

// ===== 评估任务 =====

export const getMyPendingAssignments = () =>
  request.get('/patient/assessment-assignments/pending')

export const getMyAssignmentHistory = (params: { pageNum?: number; pageSize?: number }) =>
  request.get('/patient/assessment-assignments', { params })

export const linkSessionToAssignment = (assignmentId: number, sessionId: number) =>
  request.post(`/patient/assessment-assignments/${assignmentId}/link-session`, { sessionId })

// ===== 医生端 =====

export const assignAssessment = (patientId: number, data: {
  scaleCode: string; isBaseline?: boolean; treatmentPlanId?: number; dueAt?: string
}) => request.post(`/doctor/patients/${patientId}/assessment-assignments`, data)

export const reviewAssignment = (assignmentId: number, note: string) =>
  request.post(`/doctor/assessment-assignments/${assignmentId}/review`, { note })

export const getPatientAssignments = (patientId: number, params: { pageNum?: number; pageSize?: number }) =>
  request.get(`/doctor/patients/${patientId}/assessment-assignments`, { params })

export const getPatientBaseline = (patientId: number, scaleCode: string) =>
  request.get(`/doctor/patients/${patientId}/assessment-baseline`, { params: { scaleCode } })

export const getDimensionTrend = (patientId: number, scaleCode: string, dimensionCode: string, limit = 10) =>
  request.get(`/doctor/patients/${patientId}/assessment-dimension-trend`, {
    params: { scaleCode, dimensionCode, limit }
  })

export const triggerProtocol = (patientId: number, data: {
  triggerCondition: string; sourceEventType?: string; sourceEventId?: number
}) => request.post(`/doctor/patients/${patientId}/trigger-protocol`, data)
