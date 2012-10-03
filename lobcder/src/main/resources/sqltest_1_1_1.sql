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
DROP PROCEDURE IF EXISTS checkEx $$
CREATE PROCEDURE checkEx(IN fullstr VARCHAR(10240))
BEGIN
  DECLARE a INT Default 0;
  DECLARE str VARCHAR(255);
  create temporary table myroles (
      mrole VARCHAR(255)
  ) ENGINE=MEMORY;
  simple_loop: LOOP
         SET a=a+1;
         SET str=SPLIT_STR(fullstr,",",a);
         IF str='' THEN
            LEAVE simple_loop;
         END IF;
         insert into myroles(mrole) values (str);
   END LOOP simple_loop;
   select * from permission_table join myroles on permission_table.role_name = myroles.mrole where permission_table.perm_type = 'read';
   drop table  myroles;
END $$

DELIMITER ;

CALL checkEx('A,B,D');