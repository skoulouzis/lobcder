 
 ALTER TABLE ldata_table ADD isSupervised BOOLEAN NOT NULL DEFAULT FALSE; 
 ALTER TABLE ldata_table ADD INDEX(isSupervised);
 ALTER TABLE ldata_table ADD checksum BIGINT NOT NULL DEFAULT 0;
 ALTER TABLE ldata_table ADD lastValidationDate BIGINT NOT NULL DEFAULT 0;
 ALTER TABLE ldata_table ADD lockTokenID VARCHAR(255);
 ALTER TABLE ldata_table ADD lockScope VARCHAR(255);
 ALTER TABLE ldata_table ADD lockType VARCHAR(255);
 ALTER TABLE ldata_table ADD lockedByUser VARCHAR(255);
 ALTER TABLE ldata_table ADD lockDepth VARCHAR(255);
 ALTER TABLE ldata_table ADD lockTimeout BIGINT NOT NULL DEFAULT 0;
 ALTER TABLE ldata_table ADD description VARCHAR(255);
 ALTER TABLE ldata_table ADD locationPreference VARCHAR(255);
 ALTER TABLE ldata_table ADD status enum('unavailable', 'corrupted', 'OK');
 ALTER TABLE ldata_table ADD accessDate DATETIME;
 ALTER TABLE ldata_table ADD ttlSec int;
 ALTER TABLE tokens_table ADD userId VARCHAR(255), INDEX(userId);


# Here we createtables for built-in user IDs/roles
CREATE TABLE IF NOT EXISTS auth_usernames_table (
    id SERIAL PRIMARY KEY,
    uname VARCHAR(255), index(uname)
);

CREATE TABLE IF NOT EXISTS auth_roles_tables (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(255), index(role_name),
    uname_id BIGINT unsigned, FOREIGN KEY(uname_id) REFERENCES auth_usernames_table(id) ON DELETE CASCADE
);

DELIMITER $$

DROP PROCEDURE IF EXISTS updatePermissionsProc $$
CREATE PROCEDURE updatePermissionsProc(IN mowner VARCHAR(255), IN mroles VARCHAR(10240), IN newowner VARCHAR(255), IN rroles VARCHAR(10240), IN wroles VARCHAR(10240),  IN mparent VARCHAR(10240))
MAIN_BLOCK: BEGIN
    DECLARE a INT Default 0; 
    DECLARE isadmin INT Default 0;
    DECLARE str VARCHAR(255);
    DECLARE parent1 VARCHAR(10240);
    DECLARE name1 VARCHAR(255);

    DROP TABLE IF EXISTS myroles;
    CREATE temporary table myroles(
      mrole VARCHAR(255)
    );

    DROP TABLE IF EXISTS myuid;
    CREATE temporary table myuid(
      muid BIGINT UNSIGNED
    );

    SET a=0;
    insert_myroles_loop: 
    LOOP
        SET a=a+1;
        SET str=SPLIT_STR(mroles,",",a);
        IF str='' THEN
            LEAVE insert_myroles_loop;
        END IF;
        INSERT INTO myroles(mrole) VALUES (str);
    END LOOP insert_myroles_loop;
    SET isadmin = FIND_IN_SET('admin', mroles);
    SET name1 = SUBSTRING_INDEX(TRIM(TRAILING '/' FROM mparent), '/', -1);
    SET parent1 = TRIM(TRAILING '/' FROM REVERSE(SUBSTR(REVERSE(TRIM(TRAILING '/' FROM mparent)), INSTR(REVERSE(TRIM(TRAILING '/' FROM mparent)), '/'))));

    INSERT INTO myuid(muid) 
    SELECT uid FROM ldata_table
    WHERE ldata_table.parent = parent1 AND ldata_table.ld_name = name1 AND (ldata_table.ownerId = mowner OR isadmin != 0 OR 
        EXISTS(
            SELECT 1 FROM permission_table JOIN myroles ON permission_table.role_name = myroles.mrole 
            WHERE permission_table.perm_type = 'write' AND permission_table.ld_uid_ref = ldata_table.uid
        )
    );

    INSERT INTO myuid(muid) 
    SELECT uid FROM ldata_table
    WHERE ldata_table.parent LIKE CONCAT(mparent, '%') AND (ldata_table.ownerId = mowner OR isadmin != 0 OR 
        EXISTS(
            SELECT 1 FROM permission_table JOIN myroles ON permission_table.role_name = myroles.mrole 
            WHERE permission_table.perm_type = 'write' AND permission_table.ld_uid_ref = ldata_table.uid
        )
    );
    DELETE FROM permission_table WHERE permission_table.ld_uid_ref IN (SELECT muid FROM myuid);
    SET a=0;
    insert_rroles_loop: 
    LOOP
        SET a=a+1;
        SET str=SPLIT_STR(rroles,",",a);
        IF str='' THEN
            LEAVE insert_rroles_loop;
        END IF;
        INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) 
        SELECT 'read', muid, str FROM myuid;
    END LOOP insert_rroles_loop;

    SET a=0;
    insert_wroles_loop: 
    LOOP
        SET a=a+1;
        SET str=SPLIT_STR(wroles,",",a);
        IF str='' THEN
            LEAVE insert_wroles_loop;
        END IF;
        INSERT INTO permission_table(perm_type, ld_uid_ref, role_name) 
        SELECT 'write', muid, str FROM myuid;
    END LOOP insert_wroles_loop;

    UPDATE ldata_table SET ownerId=newowner WHERE uid IN (SELECT muid FROM myuid);

