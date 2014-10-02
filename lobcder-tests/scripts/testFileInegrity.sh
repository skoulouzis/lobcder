#!/bin/bash


if mount | grep https://lobcder.vph.cyfronet.pl/lobcder/dav > /dev/null; then



    for (( i=400; i<=510; i++ ))
    do
        echo "$i of 510"
        dd if=/dev/urandom of=/tmp/testLargefile bs=1M count=$i
        md5=`md5sum /tmp/testLargefile | awk '{ print $1 }'`
        echo "md5: $md5"
        mv /tmp/testLargefile /tmp/$md5
        cp /tmp/$md5 /media/$USER/lobcder/skoulouz
        echo "sleep 600 "
        sleep 800 
        mv /media/$USER/lobcder/skoulouz/$md5 /media/$USER/lobcder/skoulouz/$md5.copy
        cp /media/$USER/lobcder/skoulouz/$md5.copy /tmp/
        md5Remote=`md5sum /tmp/$md5.copy | awk '{ print $1 }'`
        echo "md5Remote: $md5Remote"
        if [ $md5 != $md5Remote ];
        then
            echo "File  /tmp/$md5.copy is corrupted !"
            exit 1
        else
            rm /tmp/$md5.copy
        fi
    done



else
    echo "lobcder not mounted! "
fi

