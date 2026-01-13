-- V010301: Consolidate and clean up redundant timestamp columns in the IMAGES table.
-- V010301: Copy data from legacy timestamp columns to the new columns.
-- This is the first step in a two-part migration to clean up the IMAGES table.
-- This script is non-destructive; it only copies data.

-- We only update rows where the new column is NULL to avoid overwriting fresh data.

UPDATE IMAGES
SET IMPORTED_AT = FILE_MODIFIED_AT
WHERE IMPORTED_AT IS NULL AND FILE_MODIFIED_AT IS NOT NULL;
/

UPDATE IMAGES
SET MODIFIED_AT = UPDATED_AT
WHERE MODIFIED_AT IS NULL AND UPDATED_AT IS NOT NULL;
/