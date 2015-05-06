-- DROP TABLE IF EXISTS permission_table, wp4_table, ldata_table, pdri_table, pdrigroup_table, 
-- storage_site_table, credential_table, requests_table, successor_table, occurrences_table, features_table, speed_table, 
-- auth_roles_tables, auth_usernames_table, myroles, myuid;

CREATE TABLE pdrigroup_table (
  pdriGroupId SERIAL PRIMARY KEY,
  refCount INT, INDEX(refCount),
  needCheck BOOLEAN NOT NULL DEFAULT TRUE,
  bound BOOLEAN NOT NULL DEFAULT FALSE,
  INDEX(needCheck,bound)
) ENGINE=InnoDB;

CREATE TABLE delete_table (
  id BIGINT UNSIGNED, key(id),
  pdriGroupRef BIGINT UNSIGNED PRIMARY KEY, FOREIGN KEY(pdriGroupRef) REFERENCES pdrigroup_table(pdriGroupId) ON DELETE CASCADE,
  selTimestamp DATETIME, INDEX(selTimestamp)
) ENGINE=InnoDB;

DELIMITER |
CREATE PROCEDURE GET_PDRI_GROUPS_FOR_DELETE (IN lim INT UNSIGNED)
  BEGIN
    DECLARE myid BIGINT UNSIGNED;
    SET myid = UUID_SHORT();
    INSERT INTO delete_table (id, pdriGroupRef, selTimestamp) SELECT myid, pdriGroupId, now() FROM pdrigroup_table LEFT OUTER JOIN delete_table ON pdrigroup_table.pdriGroupId = delete_table.pdriGroupRef WHERE delete_table.pdriGroupRef IS NULL AND pdrigroup_table.refCount = 0 LIMIT lim;
    SELECT pdriGroupRef FROM delete_table WHERE id=myid;
  END
|
DELIMITER ;


CREATE TABLE replicate_table (
  id BIGINT UNSIGNED, key(id),
  pdriGroupRef BIGINT UNSIGNED PRIMARY KEY, FOREIGN KEY(pdriGroupRef) REFERENCES pdrigroup_table(pdriGroupId) ON DELETE CASCADE,
  selTimestamp DATETIME, INDEX(selTimestamp)
) ENGINE=InnoDB;

DELIMITER |
CREATE PROCEDURE GET_PDRI_GROUPS_FOR_REPLICATE (IN cacheId BIGINT UNSIGNED, IN lim INT UNSIGNED)
  BEGIN
    DECLARE myid BIGINT UNSIGNED;
    DECLARE mynow DATETIME;
    SET myid = UUID_SHORT();
    SET mynow = now();
    INSERT INTO replicate_table (id, pdriGroupRef, selTimestamp) SELECT DISTINCT myid, pdriGroupId, mynow FROM pdrigroup_table WHERE bound=FALSE AND refCount>0 AND (EXISTS(SELECT * FROM pdri_table WHERE pdri_table.pdriGroupRef=pdrigroup_table.pdriGroupId AND pdri_table.storageSiteRef=cacheId) OR NOT EXISTS(SELECT * FROM pdri_table JOIN storage_site_table ON pdri_table.storageSiteRef=storage_site_table.storageSiteId WHERE pdri_table.pdriGroupRef=pdrigroup_table.pdriGroupId AND storage_site_table.isCache=TRUE)) LIMIT lim;
    SELECT pdriGroupRef FROM replicate_table WHERE id=myid;
  END
|
DELIMITER ;

CREATE TABLE credential_table (
  credintialId SERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255)
) ENGINE=InnoDB;



CREATE TABLE storage_site_table (
  storageSiteId SERIAL PRIMARY KEY,
  resourceUri VARCHAR(1024),
  credentialRef BIGINT UNSIGNED, FOREIGN KEY(credentialRef) REFERENCES credential_table(credintialId) ON DELETE CASCADE,
  currentNum BIGINT,
  currentSize BIGINT,
  quotaNum BIGINT,
  quotaSize BIGINT,
  isCache BOOLEAN NOT NULL DEFAULT FALSE, INDEX(isCache),
  extra VARCHAR(512),
  encrypt BOOLEAN NOT NULL DEFAULT FALSE, INDEX(encrypt),
  private BOOLEAN NOT NULL DEFAULT FALSE, INDEX (private),
  removing BOOLEAN NOT NULL DEFAULT FALSE, INDEX(removing),
  readOnly BOOLEAN NOT NULL DEFAULT FALSE, INDEX(readOnly)
) ENGINE=InnoDB;

