package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.ChannelException;
import com.anhui.fabricbaascommon.exception.NodeException;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.fabric.CertfileUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.entity.ChannelEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.repository.ChannelRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;

@Service
@Slf4j
public class ChannelService {
    @Autowired
    private TTPChannelApi ttpChannelApi;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private PeerRepo peerRepo;
    @Autowired
    private ChannelRepo channelRepo;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;

    public ChannelEntity findChannelOrThrowEx(String channelName) throws ChannelException {
        Optional<ChannelEntity> optional = channelRepo.findById(channelName);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new ChannelException("通道不存在：" + channelName);
    }

    public PeerEntity findPeerOrThrowEx(String peerName) throws NodeException {
        Optional<PeerEntity> peerOptional = peerRepo.findById(peerName);
        if (peerOptional.isEmpty()) {
            throw new NodeException("未找到相应的Peer节点：" + peerName);
        }
        return peerOptional.get();
    }

    @Transactional
    public void create(String channelName, String networkName) throws Exception {
        ttpChannelApi.createChannel(networkName, channelName);
        ChannelEntity channel = new ChannelEntity(channelName, networkName);
        channelRepo.save(channel);
    }

    public void updateAnchor(String channelName, String peerName) throws Exception {
        // 获取集群域名
        String domain = caClientService.getCaOrganizationDomain();

        // 查询相应的Peer的端口
        PeerEntity peer = findPeerOrThrowEx(peerName);
        Node anchor = new Node(domain, peer.getKubeNodePort());
        ttpChannelApi.setAnchorPeer(channelName, anchor);
    }

    public void join(String channelName, String peerName) throws Exception {
        PeerEntity peer = findPeerOrThrowEx(peerName);
        // 如果Peer节点已经启动必然存在证书
        File certfileDir = CertfileUtils.getCertfileDir(peer.getCaUsername(), CertfileType.PEER);
        File peerCertfileZip = MyFileUtils.createTempFile("zip");
        CertfileUtils.packageCertfile(certfileDir, peerCertfileZip);

        // 调用远程接口
        Node node = new Node(caClientService.getCaOrganizationDomain(), peer.getKubeNodePort());
        ttpChannelApi.joinChannel(channelName, node, peerCertfileZip);
    }

}
