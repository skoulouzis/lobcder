DROP TABLE IF EXISTS permission_table, ldata_table, pdri_table, pdrigroup_table, role_to_ss_table, storage_site_table, credential_table;

CREATE TABLE pdrigroup_table (
  groupId SERIAL PRIMARY KEY,
  refCount INT, INDEX(refCount)
);

CREATE TABLE credential_table (
  credintialId SERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255)
);

CREATE TABLE storage_site_table (
  storageSiteId SERIAL PRIMARY KEY,
  resourceURI VARCHAR(1024),
  credentialRef BIGINT UNSIGNED, FOREIGN KEY(credentialRef) REFERENCES credential_table(credintialId) ON DELETE CASCADE,
  currentNum BIGINT,
  currentSize BIGINT,
  quotaNum BIGINT,
  quotaSize BIGINT
);

CREATE TABLE pdri_table (
  pdriId SERIAL PRIMARY KEY,
  url VARCHAR(1024),
  storageSiteId BIGINT UNSIGNED,
  pdriGroupId BIGINT UNSIGNED, INDEX(pdriGroupId)
);

CREATE TABLE ldata_table (
 uid SERIAL PRIMARY KEY,
 ownerId VARCHAR(255), INDEX(ownerId),
 datatype ENUM('logical.file', 'logical.folder', 'logical.data'), INDEX(datatype),
 ld_name VARCHAR(255), INDEX(ld_name),
 parent VARCHAR(5240), INDEX(parent),
 createDate DATETIME NOT NULL,
 modifiedDate DATETIME NOT NULL,
 ld_length BIGINT,
 contentTypesStr VARCHAR(5240),
 pdriGroupId BIGINT UNSIGNED,
 isSupervised BOOLEAN NOT NULL DEFAULT FALSE, INDEX(isSupervised), 
 checksum BIGINT NOT NULL DEFAULT 0,
 lastValidationDate BIGINT NOT NULL DEFAULT 0,
 lockTokenID  VARCHAR(255),
 lockScope  VARCHAR(255),
 lockType  VARCHAR(255),
 lockedByUser  VARCHAR(255),
 lockDepth  VARCHAR(255),
 lockTimeout  BIGINT NOT NULL DEFAULT 0,
 description VARCHAR(255)
);

CREATE TABLE permission_table (
 id SERIAL PRIMARY KEY,
 perm_type ENUM('read', 'write'), index(perm_type),
 ld_uid_ref BIGINT UNSIGNED, FOREIGN KEY(ld_uid_ref) REFERENCES ldata_table(uid) ON DELETE CASCADE, INDEX(ld_uid_ref),
 role_name VARCHAR(255)
);

DELIMITER |
DROP TRIGGER IF EXISTS on_ldata_delete |
CREATE TRIGGER on_ldata_delete
BEFORE DELETE ON ldata_table 
FOR EACH ROW BEGIN
  IF OLD.datatype = 'logical.file' THEN
    IF OLD.pdriGroupId != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount-1 WHERE groupId = OLD.pdriGroupId;
    END IF;
  END IF;
END|

DELIMITER ;

DROP FUNCTION IF EXISTS SPLIT_STR;
CREATE FUNCTION SPLIT_STR(
  x VARCHAR(10240),
  delim VARCHAR(12),
  pos INT
)
RETURNS VARCHAR(255)
RETURN REPLACE(SUBSTRING(SUBSTRING_INDEX(x, delim, pos),
       LENGTH(SUBSTRING_INDEX(x, delim, pos -1)) + 1),
       delim, '');

DELIMITER $$

DROP PROCEDURE IF EXISTS copyFolderContentProc $$
CREATE PROCEDURE copyFolderContentProc(IN newOwner VARCHAR(255), IN newOwnerRoles VARCHAR(10240), IN rdPerm VARCHAR(10240), IN wrPerm VARCHAR(10240), IN oldParent VARCHAR(10240), IN newParent VARCHAR(10240))
MAIN_BLOCK: BEGIN
  DECLARE a INT Default 0;  
  DECLARE str VARCHAR(255);
  DECLARE myStr VARCHAR(10240);
  DECLARE curStr VARCHAR(10240);
  DECLARE strLen    INT DEFAULT 0;
  DECLARE SubStrLen INT DEFAULT 0;
  DECLARE curuid BIGINT default 0; 
  DECLARE done INT DEFAULT FALSE;
  DECLARE role_name1 VARCHAR(255);
  DECLARE uid1, uidNew BIGINT UNSIGNED;
  DECLARE ownerId1 VARCHAR(255);
  DECLARE ld_name1 VARCHAR(255);
  DECLARE ld_length1 BIGINT;
  DECLARE contentTypesStr1 VARCHAR(10240);
  DECLARE pdriGroupId1 BIGINT UNSIGNED;
  DECLARE permissionToReadCursor CURSOR FOR SELECT role_name FROM permission_table WHERE ld_uid_ref = uid1;
  DECLARE folderCursor CURSOR FOR SELECT uid,ownerId,ld_name,ld_length,contentTypesStr,pdriGroupId FROM ldata_table WHERE ldata_table.parent = oldParent AND ldata_table.datatype = 'logical.file';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
  
  IF rdPerm IS NULL THEN
    SET rdPerm = '';
  END IF;
  IF wrPerm IS NULL THEN
    SET wrPerm = '';
  END IF;  

  OPEN folderCursor;
