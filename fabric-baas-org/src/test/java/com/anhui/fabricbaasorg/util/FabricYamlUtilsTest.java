package com.anhui.fabricbaasorg.util;

import com.anhui.fabricbaascommon.util.RandomUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;

@RunWith(SpringRunner.class)
class FabricYamlUtilsTest {
    private static final String[] ORGANIZATION_NAMES = {"TestOrgA", "TestOrgB", "TestOrgC", "TestOrgD"};
    private static final String[] ORGANIZATION_DOMAINS = {"orga.example.com", "orgb.example.com", "orgc.example.com", "orgd.example.com"};
    private static final String[] KUBERNETES_NODE_NAMES = {"kubenode1", "kubenode2", "kubenode3"};
    private static final String BASE_DIR_PATH = "example/kubernetes";

    @Test
    void generateOrdererYaml() throws IOException {
        int ordererNodePort = 30500;
        int ordererCount = 3;

        for (String organizationName : ORGANIZATION_NAMES) {
            for (int i = 0; i < ordererCount; i++) {
                OrdererEntity orderer = new OrdererEntity();
                orderer.setName(String.format("%sOrderer%d", organizationName, i));
                orderer.setKubeNodeName(RandomUtils.select(KUBERNETES_NODE_NAMES));
                orderer.setKubeNodePort(ordererNodePort++);

                File yaml = new File(String.format("%s/%s/orderer%d.yaml", BASE_DIR_PATH, organizationName, i));
                FabricYamlUtils.generateOrdererYaml(orderer, yaml);
                Assertions.assertTrue(yaml.exists());
            }
        }
    }

    @Test
    void generatePeerYaml() throws IOException {
        int peerNodePort = 31000;
        int peerEventNodePort = 31500;
        int peerCount = 3;

        String couchDBUsername = "admin";
        String couchDBPassword = "MhDAAH3kLfLVKA1slf";

        for (int i = 0; i < ORGANIZATION_NAMES.length; i++) {
            String organizationName = ORGANIZATION_NAMES[i];
            String organizationDomain = ORGANIZATION_DOMAINS[i];
            for (int j = 0; j < peerCount; j++) {
                PeerEntity peer = new PeerEntity();
                peer.setName(String.format("%sPeer%d", organizationName, j));
                peer.setKubeNodeName(RandomUtils.select(KUBERNETES_NODE_NAMES));
                peer.setKubeNodePort(peerNodePort++);
                peer.setKubeEventNodePort(peerEventNodePort++);
                peer.setCouchDBUsername(couchDBUsername);
                peer.setCouchDBPassword(couchDBPassword);
                peer.setOrganizationName(organizationName);
                // CA信息不会被用到
                peer.setCaUsername(null);
                peer.setCaPassword(null);

                File yaml = new File(String.format("%s/%s/peer%d.yaml", BASE_DIR_PATH, organizationName, j));
                FabricYamlUtils.generatePeerYaml(peer, organizationDomain, yaml);
                Assertions.assertTrue(yaml.exists());
            }
        }
    }
}