CREATE TABLE pdri_table (
  pdriId SERIAL PRIMARY KEY,
  fileName VARCHAR(255),
  storageSiteRef BIGINT UNSIGNED, FOREIGN KEY(storageSiteRef) REFERENCES storage_site_table(storageSiteId) ON DELETE CASCADE,
  pdriGroupRef BIGINT UNSIGNED, INDEX(pdriGroupRef), FOREIGN KEY(pdriGroupRef) REFERENCES pdrigroup_table(pdriGroupId) ON DELETE CASCADE,
  isEncrypted BOOLEAN NOT NULL DEFAULT FALSE,
  encryptionKey BIGINT UNSIGNED 
) ENGINE=InnoDB;

CREATE TABLE ldata_table (
 uid SERIAL PRIMARY KEY,
 parentRef BIGINT UNSIGNED, INDEX(parentRef),
 ownerId VARCHAR(255), INDEX(ownerId),
 datatype ENUM('logical.file', 'logical.folder'), INDEX(datatype),
 ldName VARCHAR(255), INDEX(ldName), UNIQUE KEY(parentRef, ldName),
 createDate DATETIME NOT NULL,
 modifiedDate DATETIME NOT NULL,
 ldLength BIGINT,
 contentTypesStr VARCHAR(5240),
 pdriGroupRef BIGINT UNSIGNED NOT NULL DEFAULT 0, INDEX(pdriGroupRef),
 isSupervised BOOLEAN NOT NULL DEFAULT FALSE, INDEX(isSupervised), 
 checksum VARCHAR(512),
 lastValidationDate BIGINT NOT NULL DEFAULT 0,
 lockTokenId  VARCHAR(255),
 lockScope  VARCHAR(255),
 lockType  VARCHAR(255),
 lockedByUser  VARCHAR(255),
 lockDepth  VARCHAR(255),
 lockTimeout  BIGINT NOT NULL DEFAULT 0,
 description VARCHAR(1024),
 locationPreference VARCHAR(1024),
 status enum('unavailable', 'corrupted', 'OK'),
 accessDate DATETIME,
 ttlSec int
) ENGINE=InnoDB;

