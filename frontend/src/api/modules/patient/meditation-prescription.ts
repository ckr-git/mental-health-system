import request from '@/utils/request'

// ===== 冥想处方(医生端) =====
export const createMeditationPrescription = (patientId: number, data: {
  exerciseId: number; goalCode: string; frequencyCode?: string;
  sessionsPerWeek?: number; minutesPerSession?: number; treatmentPlanId?: number
}) => request.post(`/doctor/patients/${patientId}/meditation-prescriptions`, data)

export const activatePrescription = (id: number) =>
  request.post(`/doctor/meditation-prescriptions/${id}/activate`)

export const pausePrescription = (id: number) =>
  request.post(`/doctor/meditation-prescriptions/${id}/pause`)

export const completePrescription = (id: number) =>
  request.post(`/doctor/meditation-prescriptions/${id}/complete`)

export const getPatientPrescriptions = (patientId: number, status?: string) =>
  request.get(`/doctor/patients/${patientId}/meditation-prescriptions`, { params: { status } })

export const getMeditationEffectSummary = (patientId: number, days = 30) =>
  request.get(`/doctor/patients/${patientId}/meditation-effects/summary`, { params: { days } })

// ===== 冥想处方(患者端) =====
export const getMyPrescriptions = (status?: string) =>
  request.get('/patient/meditation/prescriptions', { params: { status } })

export const recordMeditationEffect = (sessionId: number, data: {
  prescriptionId?: number; preMoodScore?: number; postMoodScore?: number;
  preStressScore?: number; postStressScore?: number; perceivedBenefitScore?: number; note?: string
}) => request.post(`/patient/meditation/sessions/${sessionId}/effect`, data)

export const getMyEffects = (limit = 20) =>
  request.get('/patient/meditation/effects', { params: { limit } })
