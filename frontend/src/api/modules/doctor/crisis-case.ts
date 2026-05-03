import request from '@/utils/request'

// ===== 危机案例 =====

export const getDoctorCrisisCases = (params: { status?: string; pageNum?: number; pageSize?: number }) =>
  request.get('/doctor/crisis-cases', { params })

export const getCrisisCaseDetail = (caseId: number) =>
  request.get(`/doctor/crisis-cases/${caseId}`)

export const getEscalationSteps = (caseId: number) =>
  request.get(`/doctor/crisis-cases/${caseId}/escalation-steps`)

export const getContactAttempts = (caseId: number) =>
  request.get(`/doctor/crisis-cases/${caseId}/contact-attempts`)

export const triageCase = (caseId: number, data: { triageLevel: string; assignDoctorId?: number; note?: string }) =>
  request.post(`/doctor/crisis-cases/${caseId}/triage`, data)

export const transitionCaseStatus = (caseId: number, data: { targetStatus: string; note?: string }) =>
  request.post(`/doctor/crisis-cases/${caseId}/transition`, data)

export const recordContactAttempt = (caseId: number, data: {
  target: string; channel: string; contactName?: string;
  contactInfo?: string; status: string; note?: string
}) => request.post(`/doctor/crisis-cases/${caseId}/contact-attempts`, data)

// ===== 安全计划 =====

export const getPatientSafetyPlan = (patientId: number) =>
  request.get(`/doctor/patients/${patientId}/safety-plan`)

export const createSafetyPlan = (patientId: number, data: any) =>
  request.post(`/doctor/patients/${patientId}/safety-plan`, data)

// ===== 患者端 =====

export const getMyCreisisCases = (params: { pageNum?: number; pageSize?: number }) =>
  request.get('/patient/crisis-cases', { params })

export const getMySafetyPlan = () =>
  request.get('/patient/safety-plan')
