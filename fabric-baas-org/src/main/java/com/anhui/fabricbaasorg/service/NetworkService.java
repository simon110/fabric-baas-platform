package com.anhui.fabricbaasorg.service;


import cn.hutool.core.util.ZipUtil;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaasorg.bean.Participation;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.repository.OrdererRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.response.OrdererQueryResult;
import com.anhui.fabricbaasorg.response.PeerQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@Slf4j
public class NetworkService {
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private CAService caService;
    @Autowired
    private CertfileRepo certfileRepo;
    @Autowired
    private OrdererRepo ordererRepo;
    @Autowired
    private PeerRepo peerRepo;

    private List<Node> getOrderers(List<Integer> ordererPorts) throws CAException {
        String domain = caService.getAdminOrganizationDomain();
        List<Node> orderers = new ArrayList<>();
        for (Integer ordererPort : ordererPorts) {
            Node orderer = new Node();
            orderer.setHost(domain);
            orderer.setPort(ordererPort);
            orderers.add(orderer);
        }
        return orderers;
    }

    public void create(NetworkCreateRequest request) throws Exception {
        // 检查端口是否已经被占用
        for (Integer ordererPort : request.getOrdererPorts()) {
            kubernetesService.assertKubePortUnused(ordererPort);
        }

        // 生成Orderer节点的连接信息
        List<Node> orderers = getOrderers(request.getOrdererPorts());

        // 将CA管理员的证书打包成zip
        File adminCertfileZip = ResourceUtils.createTempFile("zip");
        caService.getAdminCertfileZip(adminCertfileZip);

        // 调用TTP端的接口生成网络
        ttpNetworkApi.createNetwork(request.getNetworkName(), request.getConsortiumName(), orderers, adminCertfileZip);

        // 清除生成的临时Zip
        FileUtils.deleteQuietly(adminCertfileZip);
    }

    public void addOrderer(NetworkAddOrdererRequest request) throws Exception {
        // 获取集群域名
        String domain = caService.getAdminOrganizationDomain();

        Node orderer = new Node();
        orderer.setHost(domain);
        orderer.setPort(request.getOrdererPort());
        ttpNetworkApi.addOrderer(request.getNetworkName(), orderer);
    }

    public void applyParticipation(ParticipationApplyRequest request) throws Exception {
        List<Node> orderers = getOrderers(request.getOrdererPorts());
        File adminCertfileZip = ResourceUtils.createTempFile("zip");
        caService.getAdminCertfileZip(adminCertfileZip);
        // 调用TTP端的接口发送加入网络申请
        ttpNetworkApi.applyParticipation(request.getNetworkName(), orderers, request.getDescription(), adminCertfileZip);
        FileUtils.deleteQuietly(adminCertfileZip);
    }

    public void handleParticipation(ParticipationHandleRequest request) throws Exception {
        ttpNetworkApi.handleParticipation(request.getNetworkName(), request.getOrganizationName(), request.isAccepted());
    }

    public PaginationQueryResult<Participation> queryParticipations(ParticipationQueryRequest request) throws Exception {
        return ttpNetworkApi.queryParticipations(request.getNetworkName(), request.getStatus(), request.getPage(), request.getPageSize());
    }

    public void startOrderer(OrdererStartRequest request) throws Exception {
        // 获取集群域名
        String domain = caService.getAdminOrganizationDomain();

        // 获取网络的创世区块
        byte[] genesisBlockData = ttpNetworkApi.queryGenesisBlock(request.getNetworkName());
        File genesisBlock = ResourceUtils.createTempFile("block");
        FileUtils.writeByteArrayToFile(genesisBlock, genesisBlockData);

        // 获取Orderer的证书并解压
        Node node = new Node();
        node.setHost(domain);
        node.setPort(request.getKubeNodePort());
        byte[] ordererCertfileZipData = ttpNetworkApi.queryOrdererCert(request.getNetworkName(), node);
        File ordererCertfileZip = ResourceUtils.createTempFile("zip");
        FileUtils.writeByteArrayToFile(ordererCertfileZip, ordererCertfileZipData);
        File certfileDir = CertfileUtils.getCertfileDir(request.getName(), CertfileType.ORDERER);
        boolean mkdirs = certfileDir.mkdirs();
        ZipUtil.unzip(ordererCertfileZip, certfileDir);

        // 启动Orderer节点
        OrdererEntity orderer = new OrdererEntity();
        orderer.setName(request.getName());
        orderer.setKubeNodeName(request.getKubeNodeName());
        orderer.setKubeNodePort(request.getKubeNodePort());
        try {
            kubernetesService.startOrderer(orderer, certfileDir, genesisBlock);
        } catch (Exception e) {
            kubernetesService.stopOrderer(orderer.getName());
            throw e;
        }
    }

    public void startPeer(PeerStartRequest request) throws Exception {
        // 获取集群域名
        CAEntity caEntity = caService.getCAEntity();
        String domain = caEntity.getDomain();

        // 生成Peer证书
        File peerCertfileDir = CertfileUtils.getCertfileDir(request.getName(), CertfileType.PEER);
        boolean mkdirs = peerCertfileDir.mkdirs();
        String caUsername = request.getName();
        String caPassword = UUID.randomUUID().toString().replace("-", "");
        try {
            // 尝试注册证书
            caService.register(caUsername, caPassword, CertfileType.PEER);
        } catch (Exception e) {
            log.info("证书已注册：" + caUsername);
        }
        List<String> csrHosts = Arrays.asList("localhost", domain);
        caService.enroll(peerCertfileDir, caUsername, csrHosts);

        // 启动Peer节点
        Optional<CertfileEntity> certificateInfo = certfileRepo.findById(caUsername);
        assert certificateInfo.isPresent();
        caPassword = certificateInfo.get().getCaPassword();
        PeerEntity peer = new PeerEntity();
        peer.setName(request.getName());
        peer.setKubeNodeName(request.getKubeNodeName());
        peer.setKubeNodePort(request.getKubeNodePort());
        peer.setKubeEventNodePort(request.getKubeEventNodePort());
        peer.setCaUsername(caUsername);
        peer.setCaPassword(caPassword);
        peer.setCouchDBUsername("admin");
        peer.setCouchDBPassword(UUID.randomUUID().toString().replace("-", ""));
        peer.setOrganizationName(caEntity.getOrganizationName());
        try {
            kubernetesService.startPeer(peer, domain, peerCertfileDir);
        } catch (Exception e) {
            kubernetesService.stopPeer(peer.getName());
            throw e;
        }
    }

    public OrdererQueryResult getOrderers() {
        List<OrdererEntity> orderers = ordererRepo.findAll();

        OrdererQueryResult result = new OrdererQueryResult();
        result.setOrderers(orderers);
        return result;
    }

    public PeerQueryResult getPeers() {
        List<PeerEntity> peers = peerRepo.findAll();

        PeerQueryResult result = new PeerQueryResult();
        result.setPeers(peers);
        return result;
    }
}
