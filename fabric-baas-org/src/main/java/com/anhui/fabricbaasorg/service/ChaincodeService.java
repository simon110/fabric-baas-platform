package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.BasicChaincodeProperties;
import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.bean.TlsEnv;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.exception.NodeException;
import com.anhui.fabricbaascommon.fabric.ChaincodeUtils;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
import com.anhui.fabricbaasorg.entity.CommittedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.repository.*;
import com.anhui.fabricbaasorg.request.ChaincodeApproveRequest;
import com.anhui.fabricbaasorg.request.ChaincodeCommitRequest;
import com.anhui.fabricbaasorg.request.ChaincodeInstallRequest;
import com.anhui.fabricbaasorg.response.CommittedChaincodeQueryResult;
import com.anhui.fabricbaasorg.response.InstalledChaincodeQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ChaincodeService {
    @Autowired
    private CommittedChaincodeRepo committedChaincodeRepo;
    @Autowired
    private InstalledChaincodeRepo installedChaincodeRepo;
    @Autowired
    private PeerRepo peerRepo;
    @Autowired
    private OrdererRepo ordererRepo;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private TTPChannelApi ttpChannelApi;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private ChannelRepo channelRepo;

    private CoreEnv buildCoreEnvForPeer(String peerName) throws IOException, NodeException, CertfileException, CaException {
        Optional<PeerEntity> peerOptional = peerRepo.findById(peerName);
        if (peerOptional.isEmpty()) {
            throw new NodeException("Peer不存在：" + peerName);
        }
        PeerEntity peer = peerOptional.get();

        // 获取Peer证书
        File certfileDir = CertfileUtils.getCertfileDir(peer.getName(), CertfileType.PEER);
        CertfileUtils.assertCertfile(certfileDir);

        CoreEnv coreEnv = new CoreEnv();
        CaEntity caEntity = caClientService.findCaEntityOrThrowEx();
        coreEnv.setAddress(caEntity.getDomain() + ":" + peer.getKubeNodePort());
        coreEnv.setMspConfig(CertfileUtils.getMspDir(certfileDir));
        coreEnv.setMspId(caEntity.getOrganizationName());
        coreEnv.setTlsRootCert(new File(certfileDir + "/tls/ca.crt"));
        return coreEnv;
    }

    public void install(ChaincodeInstallRequest request, MultipartFile chaincodePackage) throws Exception {
        // 将链码压缩包写入临时目录
        File tempChaincodePackage = SimpleFileUtils.createTempFile("tar.gz");
        FileUtils.writeByteArrayToFile(tempChaincodePackage, chaincodePackage.getBytes());

        // 执行链码安装
        String packageId = ChaincodeUtils.installChaincode(tempChaincodePackage, buildCoreEnvForPeer(request.getPeerName()));
        InstalledChaincodeEntity installedChaincode = new InstalledChaincodeEntity();
        installedChaincode.setPeerName(request.getPeerName());
        installedChaincode.setIdentifier(packageId);
        installedChaincode.setLabel(request.getChaincodeLabel());
        installedChaincodeRepo.save(installedChaincode);
    }

    /**
     * 如果存在多个Orderer则会随机选择一个
     */
    private TlsEnv buildChannelOrdererTlsEnv(String channelName) throws Exception {
        return null;
    }

    private TlsEnv buildEndorsorPeerTlsEnv(String channelName, Node endorsor) throws Exception {
        File endorsorTlsCert = SimpleFileUtils.createTempFile("crt");
        byte[] endorsorTlsCertData = ttpChannelApi.queryPeerTlsCert(channelName, endorsor);
        FileUtils.writeByteArrayToFile(endorsorTlsCert, endorsorTlsCertData);

        TlsEnv tlsEnv = new TlsEnv();
        tlsEnv.setAddress(endorsor.getAddr());
        tlsEnv.setTlsRootCert(endorsorTlsCert);
        return tlsEnv;
    }

    public void approve(ChaincodeApproveRequest request) throws Exception {
        // 生成Peer的环境变量
        CoreEnv peerCoreEnv = buildCoreEnvForPeer(request.getPeerName());

        // 生成Orderer的环境变量
        TlsEnv ordererTlsEnv = buildChannelOrdererTlsEnv(request.getChannelName());

        BasicChaincodeProperties chaincodeProperties = new BasicChaincodeProperties();
        chaincodeProperties.setName(request.getName());
        chaincodeProperties.setSequence(request.getSequence());
        chaincodeProperties.setVersion(request.getVersion());
        ChaincodeUtils.approveChaincode(ordererTlsEnv, peerCoreEnv, request.getChannelName(), request.getChaincodePackageId(), chaincodeProperties);
    }

    public void commit(ChaincodeCommitRequest request) throws Exception {
        // 生成Peer的环境变量
        CoreEnv peerCoreEnv = buildCoreEnvForPeer(request.getPeerName());

        // 生成Orderer的环境变量
        TlsEnv ordererTlsEnv = buildChannelOrdererTlsEnv(request.getChannelName());

        // 生成背书Peer的环境变量
        List<TlsEnv> endorsorPeerTlsEnvs = new ArrayList<>();
        for (Node node : request.getEndorsorPeers()) {
            endorsorPeerTlsEnvs.add(buildEndorsorPeerTlsEnv(request.getChannelName(), node));
        }

        BasicChaincodeProperties chaincodeProperties = new BasicChaincodeProperties();
        chaincodeProperties.setName(request.getName());
        chaincodeProperties.setSequence(request.getSequence());
        chaincodeProperties.setVersion(request.getVersion());
        ChaincodeUtils.commitChaincode(ordererTlsEnv, peerCoreEnv, endorsorPeerTlsEnvs, request.getChannelName(), chaincodeProperties);

        CommittedChaincodeEntity committedChaincode = new CommittedChaincodeEntity();
        committedChaincode.setChannelName(request.getChannelName());
        committedChaincode.setName(request.getName());
        committedChaincode.setSequence(request.getSequence());
        committedChaincode.setVersion(request.getVersion());
        committedChaincode.setPeerName(request.getPeerName());
        committedChaincodeRepo.save(committedChaincode);
    }

    public InstalledChaincodeQueryResult getInstalledChaincodes() {
        InstalledChaincodeQueryResult result = new InstalledChaincodeQueryResult();
        List<InstalledChaincodeEntity> installedChaincodes = installedChaincodeRepo.findAll();
        result.setInstalledChaincodes(installedChaincodes);
        return result;
    }

    public CommittedChaincodeQueryResult getCommittedChaincodes() {
        CommittedChaincodeQueryResult result = new CommittedChaincodeQueryResult();
        List<CommittedChaincodeEntity> committedChaincodes = committedChaincodeRepo.findAll();
        result.setCommittedChaincodes(committedChaincodes);
        return result;
    }
}
