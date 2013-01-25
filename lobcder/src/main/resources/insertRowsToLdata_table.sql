ALTER TABLE ldata_table ADD lockTokenID  VARCHAR(255);
ALTER TABLE ldata_table ADD lockScope  VARCHAR(255);
ALTER TABLE ldata_table ADD lockType  VARCHAR(255);
ALTER TABLE ldata_table ADD lockedByUser  VARCHAR(255);
ALTER TABLE ldata_table ADD lockDepth  VARCHAR(255);    
ALTER TABLE ldata_table ADD lockTimeout  BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ldata_table ADD  customComment  VARCHAR(255);