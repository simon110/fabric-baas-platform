package com.anhui.fabricbaasorg.kubernetes;

import com.anhui.fabricbaasorg.exception.KubernetesException;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Streams;
import io.kubernetes.client.util.Yaml;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 注意所有的Kubernetes实例的名称应该只包含小写字母、小数点和横杠！
 * 所以该类会对所有的与实例名称相关的输入进行处理，将大写字母变成小写。
 */
@Data
@Slf4j
public class KubernetesClient {
    private static final String KUBERNETES_NAMESPACE = "default";
    private ApiClient apiClient;

    /**
     * @param adminConfig 包含集群的链接信息和证书信息
     */
    public KubernetesClient(File adminConfig) throws IOException, ApiException {
        FileReader fileReader = new FileReader(adminConfig);
        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(fileReader);
        this.apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
        getAllNodes();
        getAllPods();
    }

    public void connect() {
        Configuration.setDefaultApiClient(apiClient);
    }

    /**
     * 将编排文件定义的内容部署到集群中
     */
    public void applyYaml(File yaml) throws IOException, ApiException, KubernetesException {
        connect();
        List<Object> objects = Yaml.loadAll(yaml);
        AppsV1Api appsV1Api = new AppsV1Api();
        CoreV1Api coreV1Api = new CoreV1Api();
        for (Object object : objects) {
            KubernetesObject kubernetesObject = (KubernetesObject) object;
            String kind = kubernetesObject.getKind();
            if ("Deployment".equals(kind)) {
                appsV1Api.createNamespacedDeployment(KUBERNETES_NAMESPACE, (V1Deployment) kubernetesObject, null, null, null);
            } else if ("Service".equals(kind)) {
                coreV1Api.createNamespacedService(KUBERNETES_NAMESPACE, (V1Service) kubernetesObject, null, null, null);
            } else if ("Pod".equals(kind)) {
                coreV1Api.createNamespacedPod(KUBERNETES_NAMESPACE, (V1Pod) kubernetesObject, null, null, null);
            } else {
                throw new KubernetesException("不受支持的Kubernetes对象类型");
            }
        }
    }

    /**
     * 将编排文件定义的内容从集群中删掉。
     * 目前只用到了Deployment和Service两种类型，所以暂时不去支持其他类。
     */
    public void deleteYaml(File yaml) throws IOException, ApiException, KubernetesException {
        connect();
        List<Object> objects = Yaml.loadAll(yaml);
        AppsV1Api appsV1Api = new AppsV1Api();
        CoreV1Api coreV1Api = new CoreV1Api();
        for (Object object : objects) {
            KubernetesObject kubernetesObject = (KubernetesObject) object;
            String kind = kubernetesObject.getKind();
            String name = kubernetesObject.getMetadata().getName();
            if ("Deployment".equals(kind)) {
                appsV1Api.deleteNamespacedDeployment(name, KUBERNETES_NAMESPACE, null, null, null, null, null, new V1DeleteOptions());
            } else if ("Service".equals(kind)) {
                coreV1Api.deleteNamespacedService(name, KUBERNETES_NAMESPACE, null, null, null, null, null, new V1DeleteOptions());
            } else if ("Pod".equals(kind)) {
                coreV1Api.deleteNamespacedPod(name, KUBERNETES_NAMESPACE, null, null, null, null, null, new V1DeleteOptions());
            } else {
                throw new KubernetesException("不受支持的Kubernetes对象类型");
            }
        }
    }

    /**
     * 将本地的内容复制到集群中的指定容器中
     * 路径规范参考scp命令
     *
     * @param src           要上传的文件或文件夹的本地路径
     * @param dst           要上传的容器路径
     * @param podName       集群Pod名称
     * @param containerName Pod中的容器名称
     */
    public void uploadToContainer(File src, String dst, String podName, String containerName) throws IOException, ApiException {
        connect();
        Copy copy = new Copy();
        copy.copyFileToPod(KUBERNETES_NAMESPACE, podName.toLowerCase(), containerName.toLowerCase(), src.toPath(), Paths.get(dst));
    }

    /**
     * 将容器的内容下载到本地
     * 路径规范参考scp命令
     *
     * @param src           要下载文件或文件夹的容器路径
     * @param dst           要下载到的本地路径
     * @param containerName 集群Pod名称
     */
    public void downloadFromContainer(String src, File dst, String podName, String containerName) throws IOException, ApiException {
        connect();
        Copy copy = new Copy();
        InputStream dataStream = copy.copyFileFromPod(KUBERNETES_NAMESPACE, podName.toLowerCase(), containerName.toLowerCase(), src);
        Streams.copy(dataStream, new FileOutputStream(dst));
    }

    /**
     * 在集群中的指定容器上执行命令
     */
    public static void executeCommandOnContainer(KubernetesClient kubernetesClient, String[] command, String podName, String containerName, boolean asnyc) throws IOException, ApiException, InterruptedException {
        kubernetesClient.connect();
        Exec exec = new Exec();
        Process process = exec.exec(KUBERNETES_NAMESPACE, podName.toLowerCase(), command, containerName.toLowerCase(), false, false);
        if (!asnyc) {
            process.waitFor();
        }
        // process.destroy();
    }


    /**
     * 获取集群内所有节点的名称，用于nodeSelector
     *
     * @return 集群内所有节点的名称
     */
    public List<V1Node> getAllNodes() throws ApiException {
        connect();
        CoreV1Api api = new CoreV1Api();
        V1NodeList nodes = api.listNode(null, null, null, null, null, null, null, null, null, null);
        return nodes.getItems();
    }

    /**
     * 获取集群内所有Pod
     *
     * @return 集群内所有Pod
     */
    public List<V1Pod> getAllPods() throws ApiException {
        connect();
        CoreV1Api api = new CoreV1Api();
        V1PodList podList = api.listNamespacedPod(KUBERNETES_NAMESPACE, null, null, null, null, null, null, null, null, null, null);
        return podList.getItems();
    }

    public List<V1Pod> getPodsByKeyword(String keyword) throws ApiException {
        String lowerCaseKeyword = keyword.toLowerCase();
        List<V1Pod> pods = getAllPods();
        return pods.stream().filter(pod -> {
            String podName = Objects.requireNonNull(pod.getMetadata()).getName();
            return Objects.requireNonNull(podName).contains(lowerCaseKeyword);
        }).collect(Collectors.toList());
    }
}

