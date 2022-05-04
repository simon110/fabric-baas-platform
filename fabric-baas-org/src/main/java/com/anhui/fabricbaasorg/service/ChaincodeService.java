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
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import com.anhui.fabricbaasorg.entity.ChannelEntity;
import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.repository.ApprovedChaincodeRepo;
import com.anhui.fabricbaasorg.repository.InstalledChaincodeRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChaincodeService {
    @Autowired
    private ApprovedChaincodeRepo approvedChaincodeRepo;
    @Autowired
    private InstalledChaincodeRepo installedChaincodeRepo;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private TTPChannelApi ttpChannelApi;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private ChannelService channelService;

    private CoreEnv buildPeerCoreEnv(String peerName) throws NodeException, CertfileException, CaException {
        // 获取Peer证书
        PeerEntity peer = channelService.findPeerOrThrowEx(peerName);
        File certfileDir = CertfileUtils.getCertfileDir(peer.getCaUsername(), CertfileType.PEER);
        CertfileUtils.assertCertfile(certfileDir);

        CoreEnv coreEnv = new CoreEnv();
        CaEntity caEntity = caClientService.findCaEntityOrThrowEx();
        coreEnv.setAddress(caEntity.getDomain() + ":" + peer.getKubeNodePort());
        coreEnv.setMspConfig(CertfileUtils.getMspDir(caClientService.getRootCertfileDir()));
        coreEnv.setMspId(caEntity.getOrganizationName());
        coreEnv.setTlsRootCert(CertfileUtils.getTlsCaCert(certfileDir));
        return coreEnv;
    }

    private TlsEnv buildOrdererTlsEnv(String channelName) throws Exception {
        ChannelEntity channel = channelService.findChannelOrThrowEx(channelName);
        String networkName = channel.getNetworkName();
        List<NetworkOrderer> orderers = ttpNetworkApi.queryOrderers(networkName);
        // Node selectedOrderer = RandomUtils.select(orderers);
        Node selectedOrderer = orderers.get(0);

        File ordererTlsCert = MyFileUtils.createTempFile("crt");
        ttpNetworkApi.queryOrdererTlsCert(networkName, selectedOrderer, ordererTlsCert);
        return new TlsEnv(selectedOrderer.getAddr(), ordererTlsCert);
    }

    private TlsEnv buildEndorserTlsEnv(String channelName, Node endorser) throws Exception {
        File endorserTlsCert = MyFileUtils.createTempFile("crt");
        ttpChannelApi.queryPeerTlsCert(channelName, endorser, endorserTlsCert);
        return new TlsEnv(endorser.getAddr(), endorserTlsCert);
    }

    public String install(String peerName, String chaincodeLabel, MultipartFile chaincodePackage) throws Exception {
        // 将链码压缩包写入临时目录
        File tempChaincodePackage = MyFileUtils.createTempFile("tar.gz");
        FileUtils.writeByteArrayToFile(tempChaincodePackage, chaincodePackage.getBytes());

        // 执行链码安装
        String packageId = ChaincodeUtils.installChaincode(tempChaincodePackage, buildPeerCoreEnv(peerName));
        InstalledChaincodeEntity installedChaincode = new InstalledChaincodeEntity();
        installedChaincode.setPeerName(peerName);
        installedChaincode.setIdentifier(packageId);
        installedChaincode.setLabel(chaincodeLabel);
        installedChaincodeRepo.save(installedChaincode);

        return packageId;
    }

    public void approve(ApprovedChaincodeEntity approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        CoreEnv peerCoreEnv = buildPeerCoreEnv(approvedChaincode.getPeerName());
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);
        ChaincodeUtils.approveChaincode(ordererTlsEnv, peerCoreEnv, channelName,
                approvedChaincode.getInstalledChaincodeIdentifier(), approvedChaincode);
        approvedChaincodeRepo.save(approvedChaincode);
    }

    public void commit(String peerName, String channelName, List<Node> endorsers, BasicChaincodeProperties chaincodeProperties) throws Exception {
        CoreEnv peerCoreEnv = buildPeerCoreEnv(peerName);
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);

        List<TlsEnv> endorserTlsEnvs = new ArrayList<>();
        for (Node endorser : endorsers) {
            endorserTlsEnvs.add(buildEndorserTlsEnv(channelName, endorser));
        }

        ChaincodeUtils.commitChaincode(ordererTlsEnv, peerCoreEnv, endorserTlsEnvs, channelName, chaincodeProperties);

        ApprovedChaincodeEntity committedChaincode = new ApprovedChaincodeEntity();
        committedChaincode.setChannelName(channelName);
        committedChaincode.setName(chaincodeProperties.getName());
        committedChaincode.setSequence(chaincodeProperties.getSequence());
        committedChaincode.setVersion(chaincodeProperties.getVersion());
        committedChaincode.setPeerName(peerName);
        approvedChaincodeRepo.save(committedChaincode);
    }

    public void getChaincodeApprovals(String channelName) {

    }

    public Page<InstalledChaincodeEntity> queryInstalledChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return installedChaincodeRepo.findAll(pageable);
    }

    public Page<ApprovedChaincodeEntity> queryCommittedChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return approvedChaincodeRepo.findAll(pageable);
    }

    public List<InstalledChaincodeEntity> getAllInstalledChaincodesOnPeer(String peerName) {
        return installedChaincodeRepo.findAllByPeerName(peerName);
    }

    public List<ApprovedChaincodeEntity> getAllCommittedChaincodesOnChannel(String channelName) {
        return approvedChaincodeRepo.findAllByChannelName(channelName);
    }
}
