package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.exception.NodeException;
import com.anhui.fabricbaascommon.function.ThrowableSupplier;
import com.anhui.fabricbaascommon.fabric.CertfileUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.WatcherUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.exception.KubernetesException;
import com.anhui.fabricbaasorg.kubernetes.KubernetesClient;
import com.anhui.fabricbaasorg.repository.OrdererRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import com.anhui.fabricbaasorg.util.FabricYamlUtils;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KubernetesService {
    private final static File KUBERNETES_ADMIN_CONFIG = new File(MyFileUtils.getWorkingDir() + "/kubernetes/admin.conf");

    private KubernetesClient kubernetesClient;
    @Autowired
    private PeerRepo peerRepo;
    @Autowired
    private OrdererRepo ordererRepo;

    public KubernetesService() throws IOException {
        if (KUBERNETES_ADMIN_CONFIG.exists()) {
            kubernetesClient = new KubernetesClient(KUBERNETES_ADMIN_CONFIG);
        }
    }

    private void assertAdminConfig() throws KubernetesException {
        if (kubernetesClient == null || !KUBERNETES_ADMIN_CONFIG.exists()) {
            throw new KubernetesException("Kubernetes配置文件不存在");
        }
    }

    public void importAdminConfig(File file) throws Exception {
        try {
            FileUtils.copyFile(file, KUBERNETES_ADMIN_CONFIG);
            kubernetesClient = new KubernetesClient(KUBERNETES_ADMIN_CONFIG);
            getAllNodeNames();
        } catch (Exception e) {
            boolean delete = KUBERNETES_ADMIN_CONFIG.delete();
            log.warn("已清除连接失败的证书：" + file.getAbsolutePath());
            throw e;
        }
    }

    public List<String> getAllNodeNames() throws KubernetesException {
        assertAdminConfig();
        List<Node> nodes = kubernetesClient.getAllNodes();
        List<String> nodeNames = new ArrayList<>();
        nodes.forEach(node -> nodeNames.add(node.getMetadata().getName()));
        return nodeNames;
    }

    public Node getNode(String nodeName) throws NodeException {
        List<Node> nodes = kubernetesClient.getAllNodes();
        for (Node node : nodes) {
            if (node.getMetadata().getName().equals(nodeName)) {
                return node;
            }
        }
        throw new NodeException("未找到集群节点：" + nodeName);
    }

    public String getCompletePodName(String keyword) {
        List<Pod> podList = kubernetesClient.findPodsByKeyword(keyword);
        Assert.isTrue(podList.size() == 1);
        Pod pod = podList.get(0);
        return pod.getMetadata().getName();
    }

    public PodStatus getPodStatus(String podName) throws KubernetesException {
        List<Pod> pods = kubernetesClient.findPodsByKeyword(podName);
        if (pods.isEmpty()) {
            throw new KubernetesException("未找到相应名称的Pod：" + podName);
        }
        Assert.isTrue(pods.size() == 1);
        return pods.get(0).getStatus();
    }

    public boolean isAllContainersReady(PodStatus podStatus) {
        if ("Running".equals(podStatus.getPhase())) {
            List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
            if (containerStatuses.isEmpty()) {
                return false;
            }
            boolean isAllContainersReady = true;
            for (int i = 0; i < containerStatuses.size() && isAllContainersReady; i++) {
                ContainerStatus status = containerStatuses.get(i);
                isAllContainersReady = status.getReady();
            }
            return isAllContainersReady;
        }
        return false;
    }

    private void waitFor(String podName, int sleepMs, int timeoutMs) throws KubernetesException {
        ThrowableSupplier<Boolean, Exception> supplier = () -> {
            PodStatus podStatus = getPodStatus(podName);
            return isAllContainersReady(podStatus);
        };
        try {
            WatcherUtils.waitFor(supplier, sleepMs, timeoutMs);
        } catch (Exception e) {
            throw new KubernetesException("等待Pod内的容器启动超时：" + podName);
        }
    }

    public void assertDeploymentNameAvailable(String name) throws KubernetesException {
        if (peerRepo.existsById(name) || ordererRepo.existsById(name)) {
            throw new KubernetesException("已存在相同名称的Orderer或Peer：" + name);
        }
    }

    public void assertNodePortAvailable(int port) throws KubernetesException {
        List<PeerEntity> peers = peerRepo.findAllByKubeNodePortOrKubeEventNodePort(port, port);
        List<OrdererEntity> orderers = ordererRepo.findAllByKubeNodePort(port);
        if (!peers.isEmpty() || !orderers.isEmpty()) {
            throw new KubernetesException("端口已被占用：" + port);
        }
    }

    public void assertNodeExists(String name) throws KubernetesException {
        if (!getAllNodeNames().contains(name)) {
            throw new KubernetesException("物理节点不存在：" + name);
        }
    }

    public void applyPeerYaml(String organizationName, PeerEntity peer, String domain, File peerCertfileDir) throws Exception {
        // 生产YAML文件并部署到集群
        CertfileUtils.assertCertfile(peerCertfileDir);
        assertAdminConfig();
        File peerYaml = FabricYamlUtils.getPeerYaml(peer.getName());
        FabricYamlUtils.generatePeerYaml(organizationName, peer, domain, peerYaml);
        try {
            kubernetesClient.applyYaml(peerYaml);

            String podName = getCompletePodName(peer.getName());
            waitFor(podName, 3000, 30000);
            Pod pod = kubernetesClient.findPodsByKeyword(podName).get(0);
            List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            String containerName = null;
            for (int i = 0; i < containerStatuses.size() && containerName == null; i++) {
                ContainerStatus containerStatus = containerStatuses.get(i);
                if (containerStatus.getImage().contains("hyperledger/fabric-peer")) {
                    containerName = containerStatus.getName();
                }
            }
            Assert.notNull(containerName);

            CertfileUtils.assertCertfile(peerCertfileDir);
            File peerCertfileMSPDir = CertfileUtils.getMspDir(peerCertfileDir);
            File peerCertfileTLSDir = CertfileUtils.getTlsDir(peerCertfileDir);
            kubernetesClient.uploadToContainer(peerCertfileMSPDir, "/var/crypto-config/msp", podName, containerName);
            kubernetesClient.uploadToContainer(peerCertfileTLSDir, "/var/crypto-config/tls", podName, containerName);
        } catch (Exception e) {
            kubernetesClient.deleteYaml(peerYaml);
            throw e;
        }
    }

    @Transactional
    public void startPeer(String organizationName, PeerEntity peer, String domain, File peerCertfileDir) throws Exception {
        // 检查物理主机是否存在
        assertNodeExists(peer.getKubeNodeName());
        // 检查端口占用情况
        assertNodePortAvailable(peer.getKubeNodePort());
        assertNodePortAvailable(peer.getKubeEventNodePort());
        // 检查是否存在同名Peer
        assertDeploymentNameAvailable(peer.getName());
        assertPeerExists(peer.getName(), false);

        applyPeerYaml(organizationName, peer, domain, peerCertfileDir);
        log.info("保存Peer信息：" + peer);
        peerRepo.save(peer);
    }

    public void deletePeerYaml(String peerName) throws Exception {
        assertAdminConfig();
        File peerYaml = FabricYamlUtils.getPeerYaml(peerName);
        if (peerYaml.exists()) {
            kubernetesClient.deleteYaml(peerYaml);
        }
    }

    public void assertPeerExists(String peerName, boolean expected) throws KubernetesException {
        if (peerRepo.existsById(peerName) != expected) {
            throw new KubernetesException("Peer存在性断言不符合：" + peerName);
        }
    }

    @Transactional
    public void stopPeer(String peerName) throws Exception {
        assertPeerExists(peerName, true);
        deletePeerYaml(peerName);
        peerRepo.deleteById(peerName);
    }

    public PodStatus getPeerStatus(String peerName) throws Exception {
        assertPeerExists(peerName, true);
        String podName = getCompletePodName(peerName);
        return getPodStatus(podName);
    }

    public void applyOrdererYaml(String ordererOrganizationName, OrdererEntity orderer, File ordererCertfileDir, File genesisBlock) throws Exception {
        CertfileUtils.assertCertfile(ordererCertfileDir);
        assertAdminConfig();
        File ordererYaml = FabricYamlUtils.getOrdererYaml(orderer.getName());
        FabricYamlUtils.generateOrdererYaml(ordererOrganizationName, orderer, ordererYaml);
        try {
            kubernetesClient.applyYaml(ordererYaml);

            // 等待容器启动完成（Pod需要在等待结束后被刷新一次）
            String podName = getCompletePodName(orderer.getName());
            waitFor(podName, 5000, 60000);
            Pod pod = kubernetesClient.findPodsByKeyword(podName).get(0);
            List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            String containerName = containerStatuses.get(0).getName();

            // 上传创世区块和证书
            CertfileUtils.assertCertfile(ordererCertfileDir);
            File ordererCertfileMSPDir = CertfileUtils.getMspDir(ordererCertfileDir);
            File ordererCertfileTLSDir = CertfileUtils.getTlsDir(ordererCertfileDir);
            kubernetesClient.uploadToContainer(genesisBlock, "/var/crypto-config/genesis.block", podName, containerName);
            kubernetesClient.uploadToContainer(ordererCertfileMSPDir, "/var/crypto-config/msp", podName, containerName);
            kubernetesClient.uploadToContainer(ordererCertfileTLSDir, "/var/crypto-config/tls", podName, containerName);
        } catch (Exception e) {
            kubernetesClient.deleteYaml(ordererYaml);
            throw e;
        }
    }

    @Transactional
    public void startOrderer(String ordererOrganizationName, OrdererEntity orderer, File ordererCertfileDir, File genesisBlock) throws Exception {
        // 检查端口是否冲突
        assertNodePortAvailable(orderer.getKubeNodePort());
        // 检查Orderer是否已经存在
        assertDeploymentNameAvailable(orderer.getName());
        assertOrdererExists(orderer.getName(), false);
        // 检查物理主机是否存在
        assertNodeExists(orderer.getKubeNodeName());

        applyOrdererYaml(ordererOrganizationName, orderer, ordererCertfileDir, genesisBlock);
        log.info("保存Orderer信息：" + orderer);
        ordererRepo.save(orderer);
    }

    public void deleteOrdererYaml(String ordererName) throws Exception {
        assertAdminConfig();
        File ordererYaml = FabricYamlUtils.getOrdererYaml(ordererName);
        if (ordererYaml.exists()) {
            kubernetesClient.deleteYaml(ordererYaml);
        }
    }

    public void assertOrdererExists(String ordererName, boolean expected) throws KubernetesException {
        if (ordererRepo.existsById(ordererName) != expected) {
            throw new KubernetesException("Orderer存在性断言不符合：" + ordererName);
        }
    }

    @Transactional
    public void stopOrderer(String ordererName) throws Exception {
        assertPeerExists(ordererName, true);
        deleteOrdererYaml(ordererName);
        ordererRepo.deleteById(ordererName);
    }

    public PodStatus getOrdererStatus(String ordererName) throws Exception {
        assertOrdererExists(ordererName, true);
        String podName = getCompletePodName(ordererName);
        return getPodStatus(podName);
    }
}

