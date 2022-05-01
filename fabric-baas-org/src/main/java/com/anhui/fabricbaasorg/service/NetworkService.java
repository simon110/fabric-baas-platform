package com.anhui.fabricbaasorg.service;


import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
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
}
