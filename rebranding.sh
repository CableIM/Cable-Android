#!/bin/sh

#####################################
# Rebrand Signal to Cable.          #
# This script has to be idempotent. #
#####################################

# Rebrand strings.
# When merging from upstream sometimes it's easier
# to checkout their translations and re-run this script.

find res -iname '*.xml' -exec sed -i '
s/Signal /Cable /g;
s/Signal,/Cable,/g;
s/Signal)/Cable)/g;
s/Signal!/Cable!/g;
s/Signal</Cable</g;
s/Signal\./Cable\./g;
s/Signal\\/Cable\\/g;
s/Signal:/Cable:/g;
s/Signal-/Cable-/g;
s/Signal$/Cable/g' {} +

