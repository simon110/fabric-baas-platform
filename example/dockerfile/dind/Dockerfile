FROM docker:18.05-dind

COPY hyperledger-fabric-baseos-2.2.4.tar /root
COPY hyperledger-fabric-ccenv-2.2.4.tar /root
COPY chaincode-images-init.sh /usr/local/bin
COPY dind-entrypoint.sh /usr/local/bin
RUN chmod +x /usr/local/bin/chaincode-images-init.sh
RUN chmod +x /usr/local/bin/dind-entrypoint.sh
ENTRYPOINT ["dind-entrypoint.sh"]