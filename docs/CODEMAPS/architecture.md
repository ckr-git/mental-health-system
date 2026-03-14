<!-- Generated: 2026-03-14 | Files scanned: 314 | Token estimate: ~950 -->

# Architecture Overview

## System Type
Fullstack monorepo — Spring Boot 3.2 backend + Vue 3 SPA frontend

## Service Topology
```
Browser ──► Vite Dev :5173 ──proxy──► Spring Boot :8080 ──► MySQL :3306
                                          │                    │
                                          ├──► Redis :6379     │
                                          ├──► WebSocket/STOMP │
                                          └──► Flyway migrations
```

## Three-Role Architecture
```
PATIENT (/patient/*) ── 消费级互动功能 + 临床自助工具
DOCTOR  (/doctor/*)  ── 临床工作台 + 患者管理 + 治疗计划
ADMIN   (/admin/*)   ── 用户/资源/预约管理 + 系统分析
```

## Core Domain Modules (Phase 1-6)

| Module | Tables | Backend Services | Frontend Pages |
|--------|--------|-----------------|----------------|
| Auth & Profile | user, patient_profile | AuthService, PatientProfileService | Login, Register, Profile |
| Mood Diary | mood_diary, mood_comment | MoodDiaryService, MoodCommentService | MoodDiary, TreeHole, TimeCapsule |
| Assessment Engine | assessment_scale/item/session/response | AssessmentService, ScoringEngine | Assessments |
| Crisis Intervention | risk_rule, risk_event, crisis_alert/action | RiskScoringService, RiskAlertService, CrisisEscalationScheduler | CrisisAlerts |
| Scheduling | doctor_schedule/override, appointment, waitlist | SchedulingService, AppointmentReminderScheduler | Scheduling, Appointments |
| Treatment Plans | treatment_plan/goal/intervention, session_note, goal_progress_log | TreatmentPlanService | TreatmentPlans |
| Analytics | outcome_snapshot, doctor_performance, analytics_cache | AnalyticsService | (API-only, no dedicated page) |
| Infrastructure | outbox_event, user_presence | OutboxService, UserPresenceService | — |

## Async Event Pipeline (Outbox Pattern)
```
Service → OutboxService.append() → outbox_event table
  ↓ (5s poll)
OutboxService.dispatchBatch() → OutboxRoutingRegistry.route()
  ├──► RiskAnalysisEventHandler (DIARY_CREATED → risk scoring)
  ├──► AppointmentEventHandler (APPOINTMENT_* → notifications)
  └──► NotificationPushService (WebSocket push)
```

## Scheduled Tasks
- OutboxService: 5s poll for pending events
- CrisisEscalationScheduler: 5min SLA check + auto-escalation
- AppointmentReminderScheduler: 5min reminder (1h before) + 10min expired cleanup
- TreeHoleCleanupTask: periodic anonymous post cleanup

## Security
- JWT token (24h TTL) via JwtAuthenticationFilter
- Role-based: ADMIN > DOCTOR > PATIENT (Spring Security requestMatchers)
- WebSocket: JwtStompChannelInterceptor validates CONNECT frames
- CSRF disabled (stateless JWT)
