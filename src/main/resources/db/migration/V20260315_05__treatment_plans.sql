-- Phase 5: Treatment Plan Management
-- Treatment plans, goals, interventions, session notes, progress tracking

-- 1) Treatment plan
CREATE TABLE treatment_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  diagnosis VARCHAR(500) NULL COMMENT 'Primary diagnosis / presenting problem',
  plan_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,ACTIVE,PAUSED,COMPLETED,ARCHIVED',
  start_date DATE NULL,
  target_end_date DATE NULL,
  actual_end_date DATE NULL,
  summary TEXT NULL COMMENT 'Overall treatment objectives',
  notes TEXT NULL,
  plan_version INT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plan_patient (patient_id, plan_status),
  INDEX idx_plan_doctor (doctor_id, plan_status),
  CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES user(id),
  CONSTRAINT fk_plan_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Treatment goals (SMART goals within a plan)
CREATE TABLE treatment_goal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  goal_type VARCHAR(30) NOT NULL DEFAULT 'SHORT_TERM' COMMENT 'SHORT_TERM,LONG_TERM',
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  measurable_target VARCHAR(300) NULL COMMENT 'Specific measurable criteria',
  target_date DATE NULL,
  priority INT NOT NULL DEFAULT 1 COMMENT '1=highest',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,IN_PROGRESS,ACHIEVED,DEFERRED,DROPPED',
  progress_pct INT NOT NULL DEFAULT 0 COMMENT '0-100',
  achieved_at DATETIME NULL,
  sort_order INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_goal_plan (plan_id, status),
  CONSTRAINT fk_goal_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Treatment interventions (specific actions/exercises/techniques)
CREATE TABLE treatment_intervention (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  goal_id BIGINT NULL COMMENT 'linked to a specific goal, or null for general',
  intervention_type VARCHAR(30) NOT NULL COMMENT 'CBT,MEDICATION,EXERCISE,HOMEWORK,MEDITATION,JOURNALING,OTHER',
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  frequency VARCHAR(50) NULL COMMENT 'e.g. daily, 3x/week, each session',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE,COMPLETED,DROPPED',
  sort_order INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_intervention_plan (plan_id),
  CONSTRAINT fk_intervention_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id),
  CONSTRAINT fk_intervention_goal FOREIGN KEY (goal_id) REFERENCES treatment_goal(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Session notes (per-appointment clinical notes)
CREATE TABLE session_note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NULL COMMENT 'linked treatment plan',
  appointment_id BIGINT NULL COMMENT 'linked appointment',
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  session_date DATE NOT NULL,
  session_type VARCHAR(30) NOT NULL DEFAULT 'INDIVIDUAL' COMMENT 'INDIVIDUAL,GROUP,CRISIS,PHONE,ONLINE',
  subjective TEXT NULL COMMENT 'Patient-reported info (SOAP S)',
  objective TEXT NULL COMMENT 'Clinician observations (SOAP O)',
  assessment TEXT NULL COMMENT 'Clinical assessment (SOAP A)',
  plan_notes TEXT NULL COMMENT 'Plan/next steps (SOAP P)',
  interventions_used JSON NULL COMMENT 'interventionIds used this session',
  risk_level VARCHAR(20) NULL COMMENT 'current risk assessment',
  homework TEXT NULL COMMENT 'assigned to patient',
  next_session_plan TEXT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_note_plan (plan_id, session_date),
  INDEX idx_note_patient (patient_id, session_date),
  CONSTRAINT fk_note_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id),
  CONSTRAINT fk_note_patient FOREIGN KEY (patient_id) REFERENCES user(id),
  CONSTRAINT fk_note_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) Goal progress log (track progress over time)
CREATE TABLE goal_progress_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  goal_id BIGINT NOT NULL,
  logged_by BIGINT NOT NULL,
  progress_pct INT NOT NULL,
  note VARCHAR(500) NULL,
  log_date DATE NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_progress_goal FOREIGN KEY (goal_id) REFERENCES treatment_goal(id),
  CONSTRAINT fk_progress_logger FOREIGN KEY (logged_by) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
