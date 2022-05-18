#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export CORE_PEER_TLS_ROOTCERT_FILE=$3
export CORE_PEER_ADDRESS=$4
export CHANNEL_NAME=$5
export CHAINCODE_NAME=$6
export CHAINCODE_PARAMS=$7

peer chaincode query -C "$CHANNEL_NAME" -n "$CHAINCODE_NAME" -c "'$CHAINCODE_PARAMS'" >&/dev/stdout
