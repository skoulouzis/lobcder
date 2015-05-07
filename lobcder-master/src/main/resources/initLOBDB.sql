create database lobcderDB2;
GRANT ALL PRIVILEGES on lobcderDB2.* to lobcder@localhost IDENTIFIED by 'RoomC3156';
GRANT SELECT ON mysql.proc TO 'lobcder'@'localhost'; 
GRANT SUPER ON *.* to lobcder@localhost IDENTIFIED by 'RoomC3156';