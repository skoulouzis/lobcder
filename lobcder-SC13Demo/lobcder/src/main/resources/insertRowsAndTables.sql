 
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
