 
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
)