CREATE TABLE tx_stats (
 uid SERIAL PRIMARY KEY,
 logdate TIMESTAMP,
 tx_source VARCHAR(1024),
 tx_destination  VARCHAR(1024),
 tx_speed  DOUBLE,
 tx_size DOUBLE,
 tx_destination_country_code2 VARCHAR(1024),
 tx_destination_country_name VARCHAR(1024)
) ENGINE=InnoDB;



-- LOAD DATA LOCAL INFILE 'tx.csv' 
-- INTO TABLE tx_stats FIELDS TERMINATED BY ',' 
-- ENCLOSED BY '"'
-- LINES TERMINATED BY '\n' 
-- (@var1,tx_source,tx_destination,tx_speed,tx_size,tx_destination_country_code2,tx_destination_country_name)
-- set logdate = STR_TO_DATE(@var1, '%M %d,%Y %h:%i:%s %p')



CREATE TABLE rx_stats (
 uid SERIAL PRIMARY KEY,
 logdate TIMESTAMP,
 rx_source VARCHAR(1024),
 rx_destination  VARCHAR(1024),
 rx_speed  DOUBLE,
 rx_size DOUBLE,
 rx_destination_country_code2 VARCHAR(1024),
 rx_destination_country_name VARCHAR(1024)
) ENGINE=InnoDB;



CREATE TABLE repl_stats (
 uid SERIAL PRIMARY KEY,
 logdate TIMESTAMP,
 repl_source VARCHAR(1024),
 repl_destination  VARCHAR(1024),
 repl_speed  DOUBLE,
 rx_size DOUBLE,
 repl_size VARCHAR(1024)
) ENGINE=InnoDB;


CREATE TABLE apache_startupTime (
 uid SERIAL PRIMARY KEY,
 logdate TIMESTAMP,
 apache_startupTime  DOUBLE,
 rx_size DOUBLE
) ENGINE=InnoDB;

