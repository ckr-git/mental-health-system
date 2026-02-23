import request from '@/utils/request'
import type { AIConversation, ApiResponse } from '@/types'

export const aiApi = {
  ask: (question: string) =>
    request.post<any, ApiResponse<string>>('/patient/ai/ask', { question }),

  getConversations: (limit: number = 20) =>
    request.get<any, ApiResponse<AIConversation[]>>('/patient/ai/conversations', { params: { limit } }),

  submitFeedback: (conversationId: number, feedback: number) =>
    request.post<any, ApiResponse<string>>('/patient/ai/feedback', { conversationId, feedback }),

  generateReport: () =>
    request.post<any, ApiResponse<string>>('/patient/ai/generate-report')
}
