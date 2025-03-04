#!/bin/sh

ENDPOINTS=(./*.bin)

for endpoint in "${ENDPOINTS[@]}"
do
    echo "======================================="
    echo "$endpoint"
    vegeta report -type="hist[0,100ms,200ms,300ms,500ms,1s,2s,3s,5s]" $endpoint
    vegeta report -type="text" $endpoint
done

