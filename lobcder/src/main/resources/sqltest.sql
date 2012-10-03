drop table IF EXISTS myroles;
create temporary table myroles (
 mrole VARCHAR(255)
);

insert into myroles(mrole) values  ('other');
select * from myroles;
insert into ldata_table(ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId)
select 'new owner', 'logical.file', ld_name, '/c', NOW(), NOW(), ld_length, contentTypesStr, pdriGroupId from ldata_table
where parent='/a/b' AND datatype='logical.file' AND (ldata_table.ownerId = 'token0' OR exists(
select 1 from permission_table join myroles on permission_table.role_name = myroles.mrole 
      where permission_table.perm_type = 'read' 
       and permission_table.ld_uid_ref = ldata_table.uid
));

select * from ldata_table where parent='/c' and datatype='logical.file';

drop table myroles;

