#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export ORDERER_ADDR=$3
export ORDERER_CERT=$4
export CHANNEL_NAME=$5
export CHANNEL_BLOCK_PATH=$6

peer channel fetch 0 "$CHANNEL_BLOCK_PATH" \
  -o "$ORDERER_ADDR" \
  -c "$CHANNEL_NAME" \
  --cafile "$ORDERER_CERT" --tls >&/dev/stdout
