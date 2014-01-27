#!/bin/sh
gnuplot << EOF
set datafile separator ";"
set terminal png
set output "$1.png"
set key autotitle columnhead

set xdata time
set timefmt "%d/%m/%Y"  

set xlabel "Date"
set ylabel "MBit/sec"

set style line 1 linetype 1 linewidth 2

plot "$1" using 1:3:4 with yerrorbars  linestyle 1, "$1" using 1:5:6 with yerrorbars  linestyle 2, "$1" using 1:7:8 with yerrorbars linestyle 3;
