-- Flyway Undo Script for V010302
-- This script reverts the dropping of the legacy IMAGES timestamp columns.

-- Step 1: Re-add the old columns.
ALTER TABLE IMAGES ADD (FILE_MODIFIED_AT TIMESTAMP(6));
/
ALTER TABLE IMAGES ADD (UPDATED_AT TIMESTAMP(6));
/

-- Step 2: Copy the data back from the new columns to the old columns to restore state.
UPDATE IMAGES SET FILE_MODIFIED_AT = IMPORTED_AT;
/
UPDATE IMAGES SET UPDATED_AT = MODIFIED_AT;
/