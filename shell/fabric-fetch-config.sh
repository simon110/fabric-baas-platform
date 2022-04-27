#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export ORDERER_ADDR=$3
export ORDERER_CERT=$4
export CHANNEL_NAME=$5
export CONFIG_JSON_PATH=$6
export CONFIG_BLOCK_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).pb

peer channel fetch config "$CONFIG_BLOCK_PATH" \
  -o "$ORDERER_ADDR" \
  -c "$CHANNEL_NAME" \
  --cafile "$ORDERER_CERT" --tls >&/dev/stdout

if [ -e "$CONFIG_BLOCK_PATH" ]; then
  configtxlator proto_decode \
    --input "$CONFIG_BLOCK_PATH" \
    --type common.Block | jq .data.data[0].payload.data.config >"$CONFIG_JSON_PATH"
fi
