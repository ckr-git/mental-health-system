import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface MeditationExercise {
  id: number
  exerciseCode: string
  title: string
  category: string
  description: string
  durationSeconds: number
  inhaleSeconds?: number
  holdSeconds?: number
  exhaleSeconds?: number
  restSeconds?: number
  instructionSteps: string
  animationPreset: string
}

export interface MeditationSessionDTO {
  id: number
  exerciseId: number
  sessionStatus: string
  startedAt: string
  completedAt?: string
  plannedSeconds: number
  actualSeconds: number
}

export const meditationApi = {
  getExercises: () =>
    request.get<any, ApiResponse<MeditationExercise[]>>('/patient/meditation/exercises'),

  startSession: (exerciseId: number) =>
    request.post<any, ApiResponse<number>>('/patient/meditation/sessions/start', { exerciseId }),

  completeSession: (sessionId: number, actualSeconds: number) =>
    request.post<any, ApiResponse<void>>(`/patient/meditation/sessions/${sessionId}/complete`, { actualSeconds }),

  getHistory: (pageNum = 1, pageSize = 10) =>
    request.get<any, ApiResponse<PageResult<MeditationSessionDTO>>>('/patient/meditation/history', {
      params: { pageNum, pageSize }
    }),

  getStats: () =>
    request.get<any, ApiResponse<{ totalMinutes: number; completedSessions: number }>>('/patient/meditation/stats'),
}
