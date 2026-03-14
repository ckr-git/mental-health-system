import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export const dashboardApi = {
  getMoodHeatmap: (year: number, month: number) =>
    request.get<any, ApiResponse<{ date: string; moodScore: number }[]>>('/patient/analytics/mood-heatmap', {
      params: { year, month }
    }),

  getCorrelations: (range = 30) =>
    request.get<any, ApiResponse<{
      scatterPoints: { mood: number; sleep: number; stress: number; energy: number; date: string }[]
      correlations: { moodSleep: number; moodStress: number; moodEnergy: number }
    }>>('/patient/analytics/correlations', { params: { range } }),

  getSummary: (range = 7) =>
    request.get<any, ApiResponse<{
      avgMood: number; avgSleep: number; avgStress: number
      meditationMinutes: number; meditationSessions: number; days: number
    }>>('/patient/analytics/summary', { params: { range } }),
}
