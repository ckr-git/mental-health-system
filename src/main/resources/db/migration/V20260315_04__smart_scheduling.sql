-- Phase 4: Smart Scheduling System
-- Doctor availability, patient self-service booking, conflict detection, reminders

-- 1) Doctor weekly schedule template (recurring availability)
CREATE TABLE doctor_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doctor_id BIGINT NOT NULL,
  day_of_week TINYINT NOT NULL COMMENT '1=Monday, 7=Sunday',
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  slot_duration INT NOT NULL DEFAULT 50 COMMENT 'minutes per slot',
  max_patients INT NOT NULL DEFAULT 1 COMMENT 'concurrent patients per slot',
  location VARCHAR(100) NULL COMMENT 'room/online',
  active TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_doctor_schedule_doctor (doctor_id, day_of_week),
  CONSTRAINT fk_doctor_schedule_user FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Doctor date-specific overrides (holidays, special hours)
CREATE TABLE doctor_schedule_override (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doctor_id BIGINT NOT NULL,
  override_date DATE NOT NULL,
  override_type VARCHAR(20) NOT NULL COMMENT 'AVAILABLE,UNAVAILABLE',
  start_time TIME NULL COMMENT 'null if UNAVAILABLE (whole day off)',
  end_time TIME NULL,
  slot_duration INT NULL,
  reason VARCHAR(200) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doctor_override_date (doctor_id, override_date),
  CONSTRAINT fk_doctor_override_user FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Waitlist for fully-booked slots
CREATE TABLE appointment_waitlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  preferred_date DATE NOT NULL,
  preferred_time_start TIME NULL,
  preferred_time_end TIME NULL,
  appointment_type VARCHAR(30) NOT NULL DEFAULT 'CONSULTATION',
  symptoms VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING,NOTIFIED,BOOKED,EXPIRED,CANCELLED',
  notified_at DATETIME NULL,
  expired_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_waitlist_doctor_date (doctor_id, preferred_date, status),
  INDEX idx_waitlist_patient (patient_id, status),
  CONSTRAINT fk_waitlist_patient FOREIGN KEY (patient_id) REFERENCES user(id),
  CONSTRAINT fk_waitlist_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Add source tracking to appointment
ALTER TABLE appointment
  ADD COLUMN source VARCHAR(20) DEFAULT 'ADMIN' COMMENT 'ADMIN,PATIENT,DOCTOR,WAITLIST',
  ADD COLUMN waitlist_id BIGINT NULL;

-- 5) Seed sample doctor schedules for existing doctors
INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, slot_duration, max_patients)
SELECT u.id, dw.day_of_week, '09:00:00', '12:00:00', 50, 1
FROM user u
CROSS JOIN (SELECT 1 AS day_of_week UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) dw
WHERE u.role = 'DOCTOR' AND u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM doctor_schedule ds WHERE ds.doctor_id = u.id AND ds.day_of_week = dw.day_of_week AND ds.deleted = 0);

INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, slot_duration, max_patients)
SELECT u.id, dw.day_of_week, '14:00:00', '17:00:00', 50, 1
FROM user u
CROSS JOIN (SELECT 1 AS day_of_week UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) dw
WHERE u.role = 'DOCTOR' AND u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM doctor_schedule ds WHERE ds.doctor_id = u.id AND ds.day_of_week = dw.day_of_week
                  AND ds.start_time = '14:00:00' AND ds.deleted = 0);
