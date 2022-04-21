#!/bin/bash
# shellcheck disable=SC2155

export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export CORE_PEER_ADDRESS=$3
export CORE_PEER_TLS_ROOTCERT_FILE=$4
export CHANNEL_BLOCK_PATH=$5

peer channel join -b "$CHANNEL_BLOCK_PATH" >&/dev/stdout