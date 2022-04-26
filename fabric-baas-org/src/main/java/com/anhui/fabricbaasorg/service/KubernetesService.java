package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.exception.KubernetesException;
import com.anhui.fabricbaasorg.kubernetes.KubernetesClient;
import com.anhui.fabricbaasorg.repository.OrdererRepo;
import com.anhui.fabricbaasorg.repository.PeerRepo;
import com.anhui.fabricbaasorg.util.FabricYamlUtils;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 注意所有的Kubernetes实例的名称应该只包含小写字母、小数点和横杠！
 */
@Slf4j
@Service
public class KubernetesService {
    private final static File KUBERNETES_ADMIN_CONFIG = new File("kubernetes/cluster-config.yaml");

    private KubernetesClient kubernetesClient;
    @Autowired
    private PeerRepo peerRepo;
    @Autowired
    private OrdererRepo ordererRepo;

    public KubernetesService() throws IOException, ApiException {
        if (KUBERNETES_ADMIN_CONFIG.exists()) {
            kubernetesClient = new KubernetesClient(KUBERNETES_ADMIN_CONFIG);
        }
    }

    private void assertAdminConfig() throws KubernetesException {
        if (kubernetesClient == null || !KUBERNETES_ADMIN_CONFIG.exists()) {
            throw new KubernetesException("Kubernetes配置文件不存在");
        }
    }

    public void importAdminConfig(File file) throws IOException, ApiException {
        FileUtils.copyFile(file, KUBERNETES_ADMIN_CONFIG);
        kubernetesClient = new KubernetesClient(KUBERNETES_ADMIN_CONFIG);
    }

    private File findPeerYaml(String peerName) {
        return new File(String.format("kubernetes/peer/%s.yaml", peerName));
    }

    private File findOrdererYaml(String ordererName) {
        return new File(String.format("kubernetes/orderer/%s.yaml", ordererName));
    }

    public List<String> getNodeNames() throws ApiException, KubernetesException {
        assertAdminConfig();
        List<V1Node> nodes = kubernetesClient.getAllNodes();
        List<String> nodeNames = new ArrayList<>();
        for (V1Node node : nodes) {
            nodeNames.add(Objects.requireNonNull(node.getMetadata()).getName());
        }
        return nodeNames;
    }

    private List<V1ContainerStatus> getContainerStatuses(V1Pod pod) {
        V1PodStatus podStatus = pod.getStatus();
        List<V1ContainerStatus> containerStatuses = Objects.requireNonNull(podStatus).getContainerStatuses();
        return Objects.requireNonNull(containerStatuses);
    }

    private void waitForReadiness(String podName, int sleepMs, int timeoutMs) throws ApiException, InterruptedException, KubernetesException {
        while (timeoutMs > 0) {
            TimeUnit.MILLISECONDS.sleep(sleepMs);
            timeoutMs -= sleepMs;
            List<V1Pod> pods = kubernetesClient.getPodsByKeyword(podName);
            if (pods.isEmpty()) {
                throw new KubernetesException("未找到相应名称的Pod：" + podName);
            }
            assert pods.size() == 1;
            V1Pod pod = pods.get(0);
            List<V1ContainerStatus> containerStatuses = getContainerStatuses(pod);
            boolean isAllContainersReady = true;
            for (V1ContainerStatus status : containerStatuses) {
                if (Boolean.FALSE.equals(status.getReady())) {
                    isAllContainersReady = false;
                    break;
                }
            }
            if (isAllContainersReady) {
                return;
            }
        }
        throw new KubernetesException("等待Pod内的容器启动超时：" + podName);
    }

    public void assertKubePortUnused(int kubePort) throws KubernetesException {
        if (!peerRepo.findAllByKubeNodePortOrKubeEventNodePort(kubePort, kubePort).isEmpty() ||
                !ordererRepo.findAllByKubeNodePort(kubePort).isEmpty()) {
            throw new KubernetesException("端口已被占用：" + kubePort);
        }
    }

    public void assertDeploymentNameNotOccupied(String name) throws KubernetesException {
        if (peerRepo.existsById(name) || ordererRepo.existsById(name)) {
            throw new KubernetesException("已存在相同名称的Orderer或Peer：" + name);
        }
    }

    public void assertKubeNodeExisted(String kubeNode) throws ApiException, KubernetesException {
        if (!getNodeNames().contains(kubeNode)) {
            throw new KubernetesException("物理节点不存在：" + kubeNode);
        }
    }

