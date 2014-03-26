#!/bin/bash

user=user
pass=pass
file=file
dir=dir

# host=localhost:8080
host=lobcder.vph.cyfronet.pl
# url=http://$user:$pass@$host/lobcder/dav/dir
url=https://$user:$pass@$host/lobcder/dav/tester



STATE_OK=0
STATE_WARNING=1
STATE_CRITICAL=2
STATE_UNKNOWN=3


EXIT_STATE=$STATE_OK
# ---------------------------PUT--------------------------
echo "Test PUT"
curl -k -X DELETE $url/$file 
echo "Safe to delete" > $file 
success=`curl -i -k -H "Content-Type:plain/text" -T $file $url/$file | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: PUT request failed"
  exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "PUT Passed!"
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------GET--------------------------
echo "Test GET"
curl -k -X DELETE $url/$file
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file $url/$file

success=`curl -ki  $url | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: GET request failed"
  exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "GET Passed!"
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------DELETE--------------------------
echo "Test DELETE"
curl -k -X DELETE $url/$file
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file $url/$file

success=`curl -ki -X DELETE $url/$file | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: DELETE request failed"
  exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "DELETE Passed!"
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------PROPFIND--------------------------
echo "Test PROPFIND"
success=`curl -ik -X PROPFIND $url | grep "HTTP/1.[01] 20.."`

if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: PROPFIND request failed"
  exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "PROPFIND Passed!"
  EXIT_STATE=$STATE_OK
fi
# ------------------------------------------------------------


# ---------------------------MKCOL--------------------------
echo "Test MKCOL"
curl -k -X DELETE $url/$dir
success=`curl -ki -X MKCOL $url/$dir | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_WARNING
  echo "Critical: MKCOL request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "MKCOL Passed!"
  EXIT_STATE=$STATE_OK
fi
# --------------------------------------------------------


# ---------------------------MOVE/RENAME--------------------------
echo "Test MOVE"
curl -k -X DELETE $url/$file
curl -k -X DELETE $url/$file.move
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file $url/$file
success=`curl -ki -X MOVE --header "Destination: $url/$file.move" $url/$file | grep "HTTP/1.[01] 20.."`

if [ -z "$success" ]
then
  EXIT_STATE=$STATE_WARNING
  echo "Warning: MOVE file request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "MOVE Passed!"
  EXIT_STATE=$STATE_OK
fi

curl -k -X DELETE $url/$dir
curl -k -X DELETE $url/$dir.move

curl -ki -X MKCOL $url/$dir
success=`curl -ki -X MOVE --header "Destination: $url/$dir.move" $url/$dir | grep "HTTP/1.[01] 20.."`
if [ -z "$success" ]
then
  EXIT_STATE=$STATE_WARNING
  echo "Warning: MOVE folder request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "MOVE Passed!"
  EXIT_STATE=$STATE_OK
fi
--------------------------------------------------------




# ---------------------------COPY--------------------------
echo "Test COPY"
curl -k -X DELETE $url/$file
curl -k -X DELETE $url/$file.copy
echo "Safe to delete" > $file 
curl -i -k -H "Content-Type:plain/text" -T $file $url/$file

success=`curl -ik -X COPY --header "Destination:$url/$file.copy" $url/$file | grep "HTTP/1.[01] 20.."`

if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "Critical: COPY request failed"
  EXIT_STATE=$STATE_WARNING
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
#   echo "COPY Passed!"
  EXIT_STATE=$STATE_OK
fi
# -------------------------------------------------------------
# 
# 
# 
# 
# # ---------------------------HEAD--------------------------
# # Takes too long 
# # success=`curl -ki -X HEAD https://$user:$pass@$url/ | grep "HTTP/1.[01] 20.."`
# # if [ -z "$success" ]
# # then
# #   EXIT_STATE=$STATE_WARNING
# #   echo "Warning: HEAD request failed"
# # else
# #   EXIT_STATE=$STATE_OK
# # fi
# # --------------------------------------------------------



case "$EXIT_STATE" in

0)  echo "Passed!"
    ;;
1)  echo  "Warning! Some tests failed"
    ;;
2)  echo  "Critical! Important tests failed"
    ;;
3) echo  "Unknown! No idea what happend"
   ;;
*) echo "Unknown! No idea what happend"
   ;;
esac

exit $EXIT_STATE
