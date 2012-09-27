 create temporary table myroles (
 mrole VARCHAR(255)
);

insert into myroles(mrole) values  ('other');

select * from ldata_table
where exists(
select 1 from permission_table join myroles on permission_table.role_name = myroles.mrole 
      where permission_table.perm_type = 'write' 
       and permission_table.ld_uid_ref = ldata_table.uid
);
drop table myroles;