CREATE TABLE pref_table (
  id SERIAL PRIMARY KEY,
  ld_uid BIGINT UNSIGNED, FOREIGN KEY(ld_uid) REFERENCES ldata_table(uid) ON DELETE CASCADE,
  storageSiteRef BIGINT UNSIGNED, FOREIGN KEY(storageSiteRef) REFERENCES storage_site_table(storageSiteId) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE wp4_table (
 id SERIAL PRIMARY KEY,
 local_id BIGINT UNSIGNED, FOREIGN KEY(local_id) REFERENCES ldata_table(uid) ON DELETE SET NULL ,
 global_id VARCHAR(255),
 global_id_dev VARCHAR(255),
 views INT UNSIGNED NOT NULL DEFAULT 0,
 need_update BOOLEAN NOT NULL DEFAULT FALSE, INDEX(need_update),
 need_create BOOLEAN NOT NULL DEFAULT TRUE, INDEX(need_create)
) ENGINE=InnoDB;

CREATE TABLE permission_table (
 id SERIAL PRIMARY KEY,
 permType ENUM('read', 'write'), INDEX(permType),
 ldUidRef BIGINT UNSIGNED, FOREIGN KEY(ldUidRef) REFERENCES ldata_table(uid) ON DELETE CASCADE, INDEX(ldUidRef),
 roleName VARCHAR(255),
 INDEX(permType, ldUidRef, roleName)
) ENGINE=InnoDB;


CREATE TABLE requests_table (
 uid SERIAL PRIMARY KEY,
 methodName VARCHAR(255), INDEX(methodName),
 requestURL TEXT(5240),
 remoteAddr TEXT(5240),
 contentLen BIGINT,
 contentType VARCHAR(5240),
 elapsedTime DOUBLE,
 userName TEXT(5240),
 userAgent VARCHAR(1024),
 timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


CREATE TABLE successor_table (
 uid SERIAL PRIMARY KEY,
 keyVal VARCHAR(1024), -- INDEX(keyVal),
 lobStateID VARCHAR(1024),
 weight DOUBLE
) ENGINE=InnoDB;

CREATE TABLE occurrences_table (
 uid SERIAL PRIMARY KEY,
 keyVal VARCHAR(1024), -- INDEX(keyVal),
 occurrences BIGINT
) ENGINE=InnoDB;



CREATE TABLE features_table (
 uid SERIAL PRIMARY KEY,
 methodName VARCHAR(255),
 ldataRef BIGINT UNSIGNED,
 f1 DOUBLE, 
 f2 DOUBLE,
 f3 DOUBLE,
 f4 DOUBLE,
 f5 DOUBLE,
 f6 DOUBLE,
 f7 DOUBLE,
 f8 DOUBLE,
 f9 DOUBLE,
 f10 DOUBLE,
 f11 DOUBLE,
 f12 DOUBLE,
 f13 DOUBLE,
 f14 DOUBLE,
 f15 DOUBLE,
 f16 DOUBLE,
 f17 DOUBLE,
 f18 DOUBLE,
 f19 DOUBLE,
 f20 DOUBLE,
 f21 DOUBLE,
 f22 DOUBLE,
 f23 DOUBLE,
 f24 DOUBLE,
 f25 DOUBLE
) ENGINE=InnoDB;



CREATE TABLE speed_table (
  id SERIAL PRIMARY KEY,
  src VARCHAR(1024), -- INDEX(src),
  dst VARCHAR(1024), -- INDEX(dst),
  fSize ENUM('S', 'M','L','XL'), INDEX(fSize),
  averageSpeed DOUBLE,
  minSpeed DOUBLE,
  maxSpeed DOUBLE,
  offlineCount BIGINT UNSIGNED
) ENGINE=InnoDB;


DELIMITER |

DROP TRIGGER IF EXISTS on_ldata_delete |
CREATE TRIGGER on_ldata_delete
BEFORE DELETE ON ldata_table 
FOR EACH ROW BEGIN
  IF OLD.datatype = 'logical.file' THEN
    IF OLD.pdriGroupRef != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount-1 WHERE pdriGroupId = OLD.pdriGroupRef;
    END IF;
  END IF;
END|

DROP TRIGGER IF EXISTS on_ldata_update |
CREATE TRIGGER on_ldata_update
AFTER UPDATE ON ldata_table
FOR EACH ROW BEGIN
  IF NEW.datatype = 'logical.file' THEN
    IF NEW.pdriGroupRef != OLD.pdriGroupRef THEN
      IF OLD.pdriGroupRef != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount-1 WHERE pdriGroupId = OLD.pdriGroupRef;
      END IF;
      IF NEW.pdriGroupRef != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount+1 WHERE pdriGroupId = NEW.pdriGroupRef;
      END IF;
    END IF;
  END IF;
  IF (NEW.ldName != OLD.ldName
      OR NEW.modifiedDate != OLD.modifiedDate
      OR NEW.ownerId != NEW.ownerId) THEN
    UPDATE wp4_table SET need_update=TRUE WHERE local_id = NEW.uid;
  END IF;
END|

DROP TRIGGER IF EXISTS on_ldata_insert |
CREATE TRIGGER on_ldata_insert
AFTER INSERT ON ldata_table
FOR EACH ROW BEGIN
  INSERT INTO wp4_table (local_id) VALUES(NEW.uid);
END|

DROP TRIGGER IF EXISTS on_ss_update |
CREATE TRIGGER on_ss_update
AFTER UPDATE ON storage_site_table
FOR EACH ROW BEGIN
  IF new.removing = TRUE THEN
    DELETE FROM pref_table WHERE storageSiteRef=new.storageSiteId;
  END IF;
END|

DROP TRIGGER IF EXISTS on_pref_insert |
CREATE TRIGGER on_pref_insert
AFTER INSERT ON pref_table
FOR EACH ROW BEGIN
  UPDATE pdrigroup_table a JOIN ldata_table ON pdriGroupId=pdriGroupRef SET needCheck=TRUE WHERE uid=new.ld_uid;
END|

DROP TRIGGER IF EXISTS on_pref_delete |
CREATE TRIGGER on_pref_delete
AFTER DELETE ON pref_table
FOR EACH ROW BEGIN
  UPDATE pdrigroup_table a JOIN ldata_table ON pdriGroupId=pdriGroupRef SET needCheck=TRUE WHERE uid=old.ld_uid;
END|

-- DROP TRIGGER IF EXISTS on_pdri_incert |
-- CREATE TRIGGER on_pdri_incert
-- BEFORE INSERT ON pdri_table
-- FOR EACH ROW BEGIN
--   SET NEW.encryptionKey = FLOOR(100000000000000000 + RAND() * 1999999999999999999);
-- END|


DELIMITER ;

INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate) VALUES(1, 'root', 'logical.folder', '', NOW(), NOW());
SET @rootRef = LAST_INSERT_ID();
UPDATE ldata_table SET parentRef = @rootRef WHERE uid = @rootRef;
INSERT INTO permission_table (permType, ldUidRef, roleName) VALUES  ('read', @rootRef, 'admin'),
                                                                    ('write', @rootRef, 'admin');

INSERT INTO  credential_table(username, password) VALUES ('fakeuser', 'fakepass');
SET @credRef = LAST_INSERT_ID();
INSERT INTO storage_site_table(resourceUri, credentialRef, currentNum, currentSize, quotaNum, quotaSize, isCache)
            VALUES('file:///tmp/', @credRef, -1, -1, -1, -1, TRUE);
SET @ssRef = LAST_INSERT_ID();

-- INSERT INTO  credential_table(username, password) VALUES ('dvasunin', 'my-secretpwd');
-- SET @credRef = LAST_INSERT_ID();
-- INSERT INTO storage_site_table(resourceUri, credentialRef, currentNum, currentSize, quotaNum, quotaSize)
--             VALUES('sftp://dvasunin@elab.lab.uvalight.net/home/dvasunin/tmp/lobcder/', @credRef, -1, -1, -1, -1);
-- SET @ssRef = LAST_INSERT_ID();

# Here we createtables for built-in user IDs/roles

CREATE TABLE auth_usernames_table (
    id SERIAL PRIMARY KEY,
    token VARCHAR(1024), -- INDEX(token),
    uname VARCHAR(255), INDEX(uname)
);
CREATE TABLE auth_roles_tables (
    id SERIAL PRIMARY KEY,
    roleName VARCHAR(255), INDEX(roleName),
    unameRef BIGINT UNSIGNED, FOREIGN KEY(unameRef) REFERENCES auth_usernames_table(id) ON DELETE CASCADE
);

-- INSERT INTO auth_usernames_table(token, uname) VALUES ('admin', 'RoomC3156');
-- SET @authUserNamesRef = LAST_INSERT_ID();
-- INSERT INTO auth_roles_tables(roleName, unameRef) VALUES  ('admin',     @authUserNamesRef),
--                                                           ('other',     @authUserNamesRef),
--                                                           ('megarole',  @authUserNamesRef);


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

DELIMITER |

DROP PROCEDURE IF EXISTS copyFolderContentProc |
CREATE PROCEDURE copyFolderContentProc(IN newOwner VARCHAR(255), IN newOwnerRoles VARCHAR(10240), IN rdPerm VARCHAR(10240), IN wrPerm VARCHAR(10240), IN oldParentRef BIGINT UNSIGNED, IN newParentRef BIGINT UNSIGNED)
MAIN_BLOCK: BEGIN
  DECLARE a INT Default 0;  
  DECLARE str VARCHAR(255);
  DECLARE myStr VARCHAR(10240);
  DECLARE curStr VARCHAR(10240);
  DECLARE strLen    INT DEFAULT 0;
  DECLARE SubStrLen INT DEFAULT 0;
  DECLARE curuid BIGINT default 0; 
  DECLARE done INT DEFAULT FALSE;
  DECLARE roleName1 VARCHAR(255);
  DECLARE uid1, uidNew BIGINT UNSIGNED;
  DECLARE ownerId1 VARCHAR(255);
  DECLARE ldName1 VARCHAR(255);
  DECLARE ldLength1 BIGINT;
  DECLARE contentTypesStr1 VARCHAR(10240);
  DECLARE pdriGroupRef1 BIGINT UNSIGNED;
  DECLARE permissionToReadCursor CURSOR FOR SELECT roleName FROM permission_table WHERE ldUidRef = uid1 AND permType = 'read';
  DECLARE folderCursor CURSOR FOR SELECT uid,ownerId,ldName,ldLength,contentTypesStr,pdriGroupRef FROM ldata_table WHERE ldata_table.parentRef = oldParentRef AND ldata_table.datatype = 'logical.file';
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
    FETCH folderCursor INTO uid1, ownerId1, ldName1, ldLength1, contentTypesStr1, pdriGroupRef1;
    IF done THEN
      LEAVE loop_read_folder;
    END IF;
    IF ownerId1 = newOwner OR FIND_IN_SET('admin', newOwnerRoles) THEN
        INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef) VALUES (newParentRef, newOwner, 'logical.file', ldName1, NOW(), NOW(), ldLength1, contentTypesStr1, pdriGroupRef1);
        SET uidNew = LAST_INSERT_ID();
        UPDATE pdrigroup_table SET refCount=refCount+1 WHERE pdriGroupId = pdriGroupRef1;
        SET a = 0;
       insert_read_permission_loop:
        LOOP
          SET a=a+1;
          SET str=SPLIT_STR(rdPerm, ',', a);
          IF str='' THEN
            LEAVE insert_read_permission_loop;
          END IF;
          INSERT INTO permission_table(permType, ldUidRef, roleName) VALUES ('read', uidNew, str);
        END LOOP insert_read_permission_loop;
        SET a = 0;
       insert_wr_permission_loop:
        LOOP
          SET a=a+1;
          SET str=SPLIT_STR(wrPerm, ',', a);
          IF str='' THEN
            LEAVE insert_wr_permission_loop;
          END IF;
          INSERT INTO permission_table(permType, ldUidRef, roleName) VALUES ('write', uidNew, str);
        END LOOP insert_wr_permission_loop;     
    ELSE
        OPEN permissionToReadCursor;
       loop_permission_read:
        LOOP
            FETCH permissionToReadCursor INTO roleName1;
            IF done THEN
                SET done = FALSE;
                CLOSE permissionToReadCursor;
                LEAVE loop_permission_read; 
            END IF;
            IF FIND_IN_SET(roleName1, newOwnerRoles) THEN
                INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef) VALUES (newParentRef, newOwner, 'logical.file', ldName1, NOW(), NOW(), ldLength1, contentTypesStr1, pdriGroupRef1);
                SET uidNew = LAST_INSERT_ID();
                UPDATE pdrigroup_table SET refCount=refCount+1 WHERE pdriGroupId = pdriGroupRef1;
                SET a = 0;
               insert_read_permission_loop1:
                LOOP
                    SET a=a+1;
                    SET str=SPLIT_STR(rdPerm, ',', a);
                    IF str='' THEN
                        LEAVE insert_read_permission_loop1;
                    END IF;
                    INSERT INTO permission_table(permType, ldUidRef, roleName) VALUES ('read', uidNew, str);
                END LOOP insert_read_permission_loop1;
                SET a = 0;
               insert_wr_permission_loop1:
                LOOP
                    SET a=a+1;
                    SET str=SPLIT_STR(wrPerm, ',', a);
                    IF str='' THEN
                        LEAVE insert_wr_permission_loop1;
                    END IF;
                    INSERT INTO permission_table(permType, ldUidRef, roleName) VALUES ('write', uidNew, str);
                END LOOP insert_wr_permission_loop1;
                CLOSE permissionToReadCursor;
                LEAVE loop_permission_read;
            END IF;
        END LOOP loop_permission_read;
    END IF;
  END LOOP loop_read_folder;
