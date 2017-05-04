#!/bin/sh

#####################################
# Rebrand Signal to Cable.          #
# This script has to be idempotent. #
#####################################

# Rebrand strings.
# When merging from upstream sometimes it's easier
# to checkout their translations and re-run this script.

sed -i s/Signal/Cable/ res/layout/reminder_header.xml

find res -name 'strings.xml' -exec sed -i '
:a;
/\(^ *<string name="[[:alnum:]_-]*">.*\)Signal/ s//\1Cable/;
ta;
/<string name=/! s/Signal/Cable/g' {} +

