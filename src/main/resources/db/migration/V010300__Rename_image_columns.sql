-- Flyway Migration Script to version 010300
--
-- This script renames two columns in the IMAGES table for better clarity:
-- 1. FILE_MODIFIED_AT is renamed to IMPORTED_AT to better reflect the date the image was imported into the system.
-- 2. UPDATED_AT is renamed to MODIFIED_AT to align with more common naming conventions for modification timestamps.

ALTER TABLE IMAGES RENAME COLUMN FILE_MODIFIED_AT TO IMPORTED_AT;
/

ALTER TABLE IMAGES RENAME COLUMN UPDATED_AT TO MODIFIED_AT;
/