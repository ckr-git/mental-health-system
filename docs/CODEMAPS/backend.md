<!-- Generated: 2026-03-14 | Files scanned: 204 | Token estimate: ~900 -->

# Backend Codemap

## API Route Map (44 controllers, 100+ endpoints)

### Auth (/api/auth) — AuthController
POST /register → AuthService.register
POST /login    → AuthService.login → JWT token
POST /logout   → AuthService.logout
POST /send-code → hardcoded "123456"
POST /reset-password → AuthService.resetPassword

### Patient APIs
```
/api/patient/mood-diary   GET|POST|PUT|DEL  → MoodDiaryService → MoodDiaryMapper
/api/patient/ai           POST /ask, GET /conversations → AIService
/api/patient/assessments  GET scales, POST sessions, PUT answers, POST submit → AssessmentService+ScoringEngine
/api/patient/meditation   GET exercises, POST start/complete, GET history/stats → MeditationService
/api/patient/safety       GET /resources → static hotline list
/api/patient/appointments GET|POST book|cancel, POST waitlist → SchedulingService
/api/patient/analytics    GET mood-heatmap|correlations|summary → DashboardController
/api/patient/outcomes     GET summary|history → AnalyticsService
/api/patient/treatment-plans GET list|detail (read-only) → TreatmentPlanService
/api/patient/profile      GET|PUT aggregate → PatientProfileService
```

### Doctor APIs
```
/api/doctor/dashboard     GET /statistics → DoctorService
/api/doctor/patients      GET list|detail, POST claim|release → DoctorService
/api/doctor/reports       GET|POST|PUT|DEL → AssessmentReportService
/api/doctor/crisis-alerts GET list|detail, POST acknowledge|resolve → RiskAlertService
/api/doctor/schedule      GET|POST|DEL, POST override, GET slots|appointments → SchedulingService
/api/doctor/treatment-plans GET|POST|PUT, goals/interventions/session-notes → TreatmentPlanService
/api/doctor/performance   GET → AnalyticsService
/api/doctor/patients/{id}/assessments GET → AssessmentService
/api/doctor/patients/{id}/outcomes GET|POST snapshot → AnalyticsService
/api/doctor/chat          GET list|messages, POST send|read → ChatMessageService
/api/doctor/consultations GET list|detail, POST close → ConsultationService
```

### Admin APIs
```
/api/admin/appointments   GET|POST|PUT|DEL, statistics, trend → AppointmentService
/api/admin/messages       GET|POST, statistics → MessageService
/api/admin/resources      GET|POST|PUT|DEL → MentalResourceService
/api/admin/analytics      GET → AnalyticsService
/api/admin/*              users CRUD → UserController (via /api/admin/users)
```

## Key Service Layer

| Service | Responsibility | Key Methods |
|---------|---------------|-------------|
| ScoringEngine | Deterministic scale scoring (PHQ-9/GAD-7) | evaluate(), interpret() |
| RiskScoringService | Keyword+assessment+trend risk scoring | analyzeContent() |
| RiskAlertService | Alert creation with 4h debounce + SLA | processRiskEvent(), acknowledge() |
| SchedulingService | Slot generation, conflict detection, waitlist | generateSlots(), bookAppointment() |
| TreatmentPlanService | Full lifecycle: DRAFT→ACTIVE→COMPLETED | create/activate/complete, goals/interventions/notes |
| OutboxService | Async event publish + exponential retry | append(), dispatchBatch() |
| PatientProfileService | Aggregate user+patient_profile + Redis cache | getAggregate(), updateAggregate() |

## File Counts
- Controllers: 30 files
- Services: 35 files
- Entities: 40 files
- Mappers: 40 files (MyBatis-Plus)
- DTOs: 15 files
- Config: 7 files
- Security: 3 files
- Migrations: 8 SQL files (Flyway)
