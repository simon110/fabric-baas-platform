#!/bin/bash
# shellcheck disable=SC2155

export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export ORDERER_ADDR=$3
export ORDERER_CERT=$4
export CONFIGTX_PATH=$5
export CHANNEL_NAME=$6
export CHANNEL_GENESIS_BLOCK_PATH=$7
export CHANNEL_CREATE_TX_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).tx

configtxgen \
  -profile "$CHANNEL_NAME" \
  -outputCreateChannelTx "$CHANNEL_CREATE_TX_PATH" \
  -channelID "$CHANNEL_NAME" \
  -configPath "$CONFIGTX_PATH" >&/dev/stdout

if [ -e "$CHANNEL_CREATE_TX_PATH" ]; then
  peer channel create \
    -o "$ORDERER_ADDR" \
    -c "$CHANNEL_NAME" \
    -f "$CHANNEL_CREATE_TX_PATH" \
    --outputBlock "$CHANNEL_GENESIS_BLOCK_PATH" \
    --tls --cafile "$ORDERER_CERT" >&/dev/stdout
fi
