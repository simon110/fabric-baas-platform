package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.util.PasswordUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
class KubernetesServiceTest {
    @Autowired
    private KubernetesService kubernetesService;

    @Test
    public void test() throws Exception {
        String ordererOrganizationName = "GXNU";

        for (int i = 0; i < 2; i++) {
            OrdererEntity orderer = new OrdererEntity();
            orderer.setKubeNodePort(30500 + i);
            orderer.setKubeNodeName("kubenode1");
            orderer.setName("TestOrgAOrderer" + i);

            File ordererCertfileDir = new File(String.format("example/kubernetes/TestOrgA/orderer%d", i));
            Assertions.assertTrue(ordererCertfileDir.isDirectory());
            File sysChannelGenesis = new File(ordererCertfileDir + "/genesis.block");
            Assertions.assertTrue(sysChannelGenesis.isFile());
            kubernetesService.startOrderer(ordererOrganizationName, orderer, ordererCertfileDir, sysChannelGenesis);
        }

        for (int i = 0; i < 2; i++) {
            PeerEntity peer = new PeerEntity();
            peer.setCaUsername(null);
            peer.setCaPassword(null);
            peer.setCouchDBUsername("admin");
            peer.setCouchDBPassword(PasswordUtils.generate());
            peer.setName("TestOrgAPeer" + i);
            peer.setKubeNodeName("kubenode2");
            peer.setKubeNodePort(31000 + i);
            peer.setKubeEventNodePort(31500 + i);

            File peerCertfileDir = new File(String.format("example/kubernetes/TestOrgA/peer%d", i));
            Assertions.assertTrue(peerCertfileDir.isDirectory());
            kubernetesService.startPeer("TestOrgA", peer, "orga.example.com", peerCertfileDir);
        }

        TimeUnit.SECONDS.sleep(30);
        kubernetesService.stopOrderer("TestOrgAOrderer0");
        kubernetesService.stopOrderer("TestOrgAOrderer1");
        kubernetesService.stopPeer("TestOrgAPeer0");
        kubernetesService.stopPeer("TestOrgAPeer1");
    }
}