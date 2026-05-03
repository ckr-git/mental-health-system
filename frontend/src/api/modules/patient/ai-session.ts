import request from '@/utils/request'

// ===== 患者端AI会话 =====
export const openAiSession = (sessionType?: string) =>
  request.post('/patient/ai/sessions', { sessionType })

export const getAiSessions = (status?: string) =>
  request.get('/patient/ai/sessions', { params: { status } })

export const sendAiMessage = (sessionId: number, content: string) =>
  request.post(`/patient/ai/sessions/${sessionId}/messages`, { content })

export const getAiMessages = (sessionId: number, pageNum = 1, pageSize = 50) =>
  request.get(`/patient/ai/sessions/${sessionId}/messages`, { params: { pageNum, pageSize } })

export const closeAiSession = (sessionId: number, reason?: string) =>
  request.post(`/patient/ai/sessions/${sessionId}/close`, { reason })

// ===== 医生端AI转人工 =====
export const getHandoffTasks = (status?: string, pageNum = 1, pageSize = 20) =>
  request.get('/doctor/ai-handoff-tasks', { params: { status, pageNum, pageSize } })

export const acknowledgeHandoff = (taskId: number) =>
  request.post(`/doctor/ai-handoff-tasks/${taskId}/acknowledge`)

export const completeHandoff = (taskId: number, data: { note: string; followUpAction?: string }) =>
  request.post(`/doctor/ai-handoff-tasks/${taskId}/complete`, data)
