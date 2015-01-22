#!/bin/bash

user=user
pass=pass
file=file
dir=dir

host=localhost:8080
url=http://$user:$pass@$host/lobcder/dav/dir
localdir=/tmp


STATE_OK=0
STATE_WARNING=1
STATE_CRITICAL=2
STATE_UNKNOWN=3


DEBUG=false


EXIT_STATE=$STATE_OK
# ---------------------------PUT--------------------------
if [ "$DEBUG" ]
then
    echo "Test PUT"
fi

curl -ks -X DELETE $url/$file > /dev/null
echo "Safe to delete" > $localdir/$file 
success=`curl -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file | grep "HTTP/1.[01] 20.." 2>&1`
if [ -z "$success" ]
then
    EXIT_STATE=$STATE_CRITICAL
    echo "CRITICAL: PUT request failed"
    exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
    if [ "$DEBUG" ]
    then
        echo "PUT Passed!"
    fi

    EXIT_STATE=$STATE_OK
fi
  # --------------------------------------------------------
  
  
  # ---------------------------GET--------------------------
  
if [ "$DEBUG" ]
then
    echo "Test GET"
fi
  
curl -ks -X DELETE $url/$file > /dev/null
echo "Safe to delete" > $localdir/$file 
curl -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file > /dev/null

success=`curl -iks  $url | grep "HTTP/1.[01] 20.." 2>&1`

if [ -z "$success" ]
then
  EXIT_STATE=$STATE_CRITICAL
  echo "CRITICAL: GET request failed"
  exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
  
    if [ "$DEBUG" ]
    then
        echo "GET Passed!"
    fi

    EXIT_STATE=$STATE_OK
fi
  # --------------------------------------------------------
  
  
  # ---------------------------DELETE--------------------------
  
if [ "$DEBUG" ]
then
  echo "Test DELETE"
fi
  
curl -ks -X DELETE $url/$file > /dev/null
echo "Safe to delete" > $localdir/$file > /dev/null
curl -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file > /dev/null

success=`curl -iks -X DELETE $url/$file | grep "HTTP/1.[01] 20.." 2>&1`

if [ -z "$success" ]
then
    EXIT_STATE=$STATE_CRITICAL
    echo "CRITICAL: DELETE request failed"
    exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then

    if [ "$DEBUG" ]
    then
        echo "DELETE Passed!"
    fi
    EXIT_STATE=$STATE_OK
fi
  # --------------------------------------------------------
  
  
  # ---------------------------PROPFIND--------------------------
  
if [ "$DEBUG" ]
then
  echo "Test PROPFIND"
fi
  
success=`curl -iks -X PROPFIND $url | grep "HTTP/1.[01] 20.." 2>&1`
  
if [ -z "$success" ]
then
    EXIT_STATE=$STATE_CRITICAL
    echo "CRITICAL: PROPFIND request failed"
    exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then

    if [ "$DEBUG" ]
    then
        echo "PROPFIND Passed!"
    fi

EXIT_STATE=$STATE_OK
fi
  # ------------------------------------------------------------
  
  
  # ---------------------------MKCOL--------------------------
  
  
if [ "$DEBUG" ]
then
    echo "Test MKCOL"
fi
  
curl -ks -X DELETE $url/$dir > /dev/null
success=`curl -iks -X MKCOL $url/$dir | grep "HTTP/1.[01] 20.." 2>&1`

if [ -z "$success" ]
then
    EXIT_STATE=$STATE_WARNING
    echo "CRITICAL: MKCOL request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
    if [ "$DEBUG" ]
    then
        echo "MKCOL Passed!"
    fi

    EXIT_STATE=$STATE_OK
fi
  # --------------------------------------------------------
  
  
  # ---------------------------MOVE/RENAME--------------------------
  
if [ "$DEBUG" ]
then
    echo "Test MOVE"
fi
  
curl -ks -X DELETE $url/$file > /dev/null
curl -ks -X DELETE $url/$file.move > /dev/null
echo "Safe to delete" > $localdir/$file 
curl -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file > /dev/null
success=`curl -iks -X MOVE --header "Destination: $url/$file.move" $url/$file | grep "HTTP/1.[01] 20.." 2>&1`
  
if [ -z "$success" ]
then
    EXIT_STATE=$STATE_WARNING
    echo "WARNING: MOVE file request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then

    if [ "$DEBUG" ]
    then
        echo "MOVE Passed!"
    fi

    EXIT_STATE=$STATE_OK
fi
  
curl -ks -X DELETE $url/$dir > /dev/null
curl -ks -X DELETE $url/$dir.move > /dev/null

curl -iks -X MKCOL $url/$dir > /dev/null
success=`curl -iks -X MOVE --header "Destination: $url/$dir.move" $url/$dir | grep "HTTP/1.[01] 20.." 2>&1`

