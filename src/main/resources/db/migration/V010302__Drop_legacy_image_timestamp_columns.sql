-- V010302: Drop the legacy, now-redundant timestamp columns from the IMAGES table.
-- This is the second step in the cleanup process and should only be run after
-- verifying that the data copy in V010301 was successful.

ALTER TABLE IMAGES DROP COLUMN FILE_MODIFIED_AT;
/
ALTER TABLE IMAGES DROP COLUMN UPDATED_AT;
/