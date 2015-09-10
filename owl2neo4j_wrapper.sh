#!/bin/bash

exec java -jar -DentityExpansionLimit=1000000 "$0" "$@"
exit
