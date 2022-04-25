#!/bin/bash

organizations=("TestOrgA" "TestOrgB" "TestOrgC" "TestOrgD")
domains=("orga.example.com" "orgb.example.com" "orgc.example.com" "orgd.example.com")
usertypes=("peer" "orderer")
password='OMX0LmIyXdt8CC9U'
address='localhost:7154'

for ((i = 0; i < ${#organizations[@]}; i++)); do
    organization=${organizations[i]}
    domain=${domains[i]}
    cd "$organization" || exit
    rm -rf admin* peer* orderer* fabric-ca
    # 启动CA服务
    docker-compose -f docker-compose-fabric-ca.yaml up -d
    sleep 15
    if [ -e "$(pwd)/fabric-ca/tls-cert.pem" ]; then
        # 登记CA管理员
        mkdir admin
        ../../../shell/fabric-ca-enroll.sh "$(pwd)/admin" "${organization}CA" "$address" "$(pwd)/fabric-ca/tls-cert.pem" 'admin' "$password" "localhost,$domain"
        pushd admin || exit
        zip -r -q -o "../admin.zip" msp tls
        popd || exit

        # 生成Peer、Orderer和Client
        for usertype in ${usertypes[@]}; do
            for j in $(seq 3); do
                mkdir "${usertype}${j}"
                username="${organization}-${usertype}${j}"
                ../../../shell/fabric-ca-register.sh "$(pwd)/admin" "$(pwd)/fabric-ca/tls-cert.pem" "${organization}CA" "$username" "$password" "$usertype"
                ../../../shell/fabric-ca-enroll.sh "$(pwd)/${usertype}${j}" "${organization}CA" "$address" "$(pwd)/fabric-ca/tls-cert.pem" "$username" "$password" "localhost,$domain"

                pushd "${usertype}${j}" || exit
                zip -r -q -o "../${usertype}${j}.zip" msp tls
                popd || exit
            done
        done
    fi
    # 关闭CA服务
    docker-compose -f docker-compose-fabric-ca.yaml down
    cd ..
done
