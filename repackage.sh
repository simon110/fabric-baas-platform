#!/bin/bash

mvn clean
mvn package -DskipTests -f pom.xml
