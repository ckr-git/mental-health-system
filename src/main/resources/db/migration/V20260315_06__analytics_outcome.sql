-- Phase 6: Data Analytics & Outcome Tracking
-- Aggregated metrics, treatment effectiveness, outcome snapshots

-- 1) Outcome snapshot (periodic summary for a patient)
CREATE TABLE outcome_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NULL,
  snapshot_date DATE NOT NULL,
  snapshot_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' COMMENT 'WEEKLY,MONTHLY,QUARTERLY,DISCHARGE',
  -- Assessment scores at this point
  phq9_score INT NULL,
  gad7_score INT NULL,
  phq9_severity VARCHAR(30) NULL,
  gad7_severity VARCHAR(30) NULL,
  -- Mood diary metrics
  avg_mood_score DECIMAL(5,2) NULL,
  avg_sleep_quality DECIMAL(5,2) NULL,
  avg_stress_level DECIMAL(5,2) NULL,
  avg_energy_level DECIMAL(5,2) NULL,
  diary_count INT NOT NULL DEFAULT 0,
  -- Engagement metrics
  appointment_count INT NOT NULL DEFAULT 0,
  appointment_attended INT NOT NULL DEFAULT 0,
  meditation_minutes INT NOT NULL DEFAULT 0,
  assessment_count INT NOT NULL DEFAULT 0,
  -- Treatment plan metrics
  active_plan_id BIGINT NULL,
  goals_total INT NOT NULL DEFAULT 0,
  goals_achieved INT NOT NULL DEFAULT 0,
  avg_goal_progress INT NOT NULL DEFAULT 0,
  -- Risk
  risk_events_count INT NOT NULL DEFAULT 0,
  crisis_alerts_count INT NOT NULL DEFAULT 0,
  -- Clinician summary
  clinician_notes TEXT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_outcome_patient_date (patient_id, snapshot_date),
  CONSTRAINT fk_outcome_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Doctor performance metrics (aggregated)
CREATE TABLE doctor_performance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doctor_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  total_patients INT NOT NULL DEFAULT 0,
  active_plans INT NOT NULL DEFAULT 0,
  completed_plans INT NOT NULL DEFAULT 0,
  appointments_scheduled INT NOT NULL DEFAULT 0,
  appointments_completed INT NOT NULL DEFAULT 0,
  avg_patient_improvement DECIMAL(5,2) NULL COMMENT 'avg PHQ9/GAD7 score reduction %',
  crisis_alerts_handled INT NOT NULL DEFAULT 0,
  session_notes_count INT NOT NULL DEFAULT 0,
  avg_response_time_hours DECIMAL(8,2) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doctor_performance_period (doctor_id, period_start, period_end),
  CONSTRAINT fk_perf_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) System-wide analytics cache (for admin dashboard)
CREATE TABLE analytics_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_key VARCHAR(100) NOT NULL,
  metric_value JSON NOT NULL,
  period_type VARCHAR(20) NOT NULL COMMENT 'DAILY,WEEKLY,MONTHLY',
  period_date DATE NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_key_period (metric_key, period_type, period_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
