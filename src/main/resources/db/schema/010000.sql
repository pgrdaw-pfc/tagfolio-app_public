create table PERMISSIONS
(
    ID   NUMBER(19) generated as identity
        primary key,
    NAME VARCHAR2(255 char) not null
        unique
        check (name in ('READ_OWN_IMAGES', 'UPLOAD_IMAGES', 'DELETE_IMAGES', 'MANAGE_USERS', 'VIEW_ADMIN_DASHBOARD',
                        'READ_SHARED_IMAGES'))
)
    /

create table REPORT_TYPES
(
    ID   NUMBER(19) generated as identity
        primary key,
    NAME VARCHAR2(255 char) not null
        unique
)
    /

create table ROLES
(
    ID   NUMBER(19) generated as identity
        primary key,
    NAME VARCHAR2(255 char) not null
        unique
        check (name in ('ADMIN', 'USER', 'ANONYMOUS'))
)
    /

create table ROLE_HAS_PERMISSIONS
(
    PERMISSION_ID NUMBER(19) not null
        constraint FKJA79CYXMMGCYGCCBFEBN1QPWL
            references PERMISSIONS,
    ROLE_ID       NUMBER(19) not null
        constraint FKRLBIPAJREN817EX79EB38GXF7
            references ROLES,
    primary key (PERMISSION_ID, ROLE_ID)
)
    /

create table TAGS
(
    CREATED_AT TIMESTAMP(6),
    ID         NUMBER(19) generated as identity
        primary key,
    UPDATED_AT TIMESTAMP(6),
    NAME       VARCHAR2(255 char) not null
        unique
)
    /

create table USERS
(
    CREATED_AT TIMESTAMP(6)       not null,
    ID         NUMBER(19) generated as identity
        primary key,
    UPDATED_AT TIMESTAMP(6)       not null,
    EMAIL      VARCHAR2(255 char) not null
        unique,
    PASSWORD   VARCHAR2(255 char) not null
)
    /

create table FILTERS
(
    ID         NUMBER(19) generated as identity
        primary key,
    USER_ID    NUMBER(19)         not null
        constraint FKCV2E2YMYXQ6NCTW5NNOYTTVI
            references USERS,
    NAME       VARCHAR2(255 char) not null,
    EXPRESSION CLOB               not null
)
    /

create table IMAGES
(
    RATING              NUMBER(10),
    CREATED_AT          TIMESTAMP(6),
    FILE_MODIFIED_AT    TIMESTAMP(6),
    ID                  NUMBER(19) generated as identity
        primary key,
    UPDATED_AT          TIMESTAMP(6),
    USER_ID             NUMBER(19)
        constraint FK13LJQFRFWBYVNSDHIHWTA8CPR
            references USERS,
    ORIGINAL_FILE_NAME  VARCHAR2(255 char) not null,
    THUMBNAIL_FILE_NAME VARCHAR2(255 char) not null,
    EXIFTOOL            CLOB
)
    /

create table IMAGE_TAG
(
    IMAGE_ID NUMBER(19) not null
        constraint FK6Q9WUVP5J846QTQOD6XU3GMA1
            references IMAGES,
    TAG_ID   NUMBER(19) not null
        constraint FKMC904TMJB2DWC2IJDRWIBE43S
            references TAGS,
    primary key (IMAGE_ID, TAG_ID)
)
    /

create table REPORTS
(
    CREATION_DATE  TIMESTAMP(6)       not null,
    ID             NUMBER(19) generated as identity
        primary key,
    REPORT_TYPE_ID NUMBER(19)         not null
        constraint FKO91X31SIER8WRDTO7U6PAIK7O
            references REPORT_TYPES,
    USER_ID        NUMBER(19)
        constraint FK2O32RER9HFWEEYLG7X8UT8RJ2
            references USERS,
    NAME           VARCHAR2(255 char) not null
)
    /

create table REPORT_IMAGES
(
    SORTING_ORDER NUMBER(10),
    IMAGE_ID      NUMBER(19) not null
        constraint FKHHCHYIFJ5IALDL3VHPV1SYX71
            references IMAGES,
    REPORT_ID     NUMBER(19) not null
        constraint FKIO33XL5NYHE7FV6E8ME83DDJ5
            references REPORTS,
    primary key (IMAGE_ID, REPORT_ID)
)
    /

create table SHARED_FILTERS
(
    CREATED_AT   TIMESTAMP(6)      not null,
    EXPIRES_AT   TIMESTAMP(6),
    FILTER_ID    NUMBER(19)        not null
        constraint FKBR4QHHBISFBBE7H5RSCT70DWE
            references FILTERS,
    ID           NUMBER(19) generated as identity
        primary key,
    TOKEN        VARCHAR2(36 char) not null
        unique,
    CONTENT_HASH VARCHAR2(64 char) not null
        unique
)
    /

create table SHARED_REPORTS
(
    CREATION_DATE TIMESTAMP(6)       not null,
    ID            NUMBER(19) generated as identity
        primary key,
    REPORT_ID     NUMBER(19)         not null
        unique
        constraint FK89MCM1TTT8YXWI896U49FTDDX
            references REPORTS,
    CONTENT_HASH  VARCHAR2(255 char) not null
        unique,
    TOKEN         VARCHAR2(255 char) not null
        unique
)
    /

create table USER_HAS_ROLES
(
    ROLE_ID NUMBER(19) not null
        constraint FK6PF0MFSIQ1SVS4PWGXGVF0WYL
            references ROLES,
    USER_ID NUMBER(19) not null
        constraint FKTLLBJ605LWIXU0TY2Y8H3BAQ4
            references USERS,
    primary key (ROLE_ID, USER_ID)
)
    /