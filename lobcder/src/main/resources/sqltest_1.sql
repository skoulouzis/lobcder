DELIMITER |
DROP TRIGGER IF EXISTS on_ldata_insert |
CREATE TRIGGER on_ldata_insert 
AFTER INSERT ON ldata_table
FOR EACH ROW BEGIN
  IF NEW.datatype = 'logical.file' THEN
    IF NEW.pdriGroupId != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount+1 WHERE groupId = NEW.pdriGroupId;
        set @id := 5;
        INSERT INTO permission_table (perm_type, ld_uid_ref, role_name)
        SELECT perm_type, NEW.uid, role_name FROM permission_table WHERE ld_uid_ref = @id;
    END IF;
  END IF;
END|

DELIMITER ;