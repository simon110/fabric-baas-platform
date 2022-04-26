#!/bin/bash

export CHAINCODE_SRC_DIR=$1
export CHAINCODE_LABEL=$2
export CHAINCODE_PACKAGE_PATH=$3

pushd "$CHAINCODE_SRC_DIR" || exit
GO111MODULE=on go mod vendor
popd || exit
peer lifecycle chaincode package "$CHAINCODE_PACKAGE_PATH" \
  --path "$CHAINCODE_SRC_DIR"\
  --lang golang \
  --label "$CHAINCODE_LABEL" >&/dev/stdout