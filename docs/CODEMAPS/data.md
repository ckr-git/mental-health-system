<!-- Generated: 2026-03-14 | Files scanned: 8 migrations | Token estimate: ~600 -->

# Data Codemap

## Database: MySQL (mental_health)
## Migration: Flyway (src/main/resources/db/migration/)

## Tables by Domain

### Core (pre-existing)
```
user                    → id, username, password(BCrypt), nickname, role(PATIENT/DOCTOR/ADMIN), status, phone, email
appointment             → id, patient_id, doctor_id, type, time, duration, status(0-3), symptoms, reminder_sent
mood_diary              → id, user_id, mood(1-10), energy, sleep, stress, title, content, tags
tree_hole               → id, user_id, content, anonymous, likes, comments
time_capsule            → id, user_id, content, open_date, is_opened
chat_message            → id, session_id, sender_id, receiver_id, content, type
assessment_report       → id, user_id, report_type, content, score, + assessment_session_id, scale_code, severity_level
```

### Phase 1 — Platform Infrastructure
```
patient_profile         → user_id(UK), real_name, birth_date, marital_status, occupation,
                          emergency_contact_*, medical_history, allergy_history, family_history, consent_flags(JSON)
outbox_event            → id, aggregate_type, aggregate_id, event_type, event_key(UK), payload(JSON),
                          status(PENDING/PROCESSING/SENT/FAILED/DEAD), retry_count, next_retry_time
user_presence           → user_id(PK), websocket_session_id, online_status, last_seen_at
```

### Phase 2 — Assessment Engine
```
assessment_scale        → id, scale_code(UK+ver), scale_name, scoring_dsl, interpretation_dsl, active
assessment_scale_item   → id, scale_id(FK), item_no, question_text, answer_options(JSON), reverse_scored
assessment_session      → id, user_id, scale_id(FK), status(IN_PROGRESS/SUBMITTED/SCORED), total_score, severity_level
assessment_response     → id, session_id(FK), item_id(FK), answer_value, answer_label
```

### Phase 3 — Crisis Intervention
```
risk_rule               → id, rule_code(UK), source_type, trigger_pattern, weight, action_level
risk_event              → id, patient_id, source_type, rule_hits(JSON), final_score, final_level, confidence
crisis_alert            → id, patient_id, doctor_id, risk_event_id(FK), alert_level, alert_status(OPEN→RESOLVED),
                          sla_deadline, acknowledged_by, resolution_note
crisis_alert_action     → id, alert_id(FK), action_type, operator_id, note
```

### Phase 3 Modules
```
meditation_exercise     → id, name, type, description, duration_seconds, pattern(JSON), instructions(JSON)
meditation_session      → id, user_id, exercise_id, started_at, completed_at, duration
user_notification       → id, user_id, title, content, type, read_flag, link
```

### Phase 4 — Smart Scheduling
```
doctor_schedule         → id, doctor_id, day_of_week(1-7), start_time, end_time, slot_duration, max_patients
doctor_schedule_override → id, doctor_id, override_date(UK+doc), override_type(VACATION/EXTRA), start/end_time
appointment_waitlist    → id, patient_id, doctor_id, preferred_date, status(WAITING→BOOKED/EXPIRED)
```

### Phase 5 — Treatment Plans
```
treatment_plan          → id, patient_id, doctor_id, title, diagnosis, plan_status(DRAFT→ACTIVE→COMPLETED), summary
treatment_goal          → id, plan_id(FK), goal_type(SHORT/LONG_TERM), title, description, measurable_criteria, progress_pct
treatment_intervention  → id, plan_id(FK), intervention_type(CBT/MEDICATION/...), name, description, frequency
session_note            → id, plan_id(FK), patient_id, subjective, objective, assessment, plan_text, homework, risk_level
goal_progress_log       → id, goal_id(FK), logged_by, progress_pct, note, log_date
```

### Phase 6 — Analytics
```
outcome_snapshot        → id, patient_id, doctor_id, period_type(WEEKLY/MONTHLY/...), snapshot_data(JSON)
doctor_performance      → id, doctor_id, period_start/end, metrics(JSON)
analytics_cache         → id, metric_key, metric_value(JSON), period_start/end, cached_at
```

## Key Indexes
- outbox_event: idx_outbox_dispatch (status, next_retry_time)
- risk_event: idx_risk_event_patient_time, idx_risk_event_level
- crisis_alert: idx_crisis_alert_status_level
- assessment_session: idx_session_user_status
- appointment: idx_doctor_status_time, idx_patient_status_time
