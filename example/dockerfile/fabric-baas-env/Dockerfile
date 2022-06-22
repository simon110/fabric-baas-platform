FROM ubuntu:focal

COPY bin/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
RUN sh -c "apt update; apt install -y openjdk-11-jre; apt clean;"