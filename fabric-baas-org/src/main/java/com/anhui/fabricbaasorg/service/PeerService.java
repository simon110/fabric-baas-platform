package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.exception.KubernetesException;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class PeerService {
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private PeerRepo peerRepo;

    public void startPeer(PeerEntity peer) throws Exception {
        if (!peer.getKubeNodePort().equals(peer.getKubeEventNodePort())) {
            throw new KubernetesException("Peer的主端口和事件端口必须不同！");
        }
        // 获取集群域名
        CaEntity caEntity = caClientService.findCaEntityOrThrowEx();
        String domain = caEntity.getDomain();

        // 生成Peer证书
        CertfileEntity certfile = caClientService.findCertfileOrThrowEx(peer.getCaUsername());
        if (!certfile.getCaUsertype().equals(CertfileType.PEER)) {
            throw new CertfileException("错误的证书类型：" + certfile.getCaUsertype());
        }
        if (!certfile.getCaPassword().equals(peer.getCaPassword())) {
            throw new CertfileException("CA证书密码错误！");
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

    public Page<PeerEntity> queryPeersInCluster(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return peerRepo.findAll(pageable);
    }
}
