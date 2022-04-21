#!/bin/bash

export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_TLS_ROOTCERT_FILE=$2
export CORE_PEER_MSPCONFIGPATH=$3
export CORE_PEER_ADDRESS=$4

peer lifecycle chaincode queryinstalled | grep 'Package ID:' >&/dev/stdout