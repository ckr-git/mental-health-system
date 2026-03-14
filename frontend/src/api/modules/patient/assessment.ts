import request from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  ScaleListItem,
  ScaleDetailDTO,
  SessionResultDTO,
  AssessmentHistoryItem,
  AnswerItem
} from '@/types'

export const assessmentApi = {
  getScales: () =>
    request.get<any, ApiResponse<ScaleListItem[]>>('/patient/assessments/scales'),

  getScaleDetail: (scaleCode: string) =>
    request.get<any, ApiResponse<ScaleDetailDTO>>(`/patient/assessments/scales/${scaleCode}`),

  startSession: (scaleCode: string) =>
    request.post<any, ApiResponse<number>>('/patient/assessments/sessions', { scaleCode }),

  saveAnswers: (sessionId: number, answers: AnswerItem[]) =>
    request.put<any, ApiResponse<void>>(`/patient/assessments/sessions/${sessionId}/answers`, { answers }),

  submitSession: (sessionId: number) =>
    request.post<any, ApiResponse<SessionResultDTO>>(`/patient/assessments/sessions/${sessionId}/submit`),

  getHistory: (pageNum = 1, pageSize = 10) =>
    request.get<any, ApiResponse<PageResult<AssessmentHistoryItem>>>('/patient/assessments/history', {
      params: { pageNum, pageSize }
    }),

  getPatientAssessments: (patientId: number, pageNum = 1, pageSize = 10) =>
    request.get<any, ApiResponse<PageResult<AssessmentHistoryItem>>>(`/doctor/patients/${patientId}/assessments`, {
      params: { pageNum, pageSize }
    }),
}
