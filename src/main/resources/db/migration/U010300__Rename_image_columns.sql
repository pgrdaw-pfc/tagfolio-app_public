-- Flyway Undo Script for V010300
--
-- This script reverts the column name changes from the V010300 migration.
-- 1. IMPORTED_AT is renamed back to FILE_MODIFIED_AT.
-- 2. MODIFIED_AT is renamed back to UPDATED_AT.

ALTER TABLE IMAGES RENAME COLUMN IMPORTED_AT TO FILE_MODIFIED_AT;
/

ALTER TABLE IMAGES RENAME COLUMN MODIFIED_AT TO UPDATED_AT;
/