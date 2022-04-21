#!/bin/bash

export CORE_PEER_LOCALMSPID=$1
export CORE_PEER_MSPCONFIGPATH=$2
export ENVELOPE_PROTOBUF_PATH=$5

peer channel signconfigtx -f "$ENVELOPE_PROTOBUF_PATH" >&/dev/stdout
