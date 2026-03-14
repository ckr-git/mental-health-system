import request from '@/utils/request'
import type { ProfileAggregate, UpdateProfileCommand, ApiResponse } from '@/types'

export const profileApi = {
  getAggregateProfile: () =>
    request.get<any, ApiResponse<ProfileAggregate>>('/patient/profile'),

  updateAggregateProfile: (data: UpdateProfileCommand) =>
    request.put<any, ApiResponse<ProfileAggregate>>('/patient/profile', data),

  getPatientProfile: (patientId: number) =>
    request.get<any, ApiResponse<ProfileAggregate>>(`/patient/profile/doctor/${patientId}`)
}
