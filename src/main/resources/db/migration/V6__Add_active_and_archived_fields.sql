-- V6__Add_active_and_archived_fields.sql
ALTER TABLE timetables ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE timetables ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_timetables_is_active ON timetables (is_active);
CREATE INDEX idx_timetables_is_archived ON timetables (is_archived); 