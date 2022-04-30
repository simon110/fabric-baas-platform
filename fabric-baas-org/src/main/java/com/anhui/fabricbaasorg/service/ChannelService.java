package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import com.anhui.fabricbaasorg.entity.ChannelEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.repository.ChannelRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.response.InvitationGenerateResult;
import com.spotify.docker.client.exceptions.NodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void create(ChannelCreateRequest request) throws Exception {
        ttpChannelApi.createChannel(request.getNetworkName(), request.getChannelName());

        ChannelEntity channel = new ChannelEntity();
        channel.setName(request.getChannelName());
        channel.setNetworkName(request.getNetworkName());
        channelRepo.save(channel);
    }

    public void updateAnchor(AnchorPeerUpdateRequest request) throws Exception {
        // 获取集群域名
        String domain = caClientService.getCaOrganizationDomain();

        // 查询相应的Peer的端口
        Optional<PeerEntity> peerOptional = peerRepo.findById(request.getPeerName());
        if (peerOptional.isEmpty()) {
            throw new NodeNotFoundException("未找到相应的Peer节点：" + request.getPeerName());
        }
        Node peer = new Node();
        peer.setHost(domain);
        peer.setPort(peerOptional.get().getKubeNodePort());
        ttpChannelApi.setAnchorPeer(request.getChannelName(), peer);
    }

    public InvitationGenerateResult generateInvitation(InvitationGenerateRequest request) throws Exception {
        String invitationCode = ttpChannelApi.generateInvitationCode(request.getChannelName(), request.getInvitedOrganizationName());
        InvitationGenerateResult result = new InvitationGenerateResult();
        result.setInvitation(invitationCode);
        return result;
    }

    public void submitInvitations(InvitationSubmitRequest request) throws Exception {
        ttpChannelApi.submitInvitationCodes(request.getChannelName(), request.getInvitations());
    }

    public void join(ChannelJoinRequest request) throws Exception {
        // 如果Peer节点已经启动必然存在证书
        File certfileDir = CertfileUtils.getCertfileDir(request.getPeerName(), CertfileType.PEER);
        CertfileUtils.assertCertfile(certfileDir);
        File peerCertfileZip = SimpleFileUtils.createTempFile("zip");
        ZipUtils.zip(peerCertfileZip, CertfileUtils.getMspDir(certfileDir), CertfileUtils.getTlsDir(certfileDir));

        // 获取Peer连接信息
        Optional<PeerEntity> peerOptional = peerRepo.findById(request.getPeerName());
        if (peerOptional.isEmpty()) {
            throw new NodeNotFoundException("未找到相应的Peer节点");
        }
        Node peer = new Node();
        peer.setHost(caClientService.getCaOrganizationDomain());
        peer.setPort(peerOptional.get().getKubeNodePort());

        // 调用远程接口
        ttpChannelApi.joinChannel(request.getChannelName(), peer, peerCertfileZip);
    }
}
