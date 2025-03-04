#!/bin/sh

# vegeta -cpus 2  attack -duration 60s  -name "Spring Boot"  -output ./results.bin  -rate 50 -targets ./targets.txt  -workers 2

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

RESULTS=()

DURATION=10s
RATE=50
CPUS=1
WORKERS=1
TIMEOUT=5s


echo "CPUs\tRATE\tWORKERS\tTIMEOUT\tDURATION"
echo "----\t----\t-------\t-------\t--------"
echo "$CPUS\t$RATE\t$WORKERS\t$TIMEOUT\t$DURATION"

for endpoint in "${ENDPOINTS[@]}"
do
    result="results_$(sed 's/\//_/g' <<< "$endpoint").bin"
    RESULTS+=("$result")
    echo "Testing '$endpoint' ..."
    echo "GET http://localhost:8080/$endpoint" | vegeta -cpus $CPUS attack \
                                                        -duration $DURATION \
                                                        -name "$endpoint" \
                                                        -output "./$result" \
                                                        -timeout $TIMEOUT \
                                                        -rate $RATE \
                                                        -workers $WORKERS
done

vegeta plot "${RESULTS[@]}" > plot.html