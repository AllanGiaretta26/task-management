-- Migration V3: Cleanup legacy schema
-- Drops the legacy `tasks` table (first-version entity, no longer used) and
-- renames the `columns` table (a SQL reserved word) to `board_columns`.
-- MySQL `RENAME TABLE` preserves the existing foreign key constraints
-- (fk_column_board, fk_card_column, fk_history_column) automatically.

DROP TABLE IF EXISTS tasks;

RENAME TABLE `columns` TO board_columns;
