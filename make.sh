#!/bin/bash

s_nr="sXXXXX"
bin_dir="bin"
current_dir=${PWD##*/}

if [ "$current_dir" != "$s_nr" ] && [ "$current_dir" != "htw-rn-udp-filetransfer" ]
then
  bin_dir="$s_nr/bin"
fi

# -d option specifies output dir: bin
javac -d $bin_dir ./src/main/*.java ./src/sw/*.java ./src/sw/packets/*.java