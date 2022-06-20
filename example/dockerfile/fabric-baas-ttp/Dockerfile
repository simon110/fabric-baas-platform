FROM openjdk:11.0.15-jdk-oraclelinux7

COPY bin/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
VOLUME /opt/fabric-baas-ttp
COPY fabric-baas-ttp/target/fabric-baas-org.jar /opt/fabric-baas-org/