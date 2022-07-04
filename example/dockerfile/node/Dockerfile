FROM ubuntu:focal

COPY bin/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
RUN sh -c "apt update; apt install -y openjdk-11-jre locales; apt clean; locale-gen zh_CN.UTF-8;"
ENV LANG zh_CN.UTF-8
ENV LANGUAGE zh_CN.UTF-8
ENV LC_ALL zh_CN.UTF-8