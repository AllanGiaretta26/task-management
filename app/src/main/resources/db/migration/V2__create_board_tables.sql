-- Migration V2: Create boards, columns, cards, blockades and column_history tables
-- This migration creates the complete board structure for task management.

-- Create boards table
CREATE TABLE IF NOT EXISTS boards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_board_name (name),
    INDEX idx_board_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create columns table
CREATE TABLE IF NOT EXISTS `columns` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    position INT NOT NULL,
    board_id BIGINT NOT NULL,
    INDEX idx_column_board_position (board_id, position),
    CONSTRAINT fk_column_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT chk_column_type CHECK (type IN ('INITIAL', 'PENDING', 'FINAL', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create cards table
CREATE TABLE IF NOT EXISTS cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    column_id BIGINT NOT NULL,
    INDEX idx_card_column (column_id),
    INDEX idx_card_status (status),
    CONSTRAINT fk_card_column FOREIGN KEY (column_id) REFERENCES `columns`(id) ON DELETE CASCADE,
    CONSTRAINT chk_card_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create blockades table
CREATE TABLE IF NOT EXISTS blockades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_blocking BOOLEAN NOT NULL,
    INDEX idx_blockade_card (card_id),
    INDEX idx_blockade_date (blocked_at),
    CONSTRAINT fk_blockade_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create column_history table
CREATE TABLE IF NOT EXISTS column_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    column_id BIGINT NOT NULL,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_card (card_id),
    INDEX idx_history_column (column_id),
    INDEX idx_history_entered_at (entered_at),
    CONSTRAINT fk_history_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_column FOREIGN KEY (column_id) REFERENCES `columns`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
