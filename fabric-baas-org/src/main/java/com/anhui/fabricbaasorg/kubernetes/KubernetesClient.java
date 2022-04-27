package com.anhui.fabricbaasorg.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 注意所有的Kubernetes实例的名称应该只包含小写字母、小数点和横杠！
 * 所以该类会对所有的与实例名称相关的输入进行处理，将大写字母变成小写。
 */
@Data
@Slf4j
public class KubernetesClient {
    private static final String KUBERNETES_NAMESPACE = "default";
    private io.fabric8.kubernetes.client.KubernetesClient apiClient;

    /**
     * @param adminConfig 包含集群的链接信息和证书信息
     */
    public KubernetesClient(File adminConfig) throws IOException {
        FileReader fileReader = new FileReader(adminConfig);
        Config kubeConfig = Config.fromKubeconfig(FileUtils.readFileToString(adminConfig, StandardCharsets.UTF_8));
        this.apiClient = new DefaultKubernetesClient(kubeConfig);
        getAllNodes();
        getAllPods();
    }

    /**
     * 将编排文件定义的内容部署到集群中
     */
    public void applyYaml(File yaml) throws IOException {
        apiClient.load(new FileInputStream(yaml))
                .inNamespace(KUBERNETES_NAMESPACE)
                .createOrReplace();
    }

    /**
     * 将编排文件定义的内容从集群中删掉。
     * 目前只用到了Deployment和Service两种类型，所以暂时不去支持其他类。
     */
    public void deleteYaml(File yaml) throws IOException {
        apiClient.load(new FileInputStream(yaml))
                .inNamespace(KUBERNETES_NAMESPACE)
                .delete();
    }

    /**
     * 将本地的内容复制到集群中的指定容器中
     *
     * @param src           要上传的文件或文件夹的本地路径
     * @param dst           要上传的容器路径（行为参考kubectl cp）
     * @param podName       集群Pod名称
     * @param containerName Pod中的容器名称
     */
    public void uploadToContainer(File src, String dst, String podName, String containerName) throws IOException {
        if (src.isDirectory()) {
            apiClient.pods().inNamespace(KUBERNETES_NAMESPACE)
                    .withName(podName.toLowerCase())
                    .inContainer(containerName.toLowerCase())
                    .dir(dst)
                    .upload(src.toPath());
        } else if (src.isFile()) {
            apiClient.pods().inNamespace(KUBERNETES_NAMESPACE)
                    .withName(podName.toLowerCase())
                    .inContainer(containerName.toLowerCase())
                    .file(dst)
                    .upload(src.toPath());
        } else {
            throw new IOException("目标不存在：" + src.getAbsolutePath());
        }
    }

    /**
     * 将容器的内容下载到本地（注意不支持文件夹）
     *
     * @param src           要下载文件或文件夹的容器路径
     * @param dst           要下载到的本地路径
     * @param containerName 集群Pod名称
     */
    public void downloadFileFromContainer(String src, File dst, String podName, String containerName) throws IOException {
        apiClient.pods()
                .inNamespace(KUBERNETES_NAMESPACE)
                .withName(podName.toLowerCase())
                .inContainer(containerName.toLowerCase())
                .file(src)
                .copy(dst.toPath());
    }

    /**
     * 在集群中的指定容器上执行命令
     */
    public String executeCommandOnContainer(String[] command, String podName, String containerName, int timeoutMs) throws IOException, InterruptedException {
        CountDownLatch execLatch = new CountDownLatch(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        try (ExecWatch execWatch = apiClient.pods()
                .inNamespace(KUBERNETES_NAMESPACE)
                .withName(podName.toLowerCase())
                .inContainer(containerName.toLowerCase())
                .writingOutput(out)
                .writingError(error)
                .usingListener(new PodExecListener(execLatch))
                .exec(command)) {
            boolean latchTerminationStatus = execLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!latchTerminationStatus) {
                log.warn("命令未能在规定时间内结束！");
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }


    /**
     * 获取集群内所有节点的名称，用于nodeSelector
     *
     * @return 集群内所有节点的名称
     */
    public List<Node> getAllNodes() {
        return apiClient.nodes().list().getItems();
    }

    /**
     * 获取集群内所有Pod
     *
     * @return 集群内所有Pod
     */
    public List<Pod> getAllPods() {
        PodList podList = apiClient.pods().inNamespace(KUBERNETES_NAMESPACE).list();
        return podList.getItems();
    }

    public List<Pod> getPodsByKeyword(String keyword) {
        String lowerCaseKeyword = keyword.toLowerCase();
        List<Pod> pods = getAllPods();
        return pods.stream().filter(pod -> {
            String podName = Objects.requireNonNull(pod.getMetadata()).getName();
            return Objects.requireNonNull(podName).contains(lowerCaseKeyword);
        }).collect(Collectors.toList());
    }
}

