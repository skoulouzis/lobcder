#!/bin/bash
echo "-----------------------------" >> /home/skoulouz/workspace/lobcder-tests/measures/ipref-elabTovphlob
date  >> /home/skoulouz/workspace/lobcder-tests/measures/ipref-elabTovphlob
#for ((i = 32 ; i < 500 ; i=$i*2)); do
    echo THREADS 70 >> /home/skoulouz/workspace/lobcder-tests/measures/ipref-elabTovphlob
    iperf -c 149.156.10.138 -p 9876 -P 70 >> /home/skoulouz/workspace/lobcder-tests/measures/ipref-elabTovphlob
#done

echo "-----------------------------" >> /home/skoulouz/workspace/lobcder-tests/measures/ipref-elabTovphlob
