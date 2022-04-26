#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export ORDERER_ADDR=$3
export ORDERER_CERT=$4
export CHANNEL_NAME=$5
export ENVELOPE_PROTOBUF_PATH=$6

peer channel update \
  -f "$ENVELOPE_PROTOBUF_PATH" \
  -c "$CHANNEL_NAME" \
  -o "$ORDERER_ADDR" \
  --tls true \
  --cafile "$ORDERER_CERT" >&/dev/stdout
