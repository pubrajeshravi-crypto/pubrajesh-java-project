-- =============================================================
-- Student Management System — Schema
-- Run once against your MySQL 8 instance.
-- =============================================================

CREATE DATABASE IF NOT EXISTS sms_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sms_db;

DROP TABLE IF EXISTS students;

CREATE TABLE students (
  student_id  VARCHAR(20)   NOT NULL,
  name        VARCHAR(100)  NOT NULL,
  branch      VARCHAR(60)   NOT NULL,
  year        TINYINT       NOT NULL,
  phone       VARCHAR(15)   NOT NULL,
  marks       DECIMAL(5,2)  NOT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_student  PRIMARY KEY (student_id),
  CONSTRAINT uq_phone    UNIQUE      (phone),
  CONSTRAINT chk_year    CHECK (year  BETWEEN 1 AND 4),
  CONSTRAINT chk_marks   CHECK (marks BETWEEN 0.00 AND 100.00)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Indexes for common search columns
CREATE INDEX idx_students_name   ON students (name);
CREATE INDEX idx_students_branch ON students (branch);
