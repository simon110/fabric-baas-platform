package com.anhui.fabricbaasorg.service;


import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
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
    private OrdererService orderService;

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

    public void create(String networkName, String consortiumName, List<OrdererEntity> orderers) throws Exception {
        // 检查端口是否已经被占用
        List<Integer> ordererPorts = new ArrayList<>(orderers.size());
        for (OrdererEntity orderer : orderers) {
            int port = orderer.getKubeNodePort();
            kubernetesService.assertNodePortAvailable(port);
            ordererPorts.add(port);
        }
        // 生成Orderer节点的连接信息
        List<Node> ordererEndpoints = buildOrderers(ordererPorts);
        // 将CA管理员的证书打包成zip
        File adminCertfileZip = MyFileUtils.createTempFile("zip");
        caClientService.getRootCertfileZip(adminCertfileZip);
        // 调用TTP端的接口生成网络
        File sysChannelGenesis = MyFileUtils.createTempFile("block");
        ttpNetworkApi.createNetwork(networkName, consortiumName, ordererEndpoints, adminCertfileZip, sysChannelGenesis);
        // 用创世区块来启动所有预计提供的Orderer节点
        for (OrdererEntity orderer : orderers) {
            orderService.startOrderer(networkName, orderer, sysChannelGenesis);
        }
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
        File adminCertfileZip = MyFileUtils.createTempFile("zip");
        caClientService.getRootCertfileZip(adminCertfileZip);
        // 调用TTP端的接口发送加入网络申请
        ttpNetworkApi.applyParticipation(networkName, description, adminCertfileZip);
        FileUtils.deleteQuietly(adminCertfileZip);
    }
}
