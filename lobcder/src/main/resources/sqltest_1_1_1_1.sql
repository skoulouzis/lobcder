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
