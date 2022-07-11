package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.annotation.CacheClean;
import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.fabric.ChaincodeUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import com.anhui.fabricbaasorg.repository.ApprovedChaincodeRepo;
import com.anhui.fabricbaasorg.repository.InstalledChaincodeRepo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Service
@Slf4j
@CacheConfig(cacheNames = "org")
public class ChaincodeService {
    @Autowired
    private ApprovedChaincodeRepo approvedChaincodeRepo;
    @Autowired
    private InstalledChaincodeRepo installedChaincodeRepo;
    @Autowired
    private FabricService fabricService;
    @Autowired
    private ChannelService channelService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @CacheClean(patterns = "'ChaincodeService:queryInstalledChaincodes:*'")
    @CacheEvict(key = "'ChaincodeService:getAllInstalledChaincodesOnPeer:'+#peerName")
    @Transactional
    public String install(String peerName, String chaincodeLabel, MultipartFile chaincodePackage) throws Exception {
        // 将链码压缩包写入临时目录
        File tempChaincodePackage = MyFileUtils.createTempFile("tar.gz");
        FileUtils.writeByteArrayToFile(tempChaincodePackage, chaincodePackage.getBytes());

        // 执行链码安装
        String packageId = ChaincodeUtils.installChaincode(tempChaincodePackage, fabricService.buildPeerCoreEnv(peerName));
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
        CoreEnv peerCoreEnv = fabricService.buildPeerCoreEnv(peerName);
        TlsEnv ordererTlsEnv = fabricService.buildOrdererTlsEnv(channelName);
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

    @Cacheable(keyGenerator = "keyGenerator")
    public ApprovedChaincodeEntity findApprovedChaincodeOrThrowEx(ApprovedChaincode approvedChaincode) {
        List<ApprovedChaincodeEntity> approvedChaincodes = approvedChaincodeRepo.findAllByChannelNameAndNameAndSequenceAndVersion(
                approvedChaincode.getChannelName(),
                approvedChaincode.getName(),
                approvedChaincode.getSequence(),
                approvedChaincode.getVersion()
        );
        Assert.isTrue(approvedChaincodes.size() == 1);
        return approvedChaincodes.get(0);
    }

    public List<ChaincodeApproval> getChaincodeApprovals(ApprovedChaincode approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        ApprovedChaincodeEntity entity = findApprovedChaincodeOrThrowEx(approvedChaincode);
        CoreEnv peerCoreEnv = fabricService.buildPeerCoreEnv(entity.getPeerName());
        TlsEnv ordererTlsEnv = fabricService.buildOrdererTlsEnv(channelName);
        return ChaincodeUtils.checkCommittedReadiness(ordererTlsEnv, peerCoreEnv, channelName, approvedChaincode);
    }

    @Transactional
    @CacheEvict(key = "'ChaincodeService:findApprovedChaincodeOrThrowEx:'+#approvedChaincode.toString()")
    @CacheClean(patterns = {
            "'ChaincodeService:queryApprovedChaincodes:*'",
            "'ChaincodeService:queryCommittedChaincodes:*'",
            "'ChaincodeService:getAllCommittedChaincodesOnChannel:*'"
    })
    public void commit(List<Node> endorsers, ApprovedChaincode approvedChaincode) throws Exception {
        String channelName = approvedChaincode.getChannelName();
        ApprovedChaincodeEntity entity = findApprovedChaincodeOrThrowEx(approvedChaincode);
        CoreEnv peerCoreEnv = fabricService.buildPeerCoreEnv(entity.getPeerName());
        TlsEnv ordererTlsEnv = fabricService.buildOrdererTlsEnv(channelName);

        List<TlsEnv> endorserTlsEnvs = new ArrayList<>();
        for (Node endorser : endorsers) {
            endorserTlsEnvs.add(fabricService.buildEndorserTlsEnv(channelName, endorser));
        }
        ChaincodeUtils.commitChaincode(ordererTlsEnv, peerCoreEnv, endorserTlsEnvs, channelName, approvedChaincode);

        // 更新链码生效状态
        entity.setCommitted(true);
        approvedChaincodeRepo.save(entity);
    }

    @SneakyThrows
    public void synchronizeApprovedChaincodeStatuses() {
        List<ApprovedChaincodeEntity> entities = approvedChaincodeRepo.findAllByCommitted(false);
        Map<String, List<ApprovedChaincode>> map = new HashMap<>();

        Set<String> expiredRedisKeySet = new HashSet<>();
        boolean isUpdated = false;
        for (ApprovedChaincodeEntity entity : entities) {
            // 如果已经知道链码生效了就不必要更新状态了
            if (entity.isCommitted()) {
                continue;
            }
            String channelName = entity.getChannelName();
            // 查询通道上所有已经生效的链码
            List<ApprovedChaincode> approvedChaincodes = map.getOrDefault(channelName, null);
            if (approvedChaincodes == null) {
                CoreEnv peerCoreEnv = fabricService.buildPeerCoreEnv(entity.getPeerName());
                approvedChaincodes = ChaincodeUtils.queryCommittedChaincodes(channelName, peerCoreEnv);
                map.put(channelName, approvedChaincodes);
            }
            // 如果链码有新状态则更新
            for (ApprovedChaincode approvedChaincode : approvedChaincodes) {
                if (approvedChaincode.getName().equals(entity.getName()) &&
                        approvedChaincode.getVersion().equals(entity.getVersion()) &&
                        approvedChaincode.getSequence().equals(entity.getSequence())) {
                    isUpdated = true;
                    entity.setCommitted(true);
                    approvedChaincodeRepo.save(entity);
                    log.info("已更新链码状态：" + entity);

                    // 增加删除的缓存键值
                    expiredRedisKeySet.add("org:ChaincodeService:findApprovedChaincodeOrThrowEx:" + approvedChaincode);
                    expiredRedisKeySet.add("org:ChaincodeService:getAllCommittedChaincodesOnChannel:" + approvedChaincode.getChannelName());
                }
            }
        }

        if (isUpdated) {
            expiredRedisKeySet.add("org:ChaincodeService:queryApprovedChaincodes:*");
            expiredRedisKeySet.add("org:ChaincodeService:queryCommittedChaincodes:*");
        }
        redisTemplate.delete(expiredRedisKeySet);
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public Page<ApprovedChaincodeEntity> queryApprovedChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return approvedChaincodeRepo.findAll(pageable);
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public Page<InstalledChaincodeEntity> queryInstalledChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return installedChaincodeRepo.findAll(pageable);
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public Page<ApprovedChaincodeEntity> queryCommittedChaincodes(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return approvedChaincodeRepo.findAllByCommitted(true, pageable);
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public List<InstalledChaincodeEntity> getAllInstalledChaincodesOnPeer(String peerName) {
        return installedChaincodeRepo.findAllByPeerName(peerName);
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public List<ApprovedChaincodeEntity> getAllCommittedChaincodesOnChannel(String channelName) {
        return approvedChaincodeRepo.findAllByChannelNameAndCommitted(channelName, true);
    }

    public String executeQuery(String chaincodeName, String channelName, String functionName, List<String> params, String peerName) throws Exception {
        channelService.findChannelOrThrowEx(channelName);
        CoreEnv peerCoreEnv = fabricService.buildPeerCoreEnv(peerName);
        return ChaincodeUtils.executeQuery(chaincodeName, functionName, params, channelName, peerCoreEnv);
    }

    public void executeInvoke(String chaincodeName, String channelName, String functionName, List<String> params, String peerName, List<Node> endorserPeers) throws Exception {
        channelService.findChannelOrThrowEx(channelName);
        CoreEnv committerCoreEnv = fabricService.buildPeerCoreEnv(peerName);
        TlsEnv ordererTlsEnv = fabricService.buildOrdererTlsEnv(channelName);

        List<TlsEnv> endorserTlsEnvs = new ArrayList<>();
        for (Node endorserPeer : endorserPeers) {
            TlsEnv endorserTlsEnv = fabricService.buildEndorserTlsEnv(channelName, endorserPeer);
            endorserTlsEnvs.add(endorserTlsEnv);
        }
        ChaincodeUtils.executeInvoke(chaincodeName, functionName, params, channelName, ordererTlsEnv, committerCoreEnv, endorserTlsEnvs);
    }
}
