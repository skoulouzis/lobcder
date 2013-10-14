#!/bin/bash

#Example: ./multiThreadWget.sh --url=http://localhost:8080/lobcder/dav/fileName --user=user --password=secret --threads=3


for i in "$@"
do
case $i in
    --user=*)
    username=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`

    ;;
    --password=*)
    password=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;
    --threads=*)
    threads=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;

    --url=*)
    url=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;
    *)
            # unknown option
    ;;
esac
done

filename=`basename $url`


spider=$((wget --spider --user=$username --password=$password $url) 2>&1)
len=`echo $spider | awk '{print $41}'`


part_size=$(($len / $threads))
offest=0


# echo "Threads: $threads partSize: $part_size len: $len"

for ((i = 0 ; i < $threads ; i++));
do 
	start=$offest
	end=$(($offest + $part_size))
	offest=$(($end + 1))
	echo $i
	if [ $i -ge $(($threads - 1)) ]
	then
	  end=$len
	fi
# 	echo "part: $start - $end"
	echo ------------ -O $filename.$i --header=\"Range: bytes=$start-$end\" --user=$username --password=$password  $url
   	wget -O $filename.$i --header=\"Range: bytes=$start-$end\" --user=$username --password=$password  $url & 
done

wait


for ((i = 0 ; i < $threads ; i++));
do
  cat $filename.$i >> $filename
   rm $filename.$i
done 