    public void startPeer(PeerEntity peer, String clusterDomain, File peerCertfileDir) throws Exception {
        kubernetesClient.connect();
        // 检查端口占用情况
        assertKubePortUnused(peer.getKubeNodePort());
        assertKubePortUnused(peer.getKubeEventNodePort());

        // 检查是否存在同名Peer
        assertDeploymentNameNotOccupied(peer.getName());

        // 检查物理主机是否存在
        assertKubeNodeExisted(peer.getKubeNodeName());

        // 生产YAML文件并部署到集群
        CertfileUtils.assertCertfile(peerCertfileDir);
        assertAdminConfig();
        File peerYaml = findPeerYaml(peer.getName());
        FabricYamlUtils.generatePeerYaml(peer, clusterDomain, peerYaml);
        kubernetesClient.applyYaml(peerYaml);

        List<V1Pod> podList = kubernetesClient.getPodsByKeyword(peer.getName());

        assert podList.size() == 1;
        V1Pod pod = podList.get(0);
        String podName = Objects.requireNonNull(pod.getMetadata()).getName();
        waitForReadiness(podName, 3000, 30000);
        List<V1ContainerStatus> containerStatuses = getContainerStatuses(pod);
        String containerName = containerStatuses.get(0).getName();

        CertfileUtils.assertCertfile(peerCertfileDir);
        File peerCertfileMSPDir = CertfileUtils.getCertfileMSPDir(peerCertfileDir);
        File peerCertfileTLSDir = CertfileUtils.getCertfileTLSDir(peerCertfileDir);
        kubernetesClient.uploadToContainer(peerCertfileMSPDir, "/var/crypto-config", podName, containerName);
        kubernetesClient.uploadToContainer(peerCertfileTLSDir, "/var/crypto-config", podName, containerName);

        log.info("保存Peer信息：" + peer);
        peerRepo.save(peer);
    }

    public void stopPeer(String peerName) throws Exception {
        if (!peerRepo.existsById(peerName)) {
            throw new KubernetesException("不存在Peer：" + peerName);
        }

        assertAdminConfig();
        File peerYaml = findPeerYaml(peerName);
        if (peerYaml.exists()) {
            kubernetesClient.deleteYaml(peerYaml);
        }
        peerRepo.deleteById(peerName);
    }

    public void startOrderer(OrdererEntity orderer, File ordererCertfileDir, File genesisBlock) throws Exception {
        kubernetesClient.connect();

        // 检查端口是否冲突
        assertKubePortUnused(orderer.getKubeNodePort());

        // 检查Orderer是否已经存在
        assertDeploymentNameNotOccupied(orderer.getName());

        // 检查物理主机是否存在
        assertKubeNodeExisted(orderer.getKubeNodeName());

        CertfileUtils.assertCertfile(ordererCertfileDir);
        assertAdminConfig();
        File ordererYaml = findOrdererYaml(orderer.getName());
        FabricYamlUtils.generateOrdererYaml(orderer, ordererYaml);
        kubernetesClient.applyYaml(ordererYaml);

        // 等待容器启动完成
        List<V1Pod> podList = kubernetesClient.getPodsByKeyword(orderer.getName());
        assert podList.size() == 1;
        V1Pod pod = podList.get(0);
        String podName = Objects.requireNonNull(pod.getMetadata()).getName();
        waitForReadiness(podName, 5000, 60000);
        List<V1ContainerStatus> containerStatuses = getContainerStatuses(pod);
        String containerName = containerStatuses.get(0).getName();

        // 上传创世区块和证书
        CertfileUtils.assertCertfile(ordererCertfileDir);
        File ordererCertfileMSPDir = CertfileUtils.getCertfileMSPDir(ordererCertfileDir);
        File ordererCertfileTLSDir = CertfileUtils.getCertfileTLSDir(ordererCertfileDir);
        kubernetesClient.uploadToContainer(genesisBlock, "/var/crypto-config", podName, containerName);
        kubernetesClient.uploadToContainer(ordererCertfileMSPDir, "/var/crypto-config", podName, containerName);
        kubernetesClient.uploadToContainer(ordererCertfileTLSDir, "/var/crypto-config", podName, containerName);

        log.info("保存Orderer信息：" + orderer);
        ordererRepo.save(orderer);
    }

    public void stopOrderer(String ordererName) throws Exception {
        if (!ordererRepo.existsById(ordererName)) {
            throw new KubernetesException("不存在Orderer：" + ordererName);
        }

        assertAdminConfig();
        File ordererYaml = findOrdererYaml(ordererName);
        if (ordererYaml.exists()) {
            kubernetesClient.connect();
            kubernetesClient.deleteYaml(ordererYaml);
        }
        ordererRepo.deleteById(ordererName);
    }
}

