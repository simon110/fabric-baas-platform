package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Pair;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import com.anhui.fabricbaasorg.bean.Participation;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
class NetworkServiceTest {
    @Autowired
    private NetworkService networkService;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;

    @Test
    public void test() throws Exception {
        String networkName = "TestNetwork";
        String consortiumName = "TestConsortium";
        Pair<String, String> testOrgA = new Pair<>("TestOrgA", "12345678");
        Pair<String, String> testOrgB = new Pair<>("TestOrgB", "12345678");
        Pair<String, String> testOrgC = new Pair<>("TestOrgC", "12345678");


        ttpOrganizationApi.login(testOrgA.getKey(), testOrgA.getValue());
        List<OrdererEntity> orderers = Arrays.asList(
                new OrdererEntity("TestOrgAOrderer0", "kubenode1", 30500),
                new OrdererEntity("TestOrgAOrderer1", "kubenode2", 30501)
        );
        networkService.create(networkName, consortiumName, orderers);

        List<NetworkOrderer> networkOrderers = ttpNetworkApi.queryOrderers(networkName);
        for (NetworkOrderer orderer : networkOrderers) {
            System.out.println(orderer.getOrganizationName());
            System.out.println(orderer.getAddr());
        }

        // 模仿其他组织申请加入网络
        ttpOrganizationApi.login(testOrgB.getKey(), testOrgB.getValue());
        ttpNetworkApi.applyParticipation(networkName,
                "This is TestOrgB trying to take part in TestNetwork",
                new File("example/fabric/TestOrgB/root.zip"));
        ttpOrganizationApi.login(testOrgC.getKey(), testOrgC.getValue());
        ttpNetworkApi.applyParticipation(networkName,
                "This is TestOrgC trying to take part in TestNetwork",
                new File("example/fabric/TestOrgC/root.zip"));

        // 处理其他组织加入网络的申请
        ttpOrganizationApi.login(testOrgA.getKey(), testOrgA.getValue());
        PaginationQueryResult<Participation> participations = ttpNetworkApi.queryParticipations(networkName, 0, 1, 10);
        System.out.println(participations);
        Assertions.assertEquals(2, participations.getItems().size());

        ttpNetworkApi.handleParticipation(networkName, testOrgB.getKey(), true);
        ttpNetworkApi.handleParticipation(networkName, testOrgC.getKey(), true);
        List<String> organizationNames = ttpNetworkApi.queryOrganizations(networkName);
        System.out.println(organizationNames);
        Assertions.assertEquals(3, organizationNames.size());

        // 模拟其他组织添加Orderer节点
        ttpOrganizationApi.login(testOrgB.getKey(), testOrgB.getValue());
        ttpNetworkApi.addOrderer(networkName, new Node("orgb.example.com", 30505));
        ttpOrganizationApi.login(testOrgC.getKey(), testOrgC.getValue());
        ttpNetworkApi.addOrderer(networkName, new Node("orgc.example.com", 30510));


    }
}