loop_read_folder:
  LOOP
    FETCH folderCursor INTO uid1, ownerId1, ld_name1, ld_length1, contentTypesStr1, pdriGroupId1;  
    IF done THEN
      LEAVE loop_read_folder;
    END IF;
    IF ownerId1 = newOwner THEN
        INSERT INTO ldata_table(ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId) VALUES (newOwner, 'logical.file', ld_name1, newParent, NOW(), NOW(), ld_length1, contentTypesStr1, pdriGroupId1);
        SET uidNew = LAST_INSERT_ID();
        UPDATE pdrigroup_table SET refCount=refCount+1 WHERE groupId = pdriGroupId1;
        SET a = 0;
       insert_read_permission_loop:
        LOOP
          SET a=a+1;
          SET str=SPLIT_STR(rdPerm,",",a);
          IF str='' THEN
            LEAVE insert_read_permission_loop;
          END IF;
          INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) VALUES ('read', uidNew, str);
        END LOOP insert_read_permission_loop;
        SET a = 0;
       insert_wr_permission_loop:
        LOOP
          SET a=a+1;
          SET str=SPLIT_STR(wrPerm,",",a);
          IF str='' THEN
            LEAVE insert_wr_permission_loop;
          END IF;
          INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) VALUES ('write', uidNew, str);
        END LOOP insert_wr_permission_loop;     
    ELSE
        OPEN permissionToReadCursor;
       loop_permission_read:
        LOOP
            FETCH permissionToReadCursor INTO role_name1;
            IF done THEN
                SET done = FALSE;
                CLOSE permissionToReadCursor;
                LEAVE loop_permission_read; 
            END IF;
            IF FIND_IN_SET(role_name1, newOwnerRoles) THEN
                INSERT INTO ldata_table(ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId) VALUES (newOwner, 'logical.file', ld_name1, newParent, NOW(), NOW(), ld_length1, contentTypesStr1, pdriGroupId1);
                SET uidNew = LAST_INSERT_ID();
                UPDATE pdrigroup_table SET refCount=refCount+1 WHERE groupId = pdriGroupId1;
                SET a = 0;
            insert_read_permission_loop1:
                LOOP
                    SET a=a+1;
                    SET str=SPLIT_STR(rdPerm,",",a);
                    IF str='' THEN
                        LEAVE insert_read_permission_loop1;
                    END IF;
                    INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) VALUES ('read', uidNew, str);
                END LOOP insert_read_permission_loop1;
                SET a = 0;
            insert_wr_permission_loop1:
                LOOP
                    SET a=a+1;
                    SET str=SPLIT_STR(wrPerm,",",a);
                    IF str='' THEN
                        LEAVE insert_wr_permission_loop1;
                    END IF;
                    INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) VALUES ('write', uidNew, str);
                END LOOP insert_wr_permission_loop1;                    
            END IF;
        END LOOP loop_permission_read;
    END IF;
  END LOOP loop_read_folder;
END
$$

DELIMITER ;


CREATE TABLE role_to_ss_table (
 id SERIAL PRIMARY KEY,
 role_name VARCHAR(255), index(role_name),
 ss_id BIGINT unsigned, FOREIGN KEY(ss_id) REFERENCES storage_site_table(storageSiteId) ON DELETE CASCADE
);

INSERT INTO ldata_table(ownerId, datatype, ld_name, parent, createDate, modifiedDate) VALUES('root', 'logical.folder', '', '', NOW(), NOW());
SET @rootID = LAST_INSERT_ID();

INSERT INTO permission_table (perm_type, ld_uid_ref, role_name) VALUES  ('read', @rootID, 'other'),
                                                                        ('read', @rootID, 'admin'),
                                                                        ('write', @rootID, 'admin');
INSERT INTO  credential_table(username, password) VALUES ('fakeusername', 'fakepassword');
SET @credID = LAST_INSERT_ID();
INSERT INTO storage_site_table(resourceURI, credentialRef, currentNum, currentSize, quotaNum, quotaSize)
            VALUES('file://localhost/tmp/', @credID, -1, -1, -1, -1);
SET @ssId = LAST_INSERT_ID();
INSERT INTO role_to_ss_table(role_name, ss_id) values   ('admin', @ssId),
                                                        ('other', @ssId);
# Here we createtables for built-in user IDs/roles
CREATE TABLE IF NOT EXISTS auth_usernames_table (
    id SERIAL PRIMARY KEY,
    token VARCHAR(1024), index(token),
    uname VARCHAR(255), index(uname)
);

CREATE TABLE IF NOT EXISTS auth_roles_tables (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(255), index(role_name),
    uname_id BIGINT unsigned, FOREIGN KEY(uname_id) REFERENCES auth_usernames_table(id) ON DELETE CASCADE
)

