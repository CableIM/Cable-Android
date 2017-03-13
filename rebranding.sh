#!/bin/sh

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

