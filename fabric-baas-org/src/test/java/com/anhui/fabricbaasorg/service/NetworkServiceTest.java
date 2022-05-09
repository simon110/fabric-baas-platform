package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Pair;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
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
    @Autowired
    private KubernetesService kubernetesService;

    private final String networkName = "TestNetwork";
    private final Pair<String, String> testOrgA = new Pair<>("TestOrgA", "12345678");
    private final Pair<String, String> testOrgB = new Pair<>("TestOrgB", "12345678");
    private final Pair<String, String> testOrgC = new Pair<>("TestOrgC", "12345678");

    @Test
    public void createNetwork() throws Exception {
        final String consortiumName = "TestConsortium";
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
    }

    @Test
    public void addOrganizations() throws Exception {
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

        ttpNetworkApi.handleParticipation(networkName, testOrgB.getKey(), true);
        ttpNetworkApi.handleParticipation(networkName, testOrgC.getKey(), true);
        List<String> organizationNames = ttpNetworkApi.queryOrganizations(networkName);
        System.out.println(organizationNames);
        Assertions.assertEquals(3, organizationNames.size());
    }

    @Test
    public void addOrderers() throws Exception {
        // 获取排序组织名称
        String ordererOrganizationName = ttpOrganizationApi.getOrdererOrganizationName();
        System.out.println(ordererOrganizationName);

        // 模拟其他组织添加Orderer节点
        ttpOrganizationApi.login(testOrgB.getKey(), testOrgB.getValue());
        Node newOrderer = new Node("orgb.example.com", 30505);
        ttpNetworkApi.addOrderer(networkName, newOrderer);
        File genesisBlock = MyFileUtils.createTempFile("block");
        File newOrdererCert = MyFileUtils.createTempFile("zip");
        ttpNetworkApi.queryGenesisBlock(networkName, genesisBlock);
        ttpNetworkApi.queryOrdererCert(networkName, newOrderer, newOrdererCert);
        File ordererCertfileDir = MyFileUtils.createTempDir();
        ZipUtils.unzip(newOrdererCert, ordererCertfileDir);
        kubernetesService.applyOrdererYaml(ordererOrganizationName,
                new OrdererEntity("TestOrgBOrderer0", "kubenode3", newOrderer.getPort()),
                ordererCertfileDir, genesisBlock);

        ttpOrganizationApi.login(testOrgC.getKey(), testOrgC.getValue());
        newOrderer = new Node("orgc.example.com", 30510);
        ttpNetworkApi.addOrderer(networkName, newOrderer);
        genesisBlock = MyFileUtils.createTempFile("block");
        newOrdererCert = MyFileUtils.createTempFile("zip");
        ttpNetworkApi.queryGenesisBlock(networkName, genesisBlock);
        ttpNetworkApi.queryOrdererCert(networkName, newOrderer, newOrdererCert);
        ordererCertfileDir = MyFileUtils.createTempDir();
        ZipUtils.unzip(newOrdererCert, ordererCertfileDir);
        kubernetesService.applyOrdererYaml(ordererOrganizationName,
                new OrdererEntity("TestOrgCOrderer0", "kubenode1", newOrderer.getPort()),
                ordererCertfileDir, genesisBlock);
    }
}