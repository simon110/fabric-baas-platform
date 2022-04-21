#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export ORDERER_GENESIS_NAME=$1
export SYSTEM_CHANNEL_NAME=$2
export GENESIS_BLOCK_PATH=$3
export CONFIGTX_PATH=$4

configtxgen \
  -profile "$ORDERER_GENESIS_NAME" \
  -channelID "$SYSTEM_CHANNEL_NAME" \
  -outputBlock "$GENESIS_BLOCK_PATH" \
  -configPath "$CONFIGTX_PATH" >&/dev/stdout
