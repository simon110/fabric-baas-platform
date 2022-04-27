package com.anhui.fabricbaasorg.kubernetes;

import com.anhui.fabricbaasorg.exception.KubernetesException;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
class KubernetesClientTest {

    @Test
    public void deployBusybox() throws IOException, KubernetesException, InterruptedException {
        File adminConfig = new File("example/kubernetes/admin.conf");
        KubernetesClient kubernetesClient = new KubernetesClient(adminConfig);

        File ordererYaml = new File("example/kubernetes/busybox.yaml");

        System.out.println(kubernetesClient.getAllNodes());
        System.out.println(kubernetesClient.getAllPods());

        kubernetesClient.applyYaml(ordererYaml);
        TimeUnit.SECONDS.sleep(10);
        List<Pod> pods = kubernetesClient.getPodsByKeyword("busybox");
        Assertions.assertFalse(pods.isEmpty());
        Pod busybox = pods.get(0);
        ObjectMeta busyboxMetadata = busybox.getMetadata();
        Assertions.assertNotNull(busyboxMetadata);
        PodStatus busyboxStatus = busybox.getStatus();
        Assertions.assertNotNull(busyboxStatus);
        List<ContainerStatus> containerStatuses = busyboxStatus.getContainerStatuses();
        Assertions.assertNotNull(containerStatuses);
        Assertions.assertEquals(1, containerStatuses.size());

        String podName = busyboxMetadata.getName();
        String containerName = containerStatuses.get(0).getName();
        Assertions.assertNotNull(podName);
        Assertions.assertNotNull(containerName);

        System.out.println(podName);
        System.out.println(containerName);

        kubernetesClient.uploadToContainer(new File("shell"), "/tmp/shell", podName, containerName);
        kubernetesClient.uploadToContainer(new File("pom.xml"), "/tmp/pom.xml", podName, containerName);

        String out = kubernetesClient.executeCommandOnContainer(new String[]{"ls", "/tmp"}, podName, containerName, 5000);
        System.out.println(out);

        kubernetesClient.downloadFileFromContainer("/tmp/pom.xml", new File("temp/pom.xml"), podName, containerName);
        kubernetesClient.deleteYaml(ordererYaml);
    }
}