#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export ORGANIZATION_NAME=$1
export CONFIGTX_PATH=$2
export OUTPUT_JSON_PATH=$3

configtxgen \
  -printOrg "$ORGANIZATION_NAME" \
  -configPath "$CONFIGTX_PATH" \
  >"$OUTPUT_JSON_PATH"
