-- Migration V1: Create tasks table
-- This migration creates the initial tasks table structure for the task management system.

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_status (completed),
    INDEX idx_task_priority (priority),
    INDEX idx_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add a check constraint to ensure priority is valid
ALTER TABLE tasks
    ADD CONSTRAINT chk_task_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'));
