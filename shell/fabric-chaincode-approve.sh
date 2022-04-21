#!/bin/bash

export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_TLS_ROOTCERT_FILE=$2
export CORE_PEER_MSPCONFIGPATH=$3
export CORE_PEER_ADDRESS=$4
export CHAINCODE_PACKAGE_ID=$5
export CHAINCODE_NAME=$6
export CHAINCODE_VERSION=$7
export CHAINCODE_SEQUENCE=$8
export CHANNEL_NAME=$9
export ORDERER_ADDRESS=${10}
export ORDERER_TLS_CERTFILE=${11}

peer lifecycle chaincode approveformyorg \
  -o "$ORDERER_ADDRESS" \
  --cafile "$ORDERER_TLS_CERTFILE" --tls \
  --channelID "$CHANNEL_NAME" \
  --name "$CHAINCODE_NAME" \
  --version "$CHAINCODE_VERSION"\
  --package-id "$CHAINCODE_PACKAGE_ID" \
  --sequence "$CHAINCODE_SEQUENCE" >&/dev/stdout
