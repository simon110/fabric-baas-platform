package com.anhui.fabricbaasorg.service;

import cn.hutool.core.util.ZipUtil;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.OrdererRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OrdererService {
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private OrdererRepo ordererRepo;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

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

    public Page<OrdererEntity> queryOrderersInCluster(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return ordererRepo.findAll(pageable);
    }


}
