DROP TABLE IF EXISTS permission_table, ldata_table, pdri_table, pdrigroup_table, storage_site_table, credential_table;

CREATE TABLE pdrigroup_table (
  groupId SERIAL PRIMARY KEY,
  refCount INT, INDEX(refCount)
);

CREATE TABLE credential_table (
  credintialId SERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255)
);

CREATE TABLE storage_site_table (
  storageSiteId SERIAL PRIMARY KEY,
  resourceURI VARCHAR(1024),
  credentialRef BIGINT UNSIGNED, FOREIGN KEY(credentialRef) REFERENCES credential_table(credintialId) ON DELETE CASCADE,
  currentNum BIGINT,
  currentSize BIGINT,
  quotaNum BIGINT,
  quotaSize BIGINT
);

CREATE TABLE pdri_table (
  pdriId SERIAL PRIMARY KEY,
  url VARCHAR(1024),
  storageSiteId BIGINT UNSIGNED,
  pdriGroupId BIGINT UNSIGNED, INDEX(pdriGroupId)
);

CREATE TABLE ldata_table (
 uid SERIAL PRIMARY KEY,
 ownerId VARCHAR(255), INDEX(ownerId),
 datatype ENUM('logical.file', 'logical.folder', 'logical.data'), INDEX(datatype),
 ld_name VARCHAR(255), INDEX(ld_name),
 parent VARCHAR(10240), INDEX(parent),
 createDate DATETIME NOT NULL,
 modifiedDate DATETIME NOT NULL,
 ld_length BIGINT,
 contentTypesStr VARCHAR(10240),
 pdriGroupId BIGINT UNSIGNED
);

CREATE TABLE permission_table (
 id SERIAL PRIMARY KEY,
 perm_type ENUM('read', 'write'), index(perm_type),
 ld_uid_ref BIGINT UNSIGNED, FOREIGN KEY(ld_uid_ref) REFERENCES ldata_table(uid) ON DELETE CASCADE, INDEX(ld_uid_ref),
 role_name VARCHAR(255)
);

DELIMITER |
DROP TRIGGER IF EXISTS on_ldata_delete |
CREATE TRIGGER on_ldata_delete
BEFORE DELETE ON ldata_table 
FOR EACH ROW BEGIN
  IF OLD.datatype = 'logical.file' THEN
    IF OLD.pdriGroupId != 0 THEN
        UPDATE pdrigroup_table SET refCount=refCount-1 WHERE groupId = OLD.pdriGroupId;
    END IF;
  END IF;
END|


DELIMITER ;


INSERT INTO ldata_table(ownerId, datatype, ld_name, parent, createDate, modifiedDate) VALUES('root', 'logical.folder', '', '', NOW(), NOW());
SELECT @rootID := LAST_INSERT_ID() FROM ldata_table;

INSERT INTO permission_table (perm_type, ld_uid_ref, role_name) VALUES  ('read', @rootID, 'other'),
                                                                        ('read', @rootID, 'admin'),
                                                                        ('write', @rootID, 'admin');
INSERT INTO  credential_table(username, password) VALUES ('fakeusername', 'fakepassword');
SELECT @credID := LAST_INSERT_ID() FROM credential_table;
INSERT INTO storage_site_table(resourceURI, credentialRef, currentNum, currentSize, quotaNum, quotaSize)
            VALUES('file://localhost/tmp/', @credID, -1, -1, -1, -1);
