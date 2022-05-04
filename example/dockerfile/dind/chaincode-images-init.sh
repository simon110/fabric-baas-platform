#!/bin/sh

# shellcheck disable=SC2002
# shellcheck disable=SC2143
# shellcheck disable=SC2236
while [ ! -n "$(cat /var/lib/docker/image/overlay2/repositories.json | grep hyperledger)" ]
do
  sleep 10
  docker load -i /root/hyperledger-fabric-ccenv-2.2.4.tar
  docker load -i /root/hyperledger-fabric-baseos-2.2.4.tar
  docker tag hyperledger/fabric-ccenv:2.2.4 hyperledger/fabric-ccenv:2.2
  docker tag hyperledger/fabric-baseos:2.2.4 hyperledger/fabric-baseos:2.2
  docker tag hyperledger/fabric-ccenv:2.2.4 hyperledger/fabric-ccenv:latest
  docker tag hyperledger/fabric-baseos:2.2.4 hyperledger/fabric-baseos:latest
done