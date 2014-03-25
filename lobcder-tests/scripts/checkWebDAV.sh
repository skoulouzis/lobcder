#!/bin/bash


url=https://host
user=tester
pass=t3stLob
file=file
dir=dir

STATE_OK=0
STATE_WARNING=1
STATE_CRITICAL=2
STATE_UNKNOWN=3



# ---------------------------PUT--------------------------
curl -k -X DELETE https://$user:$pass@$url/$file
echo "Safe to delete" > $file 
success=`curl -i -k -H "Content-Type:plain/text" -T $file https://$user:$pass@$url/$file | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: PUT request failed"
  exit $EXIT_STATE
else
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------GET--------------------------
curl -k -X DELETE https://$user:$pass@$url/$file
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file https://$user:$pass@$url/$file

success=`curl -ki  https://$user:$pass@$url/$file | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: GET request failed"
  exit $EXIT_STATE
else
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------DELETE--------------------------
curl -k -X DELETE https://$user:$pass@$url/$file
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file https://$user:$pass@$url/$file

success=`curl -ki -X DELETE https://$user:$pass@$url/$file | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: DELETE request failed"
  exit $EXIT_STATE
else
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------MKCOL--------------------------
curl -k -X DELETE https://$user:$pass@$url/$dir
success=`curl -ki -X MKCOL https://$user:$pass@$url/$dir | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_WARNING
  echo "Critical: MKCOL request failed"
else
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------HEAD--------------------------
success=`curl -ki -X HEAD https://$user:$pass@$url/ | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_WARNING
  echo "Warning: HEAD request failed"
else
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


exit $EXIT_STATE
