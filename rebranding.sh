#!/bin/sh

#####################################
# Rebrand Signal to Cable.          #
# This script has to be idempotent. #
#####################################

# Rebrand strings.
# When merging from upstream sometimes it's easier
# to checkout their translations and re-run this script.

sed -i s/Signal/Cable/ res/layout/reminder_header.xml

find res -iname '*.xml' -exec sed -i ':a;/\(.*<string name="[a-zA-Z0-9_]*">.*\)Signal/s//\1Cable/;ta' {} +