if [ -z "$success" ]
then
    EXIT_STATE=$STATE_WARNING
    echo "WARNING: MOVE folder request failed"
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
    if [ "$DEBUG" ]
    then
        echo "MOVE Passed!"
    fi
  
  EXIT_STATE=$STATE_OK
fi
  #--------------------------------------------------------
  
  # ---------------------------COPY--------------------------
  
if [ "$DEBUG" ]
then
    echo "Test COPY"
fi
  
curl -ks -X DELETE $url/$file > /dev/null
curl -ks -X DELETE $url/$file.copy > /dev/null
echo "Safe to delete" > $localdir/$file 
curl -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file > /dev/null
  
success=`curl -iks -X COPY --header "Destination:$url/$file.copy" $url/$file | grep "HTTP/1.[01] 20.." 2>&1`
  
if [ -z "$success" ]
then
    EXIT_STATE=$STATE_CRITICAL
    echo "CRITICAL: COPY request failed"
    EXIT_STATE=$STATE_WARNING
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
    if [ "$DEBUG" ]
    then
        echo "COPY Passed!"
    fi

    EXIT_STATE=$STATE_OK
fi

# -------------------------------------------------------------
# 
# 
# 
# 
# # ---------------------------HEAD--------------------------
# # Takes too long 
# # success=`curl -iks -X HEAD https://$user:$pass@$url/ | grep "HTTP/1.[01] 20.." 2>&1`
# # if [ -z "$success" ]
# # then
# #   EXIT_STATE=$STATE_WARNING
# #   echo "Warning: HEAD request failed"
# # else
# #   EXIT_STATE=$STATE_OK
# # fi
# # --------------------------------------------------------
  
  
  
  
  
  # ---------------------------Check backend--------------------------
  
if [ "$DEBUG" ]
then
    echo "Test Backend"
fi
  
curl  -ks -X DELETE $url/$file > /dev/null
echo "Safe to delete" > $localdir/$file 
success=`curl  -iks -H "Content-Type:plain/text" -T $localdir/$file $url/$file | grep "HTTP/1.[01] 20.." 2>&1`
  
if [ -z "$success" ]
then
    EXIT_STATE=$STATE_CRITICAL
    echo "CRITICAL: PUT request failed"
    exit $EXIT_STATE
elif [ "$EXIT_STATE" -le "$STATE_OK" ]
then
    
    if [ "$DEBUG" ]
    then
        echo "PUT Passed!"
    fi

    availBackends=`curl -iks --request PROPFIND  --header "Content-Type: text/xml" --header "Brief:t" --data "<D:propfind xmlns:D='custom:'><D:prop><D:avail-storage-sites/></D:prop></D:propfind>"  $url/$file | grep -oPm1 "(?<=<ns1:avail-storage-sites>)[^<]+" 2>&1`
    availBackendsArray=${availBackends#"["}
    availBackendsArray=${availBackendsArray%"]"}
    IFS=', ' read -a arrayBackend <<< "$availBackendsArray"
    availBackendsArray=$array


    sleep 30

    while true
    do
        dataDistribution=`curl -iks --request PROPFIND  --header "Content-Type: text/xml" --header "Brief:t" --data "<D:propfind xmlns:D='custom:'><D:prop><D:data-distribution/></D:prop></D:propfind>"  $url/$file | grep -oPm1 "(?<=<ns1:data-distribution>)[^<]+" 2>&1`
        dataDistributionArray=${dataDistribution#"["}
        dataDistributionArray=${dataDistributionArray%"]"}
        IFS=', ' read -a arrayDistribution <<< "$dataDistributionArray"
        
        for backend in "${arrayBackend[@]}"
        do
            if [ "$DEBUG" ]
            then
                echo "backend: $backend"
            fi
            
            for dist in "${arrayDistribution[@]}"
            do
                if [ "$DEBUG" ]
                then
                    echo "dist: $dist"
                    echo "------------------"
                fi
                
                if [[ $dist == *$backend* ]]
                then
                    done=true
                    break;
                else
                    let counter+=1
                fi
            done
        done
        
        if [ "$done" = true ] ; then
            EXIT_STATE=$STATE_OK
            break;
        fi
        
        if [ "$counter" -gt 120 ]
        then
            echo "WARNING: Backend seems to be off-line"
            EXIT_STATE=$STATE_WARNING
            break;
        fi
        
        sleep 20
    done
fi
  


#-----------------------------------------------------------------------------
  
  case "$EXIT_STATE" in
  
  0)  echo "OK: All tests passed"
  ;;
  1)  echo  "WARNING: Some tests failed."
  ;;
  2)  echo  "CRITICAL: Important tests failed."  
  ;;
  3) echo  "UNKNOWN: No idea what happened." 
  ;;
  *) echo "UNKNOWN:  No idea what happened."
  ;;
  esac
  
  exit $EXIT_STATE
  