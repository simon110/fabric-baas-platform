package com.anhui.fabricbaasorg.service;


import cn.hutool.core.util.ZipUtil;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.OrdererRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NetworkService {
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private CertfileRepo certfileRepo;
    @Autowired
    private OrdererRepo ordererRepo;
    @Autowired
    private PeerRepo peerRepo;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

    private List<Node> buildOrderers(List<Integer> ordererPorts) throws CaException {
        String domain = caClientService.getCaOrganizationDomain();
        List<Node> orderers = new ArrayList<>();
        for (Integer ordererPort : ordererPorts) {
            Node orderer = new Node();
            orderer.setHost(domain);
            orderer.setPort(ordererPort);
            orderers.add(orderer);
        }
        return orderers;
    }

    public void create(String networkName, String consortiumName, List<Integer> ordererPorts) throws Exception {
        // 检查端口是否已经被占用
        for (Integer port : ordererPorts) {
            kubernetesService.assertNodePortAvailable(port);
        }
        // 生成Orderer节点的连接信息
        List<Node> orderers = buildOrderers(ordererPorts);
        // 将CA管理员的证书打包成zip
        File adminCertfileZip = SimpleFileUtils.createTempFile("zip");
        caClientService.getRootCertfileZip(adminCertfileZip);
        // 调用TTP端的接口生成网络
        ttpNetworkApi.createNetwork(networkName, consortiumName, orderers, adminCertfileZip);
        // 清除生成的临时Zip
        FileUtils.deleteQuietly(adminCertfileZip);
    }

    public void addOrderer(String networkName, int ordererPort) throws Exception {
        // 获取集群域名
        String domain = caClientService.getCaOrganizationDomain();
        Node orderer = new Node(domain, ordererPort);
        ttpNetworkApi.addOrderer(networkName, orderer);
    }

    public void applyParticipation(String networkName, String description) throws Exception {
        File adminCertfileZip = SimpleFileUtils.createTempFile("zip");
        caClientService.getRootCertfileZip(adminCertfileZip);
        // 调用TTP端的接口发送加入网络申请
        ttpNetworkApi.applyParticipation(networkName, description, adminCertfileZip);
        FileUtils.deleteQuietly(adminCertfileZip);
    }

    public void startOrderer(String networkName, OrdererEntity orderer) throws Exception {
        // 获取网络的创世区块
        File networkGenesisBlock = SimpleFileUtils.createTempFile("block");
        ttpNetworkApi.queryGenesisBlock(networkName, networkGenesisBlock);

        // 获取集群域名
        String domain = caClientService.getCaOrganizationDomain();

        // 获取Orderer的证书并解压
        Node node = new Node(domain, orderer.getKubeNodePort());
        File ordererCertfileZip = SimpleFileUtils.createTempFile("zip");
        ttpNetworkApi.queryOrdererCert(networkName, node, ordererCertfileZip);
        File certfileDir = CertfileUtils.getCertfileDir(orderer.getName(), CertfileType.ORDERER);
        boolean mkdirs = certfileDir.mkdirs();
        ZipUtil.unzip(ordererCertfileZip, certfileDir);

        // 启动Orderer节点
        String ordererOrganizationName = ttpOrganizationApi.getOrdererOrganizationName();
        kubernetesService.startOrderer(ordererOrganizationName, orderer, certfileDir, networkGenesisBlock);
    }

    public void startPeer(PeerEntity peer) throws Exception {
        // 获取集群域名
        CaEntity caEntity = caClientService.findCaEntityOrThrowEx();
        String domain = caEntity.getDomain();

        // 生成Peer证书
        CertfileEntity certfile = caClientService.findCertfileOrThrowEx(peer.getCaUsername());
        if (!certfile.getCaUsertype().equals(CertfileType.PEER)) {
            throw new CertfileException("错误的证书类型：" + certfile.getCaUsertype());
        }
        
        // 如果证书还没有存放到正式目录则进行登记
        File peerCertfileDir = CertfileUtils.getCertfileDir(peer.getCaUsername(), CertfileType.PEER);
        if (!CertfileUtils.checkCertfile(peerCertfileDir)) {
            List<String> csrHosts = CaUtils.buildCsrHosts(domain);
            caClientService.enroll(peerCertfileDir, peer.getCaUsername(), csrHosts);
        }

        // 启动Peer节点
        kubernetesService.startPeer(caEntity.getOrganizationName(), peer, domain, peerCertfileDir);
    }
}
