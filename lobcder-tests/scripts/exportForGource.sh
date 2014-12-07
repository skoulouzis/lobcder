sqlUser=user
sqlPass=password
dbName=DB
CATALINA_HOME=$HOME/apache-tomcat-6.0.36

#mysql -u$sqlUser -p$sqlPass -s -N -e  "UPDATE $dbName.requests_table SET userAgent = 'unknown' WHERE userAgent = '' OR userAgent is NULL"


mysql -u$sqlUser -p$sqlPass -s -N -e  "SELECT UNIX_TIMESTAMP(timeStamp), userName, 


CASE methodName
when 'PUT' then 'A'
when 'POST' then 'A'
when 'MKCOL' then 'A'
when 'LOCK' then 'M'
when 'UNLOCK' then 'M'
when 'PROPPATCH' then 'M'
when 'PROPFIND' then 'M'
when 'OPTIONS' then 'M'
when 'GET' then 'M'
when 'HEAD' then 'M'
when 'TRACE' then 'M'
when 'CONNECT' then 'M'
when 'COPY' then 'M'
when 'MOVE' then 'M'
when 'DELETE' then 'D'
END AS AMD,

requestURL
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
sed -i 's/http\:\/\/elab.lab.uvalight.net\/lobcder\///g' /tmp/result.csv


sed -i 's/ZGVtbzE6ZDNtb1Bhc3N3ZA==/demo1/g' /tmp/result.csv


sed -i "s/Jakarta Commons-HttpClient*/Jakarta/g" /tmp/result.csv
sed -i "s/Jakarta\ Commons-HttpClient\/[0-9]./Jakarta/g" /tmp/result.csv


sed -i "s/cadaver*/cadaver/g" /tmp/result.csv
sed -i "s|cadaver\/[0-9].[0-9][0-9].[0-9]\ neon\/[0-9].[0-9][0-9].[0-9]|cadaver|g" /tmp/result.csv

sed -i "s/python*/python/g" /tmp/result.csv
sed -i "s|python-requests\/[0-9].[0-9].[0-9]\ Cpython\/[0-9].[0-9].[0-9]\ Linux\/[0-9].[0-9].[0-9][0-9]-[0-9][0-9][0-9]-[a-z][a-z][0-9]|python|g" /tmp/result.csv
sed -i "s|python-requests\/[0-9].[0-9].[0-9]\ Cpython\/[0-9].[0-9].[0-9]\ Linux\/[0-9].[0-9][0-9].[0-9]-xxxx-std-ipv6-64|python|g"  /tmp/result.csv
sed -i "s|python-requests\/[0-9].[0-9].[0-9]\ Cpython\/[0-9].[0-9].[0-9]\ Linux\/[0-9].[0-9][0-9].[0-9]-xxxx-grs-ipv6-64|python|g"  /tmp/result.csv
sed -i "s|python-requests\/[0-9].[0-9].[0-9]\ Cpython\/[0-9].[0-9].[0-9] Windows\/[0-9]|python|g" /tmp/result.csv

sed -i "s/Python*/python/g" /tmp/result.csv
sed -i "s/davfs2*/davfs2/g" /tmp/result.csv
sed -i "s|davfs2\/[0-9].[0-9].[0-9]\ neon\/[0-9].[0-9].|davfs2|g"  /tmp/result.csv
sed -i "s/Mozilla*/Mozilla/g" /tmp/result.csv

sed -i "s#Mozilla\/[0-9].[0-9]\ (Windows\ NT\ [0-9].[0-9])\ AppleWebKit\/[0-9][0-9][0-9].[0-9][0-9]\ (KHTML,\ like\ Gecko)\ Chrome\/[0-9][0-9].[0-9].[0-9][0-9][0-9].[0-9][0-9][0-9]\ Safari/[0-9]37.36#Safari#g" /tmp/result.csv


sed -i "s/curl*/curl/g" /tmp/result.csv
sed -i "s/gvfs*/gvfs/g" /tmp/result.csv
sed -i "s/BitKinez*/BitKinez/g" /tmp/result.csv
sed -i "s/Java*/Java/g" /tmp/result.csv
sed -i "s/Opera*/Opera/g" /tmp/result.csv
sed -i "s/WebDAVLib*/Opera/g" /tmp/result.csv
sed -i "s/WebDAVFS*/Opera/g" /tmp/result.csv
sed -i "s/Cyberduck*/Cyberduck/g" /tmp/result.csv
sed -i "s/Apache-HttpClient*/Apache-HttpClient/g" /tmp/result.csv
sed -i "s/Transmit*/Transmit/g" /tmp/result.csv
sed -i "s/IPv4Scan*/IPv4Scan/g" /tmp/result.csv
sed -i "s/Benchmark*/Benchmark/g" /tmp/result.csv


sed -i '/1395853038\tjuanarenas\tD\tdav\//d' /tmp/result.csv
sed -i 's/\t/|/g' /tmp/result.csv



cp /tmp/result.csv $CATALINA_HOME/logs/gourceResult.csv


 #gource --log-format custom ~/Documents/logs/lobcder.production/gourceResult.csv --stop-at-end  -key -auto-skip-seconds 0.5 --file-idle-time 0.1 --max-file-lag 0.1 --title "LOBCDER Users Activity Visualisation" --time-scale 4 --seconds-per-day 0.25 --bloom-intensity 0.2 --user-scale 6 --multi-sampling -1024x768 --max-files 0 --hide filenames,usernames --dir-name-depth 1 --highlight-dirs  --output-ppm-stream - | avconv -an -threads 4 -y -f image2pipe -vcodec ppm -i - -b 65536K -r 30.000 -vb 8000000  Videos/lobcder-user-activity0.mp4