END

$$

DELIMITER ;


DELIMITER $$

DROP PROCEDURE IF EXISTS updateDriFlagProc $$
CREATE PROCEDURE updateDriFlagProc(IN mowner VARCHAR(255), IN mroles VARCHAR(10240), IN flag BOOLEAN, IN mparent VARCHAR(10240))
MAIN_BLOCK: BEGIN
    DECLARE a INT Default 0; 
    DECLARE isadmin INT Default 0;
    DECLARE str VARCHAR(255);
    DECLARE parent1 VARCHAR(10240);
    DECLARE name1 VARCHAR(255);

    DROP TABLE IF EXISTS myroles;
    CREATE temporary table myroles(
      mrole VARCHAR(255)
    );

    SET a=0;
    insert_myroles_loop: 
    LOOP
        SET a=a+1;
        SET str=SPLIT_STR(mroles,",",a);
        IF str='' THEN
            LEAVE insert_myroles_loop;
        END IF;
        INSERT INTO myroles(mrole) VALUES (str);
    END LOOP insert_myroles_loop;
    SET isadmin = FIND_IN_SET('admin', mroles);
    SET name1 = SUBSTRING_INDEX(TRIM(TRAILING '/' FROM mparent), '/', -1);
    SET parent1 = TRIM(TRAILING '/' FROM REVERSE(SUBSTR(REVERSE(TRIM(TRAILING '/' FROM mparent)), INSTR(REVERSE(TRIM(TRAILING '/' FROM mparent)), '/'))));

    UPDATE ldata_table SET isSupervised = flag
    WHERE ldata_table.parent = parent1 AND ldata_table.ld_name = name1 AND (ldata_table.ownerId = mowner OR isadmin != 0 OR 
        EXISTS(
            SELECT 1 FROM permission_table JOIN myroles ON permission_table.role_name = myroles.mrole 
            WHERE permission_table.perm_type = 'write' AND permission_table.ld_uid_ref = ldata_table.uid
        )
    );

    UPDATE ldata_table SET isSupervised = flag
    WHERE ldata_table.parent LIKE CONCAT(mparent, '%') AND (ldata_table.ownerId = mowner OR isadmin != 0 OR 
        EXISTS(
            SELECT 1 FROM permission_table JOIN myroles ON permission_table.role_name = myroles.mrole 
            WHERE permission_table.perm_type = 'write' AND permission_table.ld_uid_ref = ldata_table.uid
        )
    );
END

$$

DELIMITER ;


ALTER TABLE pdrigroup_table ADD needCheck BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE pdrigroup_table ADD bound BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pdrigroup_table ADD INDEX(needCheck,bound);
ALTER TABLE storage_site_table ADD private BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE storage_site_table ADD removing BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE storage_site_table ADD INDEX (private,removing);


