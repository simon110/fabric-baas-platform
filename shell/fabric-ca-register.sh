#!/bin/bash

export FABRIC_CA_CLIENT_HOME=$1
export CA_SERVER_CERT=$2
export CA_SERVER_NAME=$3
export ID_USERNAME=$4
export ID_PASSWORD=$5
export ID_USERTYPE=$6

fabric-ca-client register \
  --caname "$CA_SERVER_NAME" \
  --id.name "$ID_USERNAME" \
  --id.secret "$ID_PASSWORD" \
  --id.type "$ID_USERTYPE" \
  --tls.certfiles "$CA_SERVER_CERT" >&/dev/stdout
