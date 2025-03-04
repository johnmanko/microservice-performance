#!/bin/sh

K6_CMD=$HOME/k6

ENDPOINTS=(
  "non-async/clean"
  "non-async/cf-get"
  "async/cf-clean"
  "async/cf"
  "async/cf-with-async"
  "reactive/clean"
  "reactive/cf-get"
  "reactive/cf"
  "reactive/cf-with-async"
)

RESULTS=() # In case you we want to work with all generated files later

for endpoint in "${ENDPOINTS[@]}"
do
    echo "Testing '$endpoint' ..."

    #Export HTML report
    #result="k6_results_$(sed 's/\//_/g' <<< "$endpoint").html"
    
    #Export JSON report
    #result="k6_results_$(sed 's/\//_/g' <<< "$endpoint").json"
    
    RESULTS+=("$result")
    $K6_CMD run -e ENDPOINT=$endpoint --out 'dashboard=period=1s' k6-tests-template.js

    # or
    #Export HTML command
    #K6_CM run -e ENDPOINT=$endpoint --out "dashboard=export=$result&period=1s" k6-tests-template.js


    # or
    #Export JSON command with sample replay
    #K6_CM run -e ENDPOINT=$endpoint --out json=$result k6-tests-template.js
    # if [test -f "$result"]; then        
    #     k6-web-dashboard aggregate "$result" "$result.gz" --period 1s
    #     k6-web-dashboard replay "$result.gz"
    # fi

done
