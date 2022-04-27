#!/bin/bash

# shellcheck disable=SC2155
export FABRIC_CFG_PATH=$(pwd)
export CHANNEL_NAME=$1
export OLD_JSON_PATH=$2
export NEW_JSON_PATH=$3
export UPDATE_ENVELOPE_PROTOBUF_PATH=$4
export UPDATE_ENVELOPE_JSON_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).json
export OLD_PROTOBUF_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).pb
export NEW_PROTOBUF_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).pb
export UPDATE_PROTOBUF_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).pb
export UPDATE_JSON_PATH=/tmp/$(cat /proc/sys/kernel/random/uuid).json

# 将两个文件从JSON转回Protobuf的格式
configtxlator proto_encode \
  --input "$OLD_JSON_PATH" \
  --type common.Config \
  >"$OLD_PROTOBUF_PATH"
configtxlator proto_encode \
  --input "$NEW_JSON_PATH" \
  --type common.Config \
  >"$NEW_PROTOBUF_PATH"

# 计算两个Protobuf文件的差异
configtxlator compute_update \
  --channel_id "$CHANNEL_NAME" \
  --original "$OLD_PROTOBUF_PATH" \
  --updated "$NEW_PROTOBUF_PATH" \
  >"$UPDATE_PROTOBUF_PATH"

# 将计算得到的差异文件转换回JSON
configtxlator proto_decode \
  --input "$UPDATE_PROTOBUF_PATH" \
  --type common.ConfigUpdate \
  >"$UPDATE_JSON_PATH"

# 将JSON封装到交易中去
# shellcheck disable=SC2046
echo '{"payload":{"header":{"channel_header":{"channel_id":"'"$CHANNEL_NAME"'", "type":2}},"data":{"config_update":'$(cat "$UPDATE_JSON_PATH")'}}}' | jq . >"$UPDATE_ENVELOPE_JSON_PATH"

# 将交易转码为Protobuf文件
configtxlator proto_encode \
  --input "$UPDATE_ENVELOPE_JSON_PATH" \
  --type common.Envelope \
  >"$UPDATE_ENVELOPE_PROTOBUF_PATH"
