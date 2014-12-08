sqlUser=user
sqlPass=password
dbName=DB
CATALINA_HOME=$HOME/apache-tomcat-6.0.36

#mysql -u$sqlUser -p$sqlPass -s -N -e  "UPDATE $dbName.requests_table SET userAgent = 'unknown' WHERE userAgent = '' OR userAgent is NULL"


mysql -u$sqlUser -p$sqlPass -s -N -e  "SELECT UNIX_TIMESTAMP(timeStamp), 
remoteAddr,

CASE methodName
when 'PUT' then '200'
when 'POST' then '201'
when 'MKCOL' then '201'
when 'LOCK' then '200'
when 'UNLOCK' then '204'
when 'PROPPATCH' then '200'
when 'PROPFIND' then '207'
when 'OPTIONS' then '204'
when 'GET' then '200'
when 'HEAD' then '200'
when 'TRACE' then '200'
when 'CONNECT' then '200'
when 'COPY' then '204'
when 'MOVE' then '201'
when 'DELETE' then '204'
END AS AMD, 

requestURL, 
contentLen


-- REPLACE(requestURL, 'http://localhost:8080/lobcder/', '') AS relPath
-- SUBSTRING_INDEX(requestURL,'lobcder/dav',1) AS relPath1,
-- SUBSTRING(requestURL,'dav',2) AS relPath2
-- SUBSTRING_INDEX((SUBSTRING_INDEX((SUBSTRING_INDEX(requestURL, 'http://', -1)), '/', 1)), '.', -2) as domain
   
FROM $dbName.requests_table

   WHERE 
userName NOT LIKE 'tester' and 
requestURL NOT LIKE '%lobcder/dav/' and 
(methodName = 'PUT' or 
  methodName = 'POST' or
  methodName = 'MKCOL' or 
  --  (M)odified
  methodName = 'LOCK' or 
  methodName = 'UNLOCK' or 
  methodName = 'PROPPATCH' or
  methodName = 'MOVE' or
  methodName = 'PROPFIND' or
  methodName = 'COPY' or
  methodName = 'OPTIONS' or
  methodName = 'GET' or
  methodName = 'HEAD' or
  methodName = 'TRACE' or
  methodName = 'CONNECT' or  
  -- (D)eleted. 
  methodName = 'DELETE')
ORDER BY timeStamp;" > /tmp/result.csv

# while IFS=$'\t' read timestamp userName AMD requestURL
# do
#   xpath=${requestURL%/*}
#   path=`echo $xpath | sed "s/^http:\/\///g"`
#   fileName=${requestURL##*/}
#   relativePath=${path:28}
#   echo $relativePath"/"$fileName >> /tmp/paths
# done < /tmp/result.csv
# 

sed -i 's/http\:\/\/149.156.10.138:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/149.156.10.138\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/149.156.10.138:8443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/149.156.10.138:443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/149.156.10.138\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/149.156.10.138:8081\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/lobcder.tk\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/lobcder.tk:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/lobcder.tk:8443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/lobcder.tk:443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/lobcder.vph.cyfronet.pl\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/lobcder.vph.cyfronet.pl\/lobcder\///g' /tmp/result.csv
sed -i 's/http:\/\/lobcder.vph.cyfronet.pl:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/https:\/\/lobcder.vph.cyfronet.pl:99\/lobcder\///g' /tmp/result.csv
sed -i 's/http:\/\/lobcder.vph.cyfronet.pl:90\/lobcder\///g' /tmp/result.csv
sed -i 's/https:\/\/lobcder.vph.cyfronet.pl:8443\/lobcder\///g' /tmp/result.csv
sed -i 's/https:\/\/lobcder.vph.cyfronet.pl:443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/localhost:8443\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/localhost:443\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/localhost:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/127.0.0.1\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/127.0.0.1:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/https\:\/\/127.0.0.1\/lobcder\///g' /tmp/result.csv
sed -i 's/ZGVtbzE6ZDNtb1Bhc3N3ZA==/demo1/g' /tmp/result.csv
sed -i 's/http\:\/\/server.cyfronet.pl:8080\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/server.cyfronet.pl\/lobcder\///g' /tmp/result.csv
sed -i 's/http\:\/\/elab.lab.uvalight.net\/lobcder\///g' /tmp/result.csv


sed -i 's/ZGVtbzE6ZDNtb1Bhc3N3ZA==/demo1/g' /tmp/result.csv

sed -i '/1395853038\tjuanarenas\tD\tdav\//d' /tmp/result.csv
sed -i 's/\t/|/g' /tmp/result.csv



cp /tmp/result.csv $CATALINA_HOME/logs/logstalgia.csv