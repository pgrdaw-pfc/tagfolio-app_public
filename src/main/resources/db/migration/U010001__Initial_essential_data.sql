-- Flyway Undo Script for V010001
--
-- This script removes all data seeded by the initial data migration.

DELETE FROM ROLE_HAS_PERMISSIONS;
/
DELETE FROM PERMISSIONS;
/
DELETE FROM ROLES;
/
DELETE FROM REPORT_TYPES;
/