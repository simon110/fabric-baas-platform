package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Pair;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.PasswordUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
class ChannelServiceTest {
    @Autowired
    private ChannelService channelService;
    @Autowired
    private TTPChannelApi ttpChannelApi;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;
    @Autowired
    private PeerService peerService;
    @Autowired
    private CertService certService;
    @Autowired
    private KubernetesService kubernetesService;


    private final String channelName = "samplechannel";
    private final Pair<String, String> testOrgA = new Pair<>("TestOrgA", "12345678");
    private final Pair<String, String> testOrgB = new Pair<>("TestOrgB", "12345678");
    private final Pair<String, String> testOrgC = new Pair<>("TestOrgC", "12345678");

    @Test
    public void createChannel() throws Exception {
        ttpOrganizationApi.login(testOrgA.getKey(), testOrgA.getValue());
        String networkName = "TestNetwork";
        channelService.create(channelName, networkName);
    }

    @Test
    public void addOrganizations() throws Exception {
        // 生成邀请码
        ttpOrganizationApi.login(testOrgA.getKey(), testOrgA.getValue());
        String invitationCodeAB = ttpChannelApi.generateInvitationCode(channelName, testOrgB.getKey());
        String invitationCodeAC = ttpChannelApi.generateInvitationCode(channelName, testOrgC.getKey());

        // 提交邀请码
        ttpOrganizationApi.login(testOrgB.getKey(), testOrgB.getValue());
        ttpChannelApi.submitInvitationCodes(channelName, Collections.singletonList(invitationCodeAB));
        String invitationCodeBC = ttpChannelApi.generateInvitationCode(channelName, testOrgC.getKey());

        ttpOrganizationApi.login(testOrgC.getKey(), testOrgC.getValue());
        ttpChannelApi.submitInvitationCodes(channelName, Arrays.asList(invitationCodeAC, invitationCodeBC));
    }

    @Test
    public void addPeers() throws Exception {
        // 注册Peer证书
        ttpOrganizationApi.login(testOrgA.getKey(), testOrgA.getValue());
        String caUsername = "TestOrgAPeer0";
        String caPassword = "123456";
        CertfileEntity certfile = new CertfileEntity(caUsername, caPassword, CertfileType.PEER);
        certService.generate(certfile);

        // 将Peer加入通道并设置为锚节点
        PeerEntity peer = new PeerEntity();
        peer.setCaUsername(caUsername);
        peer.setCaPassword(caPassword);
        peer.setCouchDBUsername("admin");
        peer.setCouchDBPassword(PasswordUtils.generate());
        peer.setName("TestOrgAPeer0");
        peer.setKubeNodeName("kubenode2");
        peer.setKubeNodePort(31000);
        peer.setKubeEventNodePort(31500);
        peerService.startPeer(peer);
        TimeUnit.SECONDS.sleep(10);
        Page<PeerEntity> page = peerService.queryPeersInCluster(1, 10);
        Assertions.assertEquals(1, page.getContent().size());
        channelService.join(channelName, peer.getName());
        channelService.updateAnchor(channelName, peer.getName());

        // 模拟其他组织启动Peer
        peer = new PeerEntity();
        peer.setCaUsername(null);
        peer.setCaPassword(null);
        peer.setCouchDBUsername("admin");
        peer.setCouchDBPassword(PasswordUtils.generate());
        peer.setName("TestOrgBPeer0");
        peer.setKubeNodeName("kubenode3");
        peer.setKubeNodePort(31005);
        peer.setKubeEventNodePort(31505);
        File peerCertfile = new File("example/fabric/TestOrgB/peer0.zip");
        File peerCertfileDir = MyFileUtils.createTempDir();
        ZipUtils.unzip(peerCertfile, peerCertfileDir);
        kubernetesService.applyPeerYaml("TestOrgB", peer, "orgb.example.com", peerCertfileDir);
        TimeUnit.SECONDS.sleep(10);
        Node peerNode = new Node("orgb.example.com", 31005);
        ttpOrganizationApi.login(testOrgB.getKey(), testOrgB.getValue());
        ttpChannelApi.joinChannel(channelName, peerNode, peerCertfile);
        ttpChannelApi.setAnchorPeer(channelName, peerNode);

        peer = new PeerEntity();
        peer.setCaUsername(null);
        peer.setCaPassword(null);
        peer.setCouchDBUsername("admin");
        peer.setCouchDBPassword(PasswordUtils.generate());
        peer.setName("TestOrgCPeer0");
        peer.setKubeNodeName("kubenode1");
        peer.setKubeNodePort(31010);
        peer.setKubeEventNodePort(31510);
        peerCertfile = new File("example/fabric/TestOrgC/peer0.zip");
        peerCertfileDir = MyFileUtils.createTempDir();
        ZipUtils.unzip(peerCertfile, peerCertfileDir);
        kubernetesService.applyPeerYaml("TestOrgC", peer, "orgc.example.com", peerCertfileDir);
        TimeUnit.SECONDS.sleep(10);
        peerNode = new Node("orgc.example.com", 31010);
        ttpOrganizationApi.login(testOrgC.getKey(), testOrgC.getValue());
        ttpChannelApi.joinChannel(channelName, peerNode, peerCertfile);
        ttpChannelApi.setAnchorPeer(channelName, peerNode);
    }
}