CREATE TABLE IF NOT EXISTS  pref_table (
  id SERIAL PRIMARY KEY, 
  ld_uid BIGINT UNSIGNED, FOREIGN KEY(ld_uid) REFERENCES ldata_table(uid) ON DELETE CASCADE, 
  storageSiteRef BIGINT UNSIGNED, FOREIGN KEY(storageSiteRef) REFERENCES storage_site_table(storageSiteId) ON DELETE CASCADE
) ENGINE=InnoDB;

DELIMITER |

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
    UPDATE pdrigroup_table SET needCheck=TRUE WHERE pdriGroupId IN (SELECT pdriGroupRef FROM ldata_table WHERE uid = new.ld_uid);
END|

DROP TRIGGER IF EXISTS on_pref_delete |
CREATE TRIGGER on_pref_delete
AFTER DELETE ON pref_table
FOR EACH ROW BEGIN
  UPDATE pdrigroup_table SET needCheck=TRUE WHERE pdriGroupId IN (SELECT pdriGroupRef FROM ldata_table WHERE uid=old.ld_uid);
END|


DELIMITER ;





-- ------------------------------- create pref table for SQL state HY000: Can't create table (errno: 150)------------------

CREATE TABLE ldata_table_copy (
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
INSERT INTO ldata_table_copy SELECT * FROM ldata_table;


ALTER TABLE pdrigroup_table ADD needCheck BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE pdrigroup_table ADD bound BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pdrigroup_table ADD INDEX(needCheck,bound);
ALTER TABLE storage_site_table ADD private BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE storage_site_table ADD removing BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE storage_site_table ADD INDEX (private,removing);

CREATE TABLE credential_table_copy (
  credintialId SERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255)
) ENGINE=InnoDB;
INSERT INTO credential_table_copy SELECT * FROM credential_table;


CREATE TABLE storage_site_table_copy (
  storageSiteId SERIAL PRIMARY KEY,
  resourceUri VARCHAR(1024),
  credentialRef BIGINT UNSIGNED, FOREIGN KEY(credentialRef) REFERENCES credential_table_copy(credintialId) ON DELETE CASCADE,
  currentNum BIGINT,
  currentSize BIGINT,
  quotaNum BIGINT,
  quotaSize BIGINT,
  isCache BOOLEAN NOT NULL DEFAULT FALSE, INDEX(isCache),
  extra VARCHAR(512),
  encrypt BOOLEAN NOT NULL DEFAULT FALSE, INDEX(encrypt),
  private BOOLEAN NOT NULL DEFAULT FALSE, INDEX (private),
  removing BOOLEAN NOT NULL DEFAULT FALSE, INDEX(removing)
) ENGINE=InnoDB;
INSERT INTO storage_site_table_copy SELECT * FROM storage_site_table;



DROP TABLE storage_site_table;
DROP TABLE credential_table;
DROP TABLE ldata_table;


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
  removing BOOLEAN NOT NULL DEFAULT FALSE, INDEX(removing)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS  pref_table (
  id SERIAL PRIMARY KEY, 
  ld_uid BIGINT UNSIGNED, FOREIGN KEY(ld_uid) REFERENCES ldata_table(uid) ON DELETE CASCADE, 
  storageSiteRef BIGINT UNSIGNED, FOREIGN KEY(storageSiteRef) REFERENCES storage_site_table(storageSiteId) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO ldata_table SELECT * FROM ldata_table_copy;
INSERT INTO credential_table SELECT * FROM credential_table_copy;
INSERT INTO storage_site_table SELECT * FROM storage_site_table_copy;

DELIMITER |

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
    UPDATE pdrigroup_table SET needCheck=TRUE WHERE pdriGroupId IN (SELECT pdriGroupRef FROM ldata_table WHERE uid = new.ld_uid);
END|

DROP TRIGGER IF EXISTS on_pref_delete |
CREATE TRIGGER on_pref_delete
AFTER DELETE ON pref_table
FOR EACH ROW BEGIN
  UPDATE pdrigroup_table SET needCheck=TRUE WHERE pdriGroupId IN (SELECT pdriGroupRef FROM ldata_table WHERE uid=old.ld_uid);
END|

DELIMITER ;


-- DROP TABLE ldata_table_copy;
-- DROP TABLE credential_table_copy;
-- DROP TABLE storage_site_table_copy;

-- -----------------------------------------------------------------------------