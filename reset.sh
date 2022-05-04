#!/bin/bash

# shellcheck disable=SC2164
pushd docker
docker-compose -f docker-compose-fabric-ca.yaml down
docker-compose -f docker-compose-mongo.yaml down
docker-compose -f docker-compose-minio.yaml down
mv fabric-ca/fabric-ca-server-config.yaml .
rm -rf mongo minio fabric-ca
mkdir fabric-ca
mv fabric-ca-server-config.yaml fabric-ca
docker-compose -f docker-compose-minio.yaml up -d
docker-compose -f docker-compose-mongo.yaml up -d
popd

rm -rf temp/*
rm -rf static/download/*
rm -rf fabric/root/*
rm -rf fabric/certfile/*/*
rm kubernetes/peer/*
rm kubernetes/orderer/*
chmod +x shell/*