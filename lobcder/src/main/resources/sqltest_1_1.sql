DROP TRIGGER IF EXISTS on_ldata_insert;

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

CALL copyFolderContentProc('faker', 'A,muz,qwerty', 'other,azer,vse', 'admin,nikto', '/a/b', '/d');