END|

DROP PROCEDURE IF EXISTS updatePermissionsDirProc |
CREATE PROCEDURE updatePermissionsDirProc(IN mowner VARCHAR(255), IN mroles VARCHAR(10240), IN newowner VARCHAR(255),
                                          IN rroles VARCHAR(10240), IN wroles VARCHAR(10240), IN mparent BIGINT)
MAIN_BLOCK: BEGIN
  DECLARE a INT DEFAULT 0;
  DECLARE isadmin INT DEFAULT 0;
  DECLARE str VARCHAR(255);

  
  CREATE TEMPORARY TABLE myroles (
    mrole VARCHAR(255)
  );


  CREATE TEMPORARY TABLE myuid (
    muid BIGINT UNSIGNED
  );

  SET a = 0;
   insert_myroles_loop:
    LOOP
      SET a = a + 1;
      SET str = SPLIT_STR(mroles, ",", a);
      IF str = '' THEN
        LEAVE insert_myroles_loop;
      END IF;
      INSERT INTO myroles (mrole) VALUES (str);
    END LOOP insert_myroles_loop;
    SET isadmin = FIND_IN_SET('admin', mroles);

    INSERT INTO myuid(muid)
    SELECT uid FROM ldata_table
    WHERE parentRef = mparent AND (ownerId = mowner OR isadmin != 0 OR
        EXISTS(
           SELECT 1 FROM permission_table JOIN myroles ON permission_table.roleName = myroles.mrole
           WHERE permission_table.permType = 'write' AND permission_table.ldUidRef = ldata_table.uid
        )
    );

    DELETE FROM permission_table
    WHERE ldUidRef IN (SELECT muid FROM myuid);
    SET a = 0;
   insert_rroles_loop:
    LOOP
    SET a = a + 1;
    SET str = SPLIT_STR(rroles, ",", a);
    IF str = ''
    THEN
      LEAVE insert_rroles_loop;
    END IF;
    INSERT INTO permission_table (permType, ldUidRef, roleName)
    SELECT 'read', muid, str FROM myuid;
    END LOOP insert_rroles_loop;

    SET a = 0;
   insert_wroles_loop:
    LOOP
    SET a = a + 1;
    SET str = SPLIT_STR(wroles, ",", a);
    IF str = ''
    THEN
      LEAVE insert_wroles_loop;
    END IF;
    INSERT INTO permission_table (permType, ldUidRef, roleName)
    SELECT 'write', muid, str FROM myuid;
    END LOOP insert_wroles_loop;

    UPDATE ldata_table SET ownerId = newowner
    WHERE uid IN (SELECT muid FROM myuid);

