#!/bin/bash
echo "$(java -Dserver.port=8083 -jar termite.jar -Dlog4j.configurationFile=/app/log4j2.xml)" &
p1=$!
(for i in {0..101}
do
  if [ "$i" -eq 101 ]
  then
    echo "Failed to connect to Termite"
    exit 1
  fi
  echo "Waiting for Termite ($i/100)"
  result=$(curl --fail http://localhost:8083/fhir/metadata || exit 1)
  exit_code=$?
  echo "$result"
  if [ "$exit_code" -eq 0 ]
  then
     break
  fi
  sleep 5
done
echo "Connected to Termite"
for json_file in value_sets/*.json
do
  curl -vX POST -d @$json_file -H "Content-Type: application/json" "http://localhost:${TERMINOLOGY_SERVICE_PORT}/fhir/ValueSet"
  echo "Uploading ${json_file}"
done
for json_file in code_systems/*.json
 do
   curl -vX POST -d @$json_file -H "Content-Type: application/json" "http://localhost:${TERMINOLOGY_SERVICE_PORT}/fhir/CodeSystem"
   echo "Uploading ${json_file}"
done) &
p2=$!
wait $p1 $p2