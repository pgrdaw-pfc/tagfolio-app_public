-- This script populates a development environment with a comprehensive set of sample data,
-- simulating multiple users and their content.
-- It is NOT part of the automated Flyway migration pipeline and should be run manually.

-- Create additional users
INSERT INTO USERS (ID, EMAIL, PASSWORD, CREATED_AT, UPDATED_AT)
VALUES (100, 'alice@example.com', 'password123', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO USERS (ID, EMAIL, PASSWORD, CREATED_AT, UPDATED_AT)
VALUES (101, 'bob@example.com', 'password123', SYSTIMESTAMP, SYSTIMESTAMP);
/

-- Create a variety of tags
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (100, 'landscape', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (101, 'city', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (102, 'portrait', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (103, 'animal', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (104, 'nature', SYSTIMESTAMP, SYSTIMESTAMP);
/
INSERT INTO TAGS (ID, NAME, CREATED_AT, UPDATED_AT) VALUES (105, 'travel', SYSTIMESTAMP, SYSTIMESTAMP);
/

-- Add images for Alice (user 100)
INSERT INTO IMAGES (ID, USER_ID, ORIGINAL_FILE_NAME, THUMBNAIL_FILE_NAME, CREATED_AT, IMPORTED_AT, MODIFIED_AT, RATING)
VALUES (100, 100, 'mountain_vista.jpg', 'mountain_vista_thumb.jpg', SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP, 5);
/
INSERT INTO IMAGES (ID, USER_ID, ORIGINAL_FILE_NAME, THUMBNAIL_FILE_NAME, CREATED_AT, IMPORTED_AT, MODIFIED_AT, RATING)
VALUES (101, 100, 'city_skyline.jpg', 'city_skyline_thumb.jpg', SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP, 4);
/

-- Add images for Bob (user 101)
INSERT INTO IMAGES (ID, USER_ID, ORIGINAL_FILE_NAME, THUMBNAIL_FILE_NAME, CREATED_AT, IMPORTED_AT, MODIFIED_AT, RATING)
VALUES (102, 101, 'friendly_dog.jpg', 'friendly_dog_thumb.jpg', SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP, 5);
/

-- Associate tags with images
INSERT INTO IMAGE_TAG (IMAGE_ID, TAG_ID) VALUES (100, 100); -- mountain_vista.jpg -> landscape
INSERT INTO IMAGE_TAG (IMAGE_ID, TAG_ID) VALUES (100, 104); -- mountain_vista.jpg -> nature
INSERT INTO IMAGE_TAG (IMAGE_ID, TAG_ID) VALUES (100, 105); -- mountain_vista.jpg -> travel
INSERT INTO IMAGE_TAG (IMAGE_ID, TAG_ID) VALUES (101, 101); -- city_skyline.jpg -> city
INSERT INTO IMAGE_TAG (IMAGE_ID, TAG_ID) VALUES (102, 103); -- friendly_dog.jpg -> animal
/

-- Create a saved filter for Alice
INSERT INTO FILTERS (ID, USER_ID, NAME, EXPRESSION)
VALUES (100, 100, 'My Travel Photos', 'tag:travel');
/

-- Create a report for Bob
INSERT INTO REPORTS (ID, USER_ID, REPORT_TYPE_ID, NAME, CREATION_DATE)
VALUES (100, 101, 1, 'Animal Pictures Report', SYSTIMESTAMP); -- report_type_id 1 = compact
/

-- Add images to Bob's report
INSERT INTO REPORT_IMAGES (REPORT_ID, IMAGE_ID, SORTING_ORDER)
VALUES (100, 102, 1);
/