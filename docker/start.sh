#!/bin/bash

docker-compose -f docker-compose-mongo.yaml up -d
docker-compose -f docker-compose-redis.yaml up -d
docker-compose -f docker-compose-minio.yaml up -d

sleep 15
docker-compose -f docker-compose-app.yaml up -d
