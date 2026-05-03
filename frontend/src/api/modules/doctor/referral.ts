import request from '@/utils/request'

// ===== 转诊 =====
export const initiateReferral = (data: any) =>
  request.post('/doctor/referrals', data)

export const getReferrals = (role: 'from' | 'to' = 'from', status?: string) =>
  request.get('/doctor/referrals', { params: { role, status } })

export const acceptReferral = (id: number) =>
  request.post(`/doctor/referrals/${id}/accept`)

export const rejectReferral = (id: number, reason: string) =>
  request.post(`/doctor/referrals/${id}/reject`, { reason })

export const createHandoff = (referralId: number, data: any) =>
  request.post(`/doctor/referrals/${referralId}/handoffs`, data)

export const acknowledgeHandoff = (handoffId: number) =>
  request.post(`/doctor/referral-handoffs/${handoffId}/acknowledge`)

export const completeReferral = (id: number) =>
  request.post(`/doctor/referrals/${id}/complete`)

// ===== 护理团队 =====
export const getCareTeam = (patientId: number) =>
  request.get(`/doctor/patients/${patientId}/care-team`)

export const addCareTeamMember = (patientId: number, data: { doctorId: number; memberRoleCode: string }) =>
  request.post(`/doctor/patients/${patientId}/care-team`, data)

export const removeCareTeamMember = (memberId: number, reason?: string) =>
  request.delete(`/doctor/care-team-members/${memberId}`, { params: { reason } })
