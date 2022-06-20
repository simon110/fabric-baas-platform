FROM openjdk:11.0.15-jdk-oraclelinux7

COPY bin/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
VOLUME /opt/fabric-baas-org
COPY fabric-baas-ttp/target/fabric-baas-ttp.jar /opt/fabric-baas-ttp/