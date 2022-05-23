package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.*;
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
import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.ChannelEntity;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Transactional
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

    @Transactional
    public void approve(String peerName, String chaincodeIdentifier, ApprovedChaincode approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        CoreEnv peerCoreEnv = buildPeerCoreEnv(peerName);
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);
        ChaincodeUtils.approveChaincode(ordererTlsEnv, peerCoreEnv, channelName, chaincodeIdentifier, approvedChaincode);

        ApprovedChaincodeEntity entity = new ApprovedChaincodeEntity();
        entity.setPeerName(peerName);
        entity.setChannelName(channelName);
        entity.setSequence(approvedChaincode.getSequence());
        entity.setVersion(approvedChaincode.getVersion());
        entity.setCommitted(false);
        entity.setName(approvedChaincode.getName());
        entity.setInstalledChaincodeIdentifier(chaincodeIdentifier);
        approvedChaincodeRepo.save(entity);
    }

    public ApprovedChaincodeEntity findApprovedChaincodeOrThrowEx(ApprovedChaincode approvedChaincode) {
        List<ApprovedChaincodeEntity> approvedChaincodes = approvedChaincodeRepo.findAllByChannelNameAndNameAndSequenceAndVersion(
                approvedChaincode.getChannelName(), approvedChaincode.getName(), approvedChaincode.getSequence(), approvedChaincode.getVersion()
        );
        assert approvedChaincodes.size() == 1;
        return approvedChaincodes.get(0);
    }

    public List<ChaincodeApproval> getChaincodeApprovals(ApprovedChaincode approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        ApprovedChaincodeEntity entity = findApprovedChaincodeOrThrowEx(approvedChaincode);
        CoreEnv peerCoreEnv = buildPeerCoreEnv(entity.getPeerName());
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);
        return ChaincodeUtils.checkCommittedReadiness(ordererTlsEnv, peerCoreEnv, channelName, approvedChaincode);
    }

    @Transactional
    public void commit(List<Node> endorsers, ApprovedChaincode approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        ApprovedChaincodeEntity entity = findApprovedChaincodeOrThrowEx(approvedChaincode);
        CoreEnv peerCoreEnv = buildPeerCoreEnv(entity.getPeerName());
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);

        List<TlsEnv> endorserTlsEnvs = new ArrayList<>();
        for (Node endorser : endorsers) {
            endorserTlsEnvs.add(buildEndorserTlsEnv(channelName, endorser));
        }
        ChaincodeUtils.commitChaincode(ordererTlsEnv, peerCoreEnv, endorserTlsEnvs, channelName, approvedChaincode);

        // 更新链码生效状态
        entity.setCommitted(true);
        approvedChaincodeRepo.save(entity);
    }

    public void syncApprovedChaincodeStatuses() {
        List<ApprovedChaincodeEntity> entities = approvedChaincodeRepo.findAllByCommitted(false);
        Map<String, List<ApprovedChaincode>> map = new HashMap<>();
        entities.forEach(entity -> {
            try {
                String channelName = entity.getChannelName();
                List<ApprovedChaincode> approvedChaincodes = map.getOrDefault(channelName, null);
                if (approvedChaincodes == null) {
                    CoreEnv peerCoreEnv = buildPeerCoreEnv(entity.getPeerName());
                    approvedChaincodes = ChaincodeUtils.queryCommittedChaincodes(channelName, peerCoreEnv);
                    map.put(channelName, approvedChaincodes);
                }
                for (ApprovedChaincode approvedChaincode : approvedChaincodes) {
                    if (approvedChaincode.getName().equals(entity.getName()) &&
                            approvedChaincode.getVersion().equals(entity.getVersion()) &&
                            approvedChaincode.getSequence().equals(entity.getSequence())) {
                        entity.setCommitted(true);
                        approvedChaincodeRepo.save(entity);
                    }
                }
            } catch (Exception e) {
                log.warn("同步链码状态时发生异常：" + e);
            }
        });
    }

    public Page<ApprovedChaincodeEntity> queryApprovedChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return approvedChaincodeRepo.findAll(pageable);
    }

    public Page<InstalledChaincodeEntity> queryInstalledChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return installedChaincodeRepo.findAll(pageable);
    }

    public Page<ApprovedChaincodeEntity> queryCommittedChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return approvedChaincodeRepo.findAllByCommitted(true, pageable);
    }

    public List<InstalledChaincodeEntity> getAllInstalledChaincodesOnPeer(String peerName) {
        return installedChaincodeRepo.findAllByPeerName(peerName);
    }

    public List<ApprovedChaincodeEntity> getAllCommittedChaincodesOnChannel(String channelName) {
        return approvedChaincodeRepo.findAllByChannelNameAndCommitted(channelName, true);
    }

    public String executeQuery(String chaincodeName, String channelName, String functionName, List<String> params, String peerName) throws Exception {
        channelService.findChannelOrThrowEx(channelName);
        CoreEnv peerCoreEnv = buildPeerCoreEnv(peerName);
        return ChaincodeUtils.executeQuery(chaincodeName, functionName, params, channelName, peerCoreEnv);
    }

    public void executeInvoke(String chaincodeName, String channelName, String functionName, List<String> params, String peerName, List<Node> endorserPeers) throws Exception {
        channelService.findChannelOrThrowEx(channelName);
        CoreEnv committerCoreEnv = buildPeerCoreEnv(peerName);
        TlsEnv ordererTlsEnv = buildOrdererTlsEnv(channelName);

        List<TlsEnv> endorserTlsEnvs = new ArrayList<>();
        for (Node endorserPeer : endorserPeers) {
            TlsEnv endorserTlsEnv = buildEndorserTlsEnv(channelName, endorserPeer);
            endorserTlsEnvs.add(endorserTlsEnv);
        }
        ChaincodeUtils.executeInvoke(chaincodeName, functionName, params, channelName, ordererTlsEnv, committerCoreEnv, endorserTlsEnvs);
    }
}