END

|

DELIMITER ;


CREATE TABLE IF NOT EXISTS tokens_table (
  short_tkt CHAR(12) NOT NULL PRIMARY KEY,
  userId VARCHAR(255), INDEX(userId),
  long_tkt VARCHAR(5240) NOT NULL, INDEX(long_tkt),
  exp_date DATETIME, INDEX(exp_date)
) ENGINE=InnoDB;


DELIMITER |
CREATE EVENT IF NOT EXISTS e_tokens_sweep
  ON SCHEDULE
    EVERY 600 SECOND
DO
  BEGIN
    DELETE FROM tokens_table WHERE exp_date < NOW();
    DELETE FROM delete_table WHERE timestampdiff(MINUTE, selTimestamp, now()) > 15;
    DELETE FROM replicate_table WHERE timestampdiff(MINUTE, selTimestamp, now()) > 30;
  END
|
DELIMITER ;


DROP EVENT IF EXISTS ttl_sweep;

DELIMITER |
CREATE EVENT IF NOT EXISTS ttl_sweep
  ON SCHEDULE
    EVERY 120 SECOND
DO
  BEGIN
    DECLARE countRow INT;
        DELETE FROM ldata_table WHERE datatype = 'logical.file' AND  ttlSec IS NOT NULL AND accessDate IS NOT NULL AND timestampdiff(SECOND, accessDate, now()) > ttlSec;
    del_fold_loop:
    LOOP
      DELETE FROM ldata_table WHERE uid in (
        SELECT * FROM (
                        SELECT * FROM (
                          (SELECT uid FROM ldata_table WHERE datatype = 'logical.folder' AND  ttlSec IS NOT NULL AND timestampdiff(SECOND, accessDate, now()) > ttlSec) AS t)
                        WHERE (SELECT COUNT(*) FROM ldata_table WHERE parentRef=t.uid) = 0)
          AS qq);
      SET countRow =  ROW_COUNT();
      IF countRow = 0 THEN
        LEAVE del_fold_loop;
      END IF;
    END LOOP del_fold_loop;
--   END;
END 
|
DELIMITER ;

SET GLOBAL event_scheduler = ON;

