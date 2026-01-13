-- V010301: Consolidate and clean up redundant timestamp columns in the IMAGES table.
-- This script addresses the mixed schema state caused by using ddl-auto=update.

-- Step 1: Consolidate data from old columns into new columns.
-- We only update rows where the new column is NULL to avoid overwriting fresh data.

UPDATE IMAGES
SET IMPORTED_AT = FILE_MODIFIED_AT
WHERE IMPORTED_AT IS NULL AND FILE_MODIFIED_AT IS NOT NULL;
/

UPDATE IMAGES
SET MODIFIED_AT = UPDATED_AT
WHERE MODIFIED_AT IS NULL AND UPDATED_AT IS NOT NULL;
/

-- Step 2: Drop the old, now-redundant columns.
ALTER TABLE IMAGES DROP COLUMN FILE_MODIFIED_AT;
/
ALTER TABLE IMAGES DROP COLUMN UPDATED_AT;
/