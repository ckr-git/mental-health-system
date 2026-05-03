import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface TreatmentPlanItem {
  id: number
  patientId: number
  doctorId: number
  title: string
  diagnosis?: string
  planStatus: string
  startDate?: string
  targetEndDate?: string
  actualEndDate?: string
  summary?: string
  notes?: string
  createTime: string
}

export interface TreatmentGoalItem {
  id: number
  planId: number
  goalType: string
  title: string
  description?: string
  measurableTarget?: string
  targetDate?: string
  priority: number
  status: string
  progressPct: number
  sortOrder: number
}

export interface TreatmentInterventionItem {
  id: number
  planId: number
  goalId?: number
  interventionType: string
  title: string
  description?: string
  frequency?: string
  status: string
}

export interface SessionNoteItem {
  id: number
  planId?: number
  appointmentId?: number
  patientId: number
  doctorId: number
  sessionDate: string
  sessionType: string
  subjective?: string
  objective?: string
  assessment?: string
  planNotes?: string
  riskLevel?: string
  homework?: string
  nextSessionPlan?: string
  createTime: string
}

export interface PlanDetailDTO {
  plan: TreatmentPlanItem
  goals: TreatmentGoalItem[]
  interventions: TreatmentInterventionItem[]
  sessionNotes: SessionNoteItem[]
  totalGoals: number
  achievedGoals: number
  avgProgress: number
  sessionCount: number
}

export const treatmentApi = {
  // Doctor: list plans
  getDoctorPlans: (status?: string) =>
    request.get<any, ApiResponse<TreatmentPlanItem[]>>('/doctor/treatment-plans', { params: { status } }),

  // Doctor: plan detail
  getPlanDetail: (id: number) =>
    request.get<any, ApiResponse<PlanDetailDTO>>(`/doctor/treatment-plans/${id}`),

  // Doctor: create plan
  createPlan: (data: Partial<TreatmentPlanItem>) =>
    request.post<any, ApiResponse<TreatmentPlanItem>>('/doctor/treatment-plans', data),

  // Doctor: update plan
  updatePlan: (id: number, data: Partial<TreatmentPlanItem>) =>
    request.put<any, ApiResponse<TreatmentPlanItem>>(`/doctor/treatment-plans/${id}`, data),

  // Doctor: activate plan
  activatePlan: (id: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/treatment-plans/${id}/activate`),

  // Doctor: complete plan
  completePlan: (id: number) =>
    request.post<any, ApiResponse<string>>(`/doctor/treatment-plans/${id}/complete`),

  // Goals
  addGoal: (planId: number, data: Partial<TreatmentGoalItem>) =>
    request.post<any, ApiResponse<TreatmentGoalItem>>(`/doctor/treatment-plans/${planId}/goals`, data),

  updateGoal: (goalId: number, data: Partial<TreatmentGoalItem>) =>
    request.put<any, ApiResponse<TreatmentGoalItem>>(`/doctor/treatment-plans/goals/${goalId}`, data),

  updateGoalProgress: (goalId: number, progressPct: number, note?: string) =>
    request.post<any, ApiResponse<string>>(`/doctor/treatment-plans/goals/${goalId}/progress`, { progressPct, note }),

  getGoalProgressHistory: (goalId: number) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/treatment-plans/goals/${goalId}/progress-history`),

  // Interventions
  addIntervention: (planId: number, data: Partial<TreatmentInterventionItem>) =>
    request.post<any, ApiResponse<TreatmentInterventionItem>>(`/doctor/treatment-plans/${planId}/interventions`, data),

  updateIntervention: (interventionId: number, data: Partial<TreatmentInterventionItem>) =>
    request.put<any, ApiResponse<TreatmentInterventionItem>>(`/doctor/treatment-plans/interventions/${interventionId}`, data),

  // Session notes
  createSessionNote: (data: Partial<SessionNoteItem>) =>
    request.post<any, ApiResponse<SessionNoteItem>>('/doctor/treatment-plans/session-notes', data),

  updateSessionNote: (noteId: number, data: Partial<SessionNoteItem>) =>
    request.put<any, ApiResponse<SessionNoteItem>>(`/doctor/treatment-plans/session-notes/${noteId}`, data),

  getPatientSessionNotes: (patientId: number) =>
    request.get<any, ApiResponse<SessionNoteItem[]>>(`/doctor/treatment-plans/patients/${patientId}/session-notes`),

  // Patient: my plans
  getMyPlans: () =>
    request.get<any, ApiResponse<TreatmentPlanItem[]>>('/patient/treatment-plans'),

  getMyPlanDetail: (id: number) =>
    request.get<any, ApiResponse<PlanDetailDTO>>(`/patient/treatment-plans/${id}`),

  // ===== Phase 2: 阶段化 =====
  getPlanPhases: (planId: number) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/treatment-plans/${planId}/phases`),

  initPlanPhases: (planId: number) =>
    request.post<any, ApiResponse<void>>(`/doctor/treatment-plans/${planId}/phases/init`),

  activatePhase: (planId: number, phaseId: number) =>
    request.post<any, ApiResponse<void>>(`/doctor/treatment-plans/${planId}/phases/${phaseId}/activate`),

  completePhase: (planId: number, phaseId: number, note: string) =>
    request.post<any, ApiResponse<void>>(`/doctor/treatment-plans/${planId}/phases/${phaseId}/complete`, { note }),

  // 评审
  getPlanReviews: (planId: number, status?: string) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/treatment-plans/${planId}/reviews`, { params: { status } }),

  createReview: (planId: number, data: { phaseId?: number; dueAt?: string }) =>
    request.post<any, ApiResponse<number>>(`/doctor/treatment-plans/${planId}/reviews`, data),

  completeReview: (reviewId: number, data: { conclusionCode: string; summaryText: string }) =>
    request.post<any, ApiResponse<void>>(`/doctor/treatment-plan-reviews/${reviewId}/complete`, data),

  // 修订
  proposeRevision: (planId: number, data: { changeSummary: string; reason: string }) =>
    request.post<any, ApiResponse<number>>(`/doctor/treatment-plans/${planId}/revisions`, data),

  approveRevision: (revisionId: number) =>
    request.post<any, ApiResponse<void>>(`/doctor/treatment-plan-revisions/${revisionId}/approve`),

  // 干预任务
  assignTask: (patientId: number, data: any) =>
    request.post<any, ApiResponse<number>>(`/doctor/patients/${patientId}/intervention-tasks`, data),

  reviewTask: (taskId: number, data: { resultCode: string; note: string }) =>
    request.post<any, ApiResponse<void>>(`/doctor/intervention-tasks/${taskId}/review`, data),

  getPlanTasks: (planId: number) =>
    request.get<any, ApiResponse<any[]>>(`/doctor/treatment-plans/${planId}/tasks`),

  getMyTasks: (status?: string) =>
    request.get<any, ApiResponse<any[]>>('/patient/intervention-tasks', { params: { status } }),

  submitTask: (taskId: number, data: { note: string; value?: string }) =>
    request.post<any, ApiResponse<void>>(`/patient/intervention-tasks/${taskId}/submit`, data),
}
