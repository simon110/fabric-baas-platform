package com.anhui.fabricbaasorg.remote;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
class TTPNetworkApiTest {
    @Autowired
    private RemoteHttpClient remoteHttpClient;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

    @BeforeEach
    public void initClient() throws Exception {
        remoteHttpClient.init("192.168.0.208:8080");
        String token = ttpOrganizationApi.login("TestOrgA", "12345678");
        System.out.println(token);
    }

    @Test
    public void test() throws Exception {
        List<Node> orderers = Arrays.asList(
                new Node("orga.example.com", 30500),
                new Node("orga.example.com", 30501)
        );

        File adminCertZip = new File("example/fabric/TestOrgA/root.zip");
        File sysChannelGenesisBlock = MyFileUtils.createTempFile("block");
        ttpNetworkApi.createNetwork("FirstNetwork", "FirstConsortium", orderers, adminCertZip, sysChannelGenesisBlock);
        Assertions.assertTrue(sysChannelGenesisBlock.exists());
        System.out.println(FileUtils.readFileToByteArray(sysChannelGenesisBlock).length);
        FileUtils.deleteQuietly(sysChannelGenesisBlock);

        List<NetworkOrderer> networkOrderers = ttpNetworkApi.queryOrderers("FirstNetwork");
        Assertions.assertEquals(orderers.size(), networkOrderers.size());
        for (NetworkOrderer networkOrderer : networkOrderers) {
            System.out.println(networkOrderer.getOrganizationName());
            System.out.println(networkOrderer.getAddr());
        }
    }
}