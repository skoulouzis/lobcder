#!/bin/bash 

NUM_OF_LINES=$1

awk "NR % $NUM_OF_LINES == 1"  $2 > $2_del_every$1.csv 
