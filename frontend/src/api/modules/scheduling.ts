import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface TimeSlot {
  date: string
  startTime: string
  endTime: string
  available: number
  total: number
  location: string | null
}

export interface DoctorScheduleItem {
  id: number
  doctorId: number
  dayOfWeek: number
  startTime: string
  endTime: string
  slotDuration: number
  maxPatients: number
  location: string | null
  active: number
}

export interface BookAppointmentRequest {
  doctorId: number
  appointmentTime: string
  appointmentType: string
  symptoms?: string
  notes?: string
}

export interface AppointmentItem {
  id: number
  patientId: number
  doctorId: number
  appointmentType: string
  appointmentTime: string
  duration: number
  status: number
  symptoms?: string
  notes?: string
  cancelReason?: string
  cancelTime?: string
  confirmTime?: string
  completeTime?: string
  reminderSent: number
  createTime: string
}

export interface WaitlistItem {
  id: number
  patientId: number
  doctorId: number
  preferredDate: string
  preferredTimeStart?: string
  preferredTimeEnd?: string
  appointmentType: string
  symptoms?: string
  status: string
  createTime: string
}

export const schedulingApi = {
  // Patient: get available slots
  getAvailableSlots: (doctorId: number, startDate: string, endDate: string) =>
    request.get<any, ApiResponse<TimeSlot[]>>('/patient/appointments/slots', {
      params: { doctorId, startDate, endDate }
    }),

  // Patient: book appointment
  bookAppointment: (data: BookAppointmentRequest) =>
    request.post<any, ApiResponse<number>>('/patient/appointments/book', data),

  // Patient: my appointments
  getMyAppointments: (status?: number) =>
    request.get<any, ApiResponse<AppointmentItem[]>>('/patient/appointments', {
      params: { status }
    }),

  // Patient: cancel appointment
  cancelAppointment: (id: number, reason?: string) =>
    request.post<any, ApiResponse<string>>(`/patient/appointments/${id}/cancel`, { reason }),

  // Patient: join waitlist
  joinWaitlist: (data: {
    doctorId: number
    preferredDate: string
    preferredTimeStart?: string
    preferredTimeEnd?: string
    appointmentType?: string
    symptoms?: string
  }) =>
    request.post<any, ApiResponse<number>>('/patient/appointments/waitlist', data),

  // Patient: my waitlist
  getMyWaitlist: () =>
    request.get<any, ApiResponse<WaitlistItem[]>>('/patient/appointments/waitlist'),

  // Patient: cancel waitlist
  cancelWaitlist: (id: number) =>
    request.post<any, ApiResponse<string>>(`/patient/appointments/waitlist/${id}/cancel`),

  // Doctor: get my schedule
  getDoctorSchedule: () =>
    request.get<any, ApiResponse<DoctorScheduleItem[]>>('/doctor/schedule'),

  // Doctor: save schedule slot
  saveScheduleSlot: (data: Partial<DoctorScheduleItem>) =>
    request.post<any, ApiResponse<DoctorScheduleItem>>('/doctor/schedule', data),

  // Doctor: delete schedule slot
  deleteScheduleSlot: (id: number) =>
    request.delete<any, ApiResponse<string>>(`/doctor/schedule/${id}`),

  // Doctor: save override
  saveOverride: (data: {
    overrideDate: string
    overrideType: string
    startTime?: string
    endTime?: string
    slotDuration?: number
    reason?: string
  }) =>
    request.post<any, ApiResponse<any>>('/doctor/schedule/override', data),

  // Doctor: get overrides
  getOverrides: (startDate: string, endDate: string) =>
    request.get<any, ApiResponse<any[]>>('/doctor/schedule/overrides', {
      params: { startDate, endDate }
    }),

  // Doctor: get my available slots
  getDoctorSlots: (startDate: string, endDate: string) =>
    request.get<any, ApiResponse<TimeSlot[]>>('/doctor/schedule/slots', {
      params: { startDate, endDate }
    }),

  // Doctor: get my appointments
  getDoctorAppointments: (status?: number, date?: string) =>
    request.get<any, ApiResponse<AppointmentItem[]>>('/doctor/schedule/appointments', {
      params: { status, date }
    }),

  // Doctor: confirm appointment
  confirmAppointment: (id: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/schedule/appointments/${id}/confirm`),

  // Doctor: complete appointment
  completeAppointment: (id: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/schedule/appointments/${id}/complete`),

  // Doctor: cancel appointment
  doctorCancelAppointment: (id: number, reason?: string) =>
    request.post<any, ApiResponse<string>>(`/doctor/schedule/appointments/${id}/cancel`, { reason }),
}
