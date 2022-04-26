#!/bin/bash

export FABRIC_CA_CLIENT_HOME=$1
export CA_SERVER_NAME=$2
export CA_SERVER_ADDR=$3
export CA_SERVER_CERT=$4
export ID_USERNAME=$5
export ID_PASSWORD=$6
export CSR_HOSTS=$7
export CA_SERVER_URL="https://${ID_USERNAME}:${ID_PASSWORD}@${CA_SERVER_ADDR}"

if [ -d "$FABRIC_CA_CLIENT_HOME" ]; then
  rm -rf "$FABRIC_CA_CLIENT_HOME"
fi
mkdir -p "$FABRIC_CA_CLIENT_HOME"

fabric-ca-client enroll \
  -u "$CA_SERVER_URL" \
  --caname "$CA_SERVER_NAME" \
  --mspdir "$FABRIC_CA_CLIENT_HOME"/msp \
  --csr.hosts "$CSR_HOSTS" \
  --tls.certfiles "$CA_SERVER_CERT" >&/dev/stdout
fabric-ca-client enroll \
  -u "$CA_SERVER_URL" \
  --caname "$CA_SERVER_NAME" \
  --mspdir "$FABRIC_CA_CLIENT_HOME"/tls \
  --csr.hosts "$CSR_HOSTS" \
  --tls.certfiles "$CA_SERVER_CERT" \
  --enrollment.profile tls >&/dev/stdout

cp -r "$FABRIC_CA_CLIENT_HOME"/tls/tlscacerts "$FABRIC_CA_CLIENT_HOME"/msp
mv "$FABRIC_CA_CLIENT_HOME"/msp/cacerts/* "$FABRIC_CA_CLIENT_HOME"/msp/cacerts/ca.pem
mv "$FABRIC_CA_CLIENT_HOME"/msp/keystore/* "$FABRIC_CA_CLIENT_HOME"/msp/keystore/key.pem
mv "$FABRIC_CA_CLIENT_HOME"/msp/signcerts/* "$FABRIC_CA_CLIENT_HOME"/msp/signcerts/cert.pem
cp "$FABRIC_CA_CLIENT_HOME"/tls/tlscacerts/* "$FABRIC_CA_CLIENT_HOME"/tls/ca.crt
cp "$FABRIC_CA_CLIENT_HOME"/tls/signcerts/* "$FABRIC_CA_CLIENT_HOME"/tls/server.crt
cp "$FABRIC_CA_CLIENT_HOME"/tls/keystore/* "$FABRIC_CA_CLIENT_HOME"/tls/server.key

echo "NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/ca.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/ca.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/ca.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/ca.pem
    OrganizationalUnitIdentifier: orderer" \
  >"$FABRIC_CA_CLIENT_HOME"/msp/config.yaml
