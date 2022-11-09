#!/bin/bash
java -Dserver.port=8083 -jar termite.jar -Dlog4j.configurationFile=app/log4j2.xml &
for i in {0..101}
do
  if [ "$i" -eq 101 ]
  then
    echo "Failed to connect to Termite"
    exit 1
  fi
  echo "Waiting for Termite ($i/100)"
  result=$(curl --fail http://terminology-service:8083/fhir/metadata || exit 1)
  exit_code=$?
  echo "$result"
  if [ "$exit_code" -eq 0 ]
  then
     break
  fi
  sleep 5
done
echo "Connected to Termite"
for json_file in app/terminology_data/*.json
do
  curl -vX POST -d @$json_file -H "Content-Type: application/json" http://terminology-service:8083/fhir/ValueSet
done