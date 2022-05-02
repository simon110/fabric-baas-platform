package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.function.ThrowableSupplier;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class KubernetesService {
    private final static File KUBERNETES_ADMIN_CONFIG = new File(SimpleFileUtils.getWorkingDir() + "/kubernetes/admin.conf");

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

    public void importAdminConfig(File file) throws IOException {
        FileUtils.copyFile(file, KUBERNETES_ADMIN_CONFIG);
        kubernetesClient = new KubernetesClient(KUBERNETES_ADMIN_CONFIG);
    }

    public List<String> getNodeNames() throws KubernetesException {
        assertAdminConfig();
        List<Node> nodes = kubernetesClient.getAllNodes();
        List<String> nodeNames = new ArrayList<>();
        nodes.forEach(node -> nodeNames.add(node.getMetadata().getName()));
        return nodeNames;
    }

    private void waitFor(String podName, int sleepMs, int timeoutMs) throws InterruptedException, KubernetesException {
        ThrowableSupplier<Boolean, Exception> supplier = () -> {
            List<Pod> pods = kubernetesClient.findPodsByKeyword(podName);
            if (pods.isEmpty()) {
                throw new KubernetesException("未找到相应名称的Pod：" + podName);
            }
            assert pods.size() == 1;
            PodStatus podStatus = pods.get(0).getStatus();
            if ("Running".equals(podStatus.getPhase())) {
                List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
                boolean isAllContainersReady = true;
                for (int i = 0; i < containerStatuses.size() && isAllContainersReady; i++) {
                    ContainerStatus status = containerStatuses.get(i);
                    if (Boolean.FALSE.equals(status.getReady())) {
                        isAllContainersReady = false;
                    }
                }
                return isAllContainersReady;
            }
            return false;
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
        if (!getNodeNames().contains(name)) {
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

            List<Pod> podList = kubernetesClient.findPodsByKeyword(peer.getName());

            assert podList.size() == 1;
            Pod pod = podList.get(0);
            String podName = pod.getMetadata().getName();
            waitFor(podName, 3000, 30000);
            List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            String containerName = null;
            for (int i = 0; i < containerStatuses.size() && containerName == null; i++) {
                ContainerStatus containerStatus = containerStatuses.get(i);
                if (containerStatus.getImage().contains("hyperledger/fabric-peer")) {
                    containerName = containerStatus.getName();
                }
            }
            assert containerName != null;

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

    public void startPeer(String organizationName, PeerEntity peer, String domain, File peerCertfileDir) throws Exception {
        // 检查物理主机是否存在
        assertNodeExists(peer.getKubeNodeName());
        // 检查端口占用情况
        assertNodePortAvailable(peer.getKubeNodePort());
        assertNodePortAvailable(peer.getKubeEventNodePort());
        // 检查是否存在同名Peer
        assertDeploymentNameAvailable(peer.getName());


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

    public void stopPeer(String peerName) throws Exception {
        if (!peerRepo.existsById(peerName)) {
            throw new KubernetesException("不存在Peer：" + peerName);
        }
        deletePeerYaml(peerName);
        peerRepo.deleteById(peerName);
    }

    public void applyOrdererYaml(String ttpOrgName, OrdererEntity orderer, File ordererCertfileDir, File genesisBlock) throws Exception {
        CertfileUtils.assertCertfile(ordererCertfileDir);
        assertAdminConfig();
        File ordererYaml = FabricYamlUtils.getOrdererYaml(orderer.getName());
        FabricYamlUtils.generateOrdererYaml(ttpOrgName, orderer, ordererYaml);
        try {
            kubernetesClient.applyYaml(ordererYaml);

            // 等待容器启动完成
            List<Pod> podList = kubernetesClient.findPodsByKeyword(orderer.getName());
            assert podList.size() == 1;
            Pod pod = podList.get(0);
            String podName = pod.getMetadata().getName();
            waitFor(podName, 5000, 60000);
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

    public void startOrderer(String ttpOrgName, OrdererEntity orderer, File ordererCertfileDir, File genesisBlock) throws Exception {
        // 检查端口是否冲突
        assertNodePortAvailable(orderer.getKubeNodePort());
        // 检查Orderer是否已经存在
        assertDeploymentNameAvailable(orderer.getName());
        // 检查物理主机是否存在
        assertNodeExists(orderer.getKubeNodeName());

        applyOrdererYaml(ttpOrgName, orderer, ordererCertfileDir, genesisBlock);
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

    public void stopOrderer(String ordererName) throws Exception {
        if (!ordererRepo.existsById(ordererName)) {
            throw new KubernetesException("不存在Orderer：" + ordererName);
        }
        deleteOrdererYaml(ordererName);
        ordererRepo.deleteById(ordererName);
    }
}

