#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export CORE_PEER_ADDRESS=$3
export CORE_PEER_TLS_ROOTCERT_FILE=$4
export ORDERER_ADDR=$5
export ORDERER_CERT=$6
export CHANNEL_NAME=$7

peer channel getinfo \
  -c "$CHANNEL_NAME" \
  -o "$ORDERER_ADDR" \
  --cafile "$ORDERER_CERT" --tls >&/dev/stdout
