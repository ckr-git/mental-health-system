import request from '@/utils/request'

// ===== 候补Offer =====

export const getMyOffers = (status?: string) =>
  request.get('/patient/waitlist-offers', { params: { status } })

export const acceptOffer = (offerId: number) =>
  request.post(`/patient/waitlist-offers/${offerId}/accept`)

export const declineOffer = (offerId: number, reason: string) =>
  request.post(`/patient/waitlist-offers/${offerId}/decline`, { reason })

// ===== 到诊签到 =====

export const checkIn = (appointmentId: number) =>
  request.post(`/patient/appointments/${appointmentId}/check-in`)

// ===== 医生端 =====

export const markNoShow = (appointmentId: number) =>
  request.post(`/doctor/appointments/${appointmentId}/no-show`)

export const rescheduleAppointment = (appointmentId: number, newTime: string, reason: string) =>
  request.post(`/doctor/appointments/${appointmentId}/reschedule`, { newTime, reason })

export const cancelAppointment = (appointmentId: number, reason: string) =>
  request.post(`/doctor/appointments/${appointmentId}/cancel`, { reason })
