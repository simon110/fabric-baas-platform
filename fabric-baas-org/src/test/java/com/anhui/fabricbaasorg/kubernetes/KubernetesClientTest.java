package com.anhui.fabricbaasorg.kubernetes;

import cn.hutool.core.lang.Pair;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
class KubernetesClientTest {

    // @Test
    public void deployBusybox() throws IOException, InterruptedException {
        File adminConfig = new File("example/kubernetes/admin.conf");
        KubernetesClient kubernetesClient = new KubernetesClient(adminConfig);

        File busyboxYaml = new File("example/kubernetes/busybox.yaml");

        System.out.println(kubernetesClient.getAllNodes());
        System.out.println(kubernetesClient.getAllPods());

        kubernetesClient.applyYaml(busyboxYaml);
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
        kubernetesClient.deleteYaml(busyboxYaml);
    }

    @Test
    public void deployInitialOrderers() throws IOException, InterruptedException {
        File adminConfig = new File("example/kubernetes/admin.conf");
        KubernetesClient kubernetesClient = new KubernetesClient(adminConfig);
        List<Pair<String, String>> pairs = new ArrayList<>();
        // pairs.add(new Pair<>("TestOrgA", "orderer0"));
        // pairs.add(new Pair<>("TestOrgA", "orderer1"));
        // pairs.add(new Pair<>("TestOrgB", "orderer0"));
        pairs.add(new Pair<>("TestOrgC", "orderer0"));

        for (Pair<String, String> pair : pairs) {
            File ordererYaml = new File(String.format("example/kubernetes/%s/%s.yaml", pair.getKey(), pair.getValue()));
            File materialDir = new File(String.format("temp/%s/%s", pair.getKey(), pair.getValue()));

            kubernetesClient.applyYaml(ordererYaml);
            TimeUnit.SECONDS.sleep(10);

            List<Pod> pods = kubernetesClient.getPodsByKeyword((pair.getKey() + pair.getValue()).toLowerCase());
            Assertions.assertEquals(1, pods.size());
            Pod orderer = pods.get(0);
            ObjectMeta ordererMetadata = orderer.getMetadata();
            Assertions.assertNotNull(ordererMetadata);
            PodStatus ordererStatus = orderer.getStatus();
            Assertions.assertNotNull(ordererStatus);
            List<ContainerStatus> containerStatuses = ordererStatus.getContainerStatuses();
            Assertions.assertNotNull(containerStatuses);
            Assertions.assertEquals(1, containerStatuses.size());

            String podName = ordererMetadata.getName();
            String containerName = containerStatuses.get(0).getName();
            Assertions.assertNotNull(podName);
            Assertions.assertNotNull(containerName);

            kubernetesClient.uploadToContainer(new File(materialDir + "/msp"), "/var/crypto-config/msp", podName, containerName);
            kubernetesClient.uploadToContainer(new File(materialDir + "/tls"), "/var/crypto-config/tls", podName, containerName);
            kubernetesClient.uploadToContainer(new File(materialDir + "/genesis.block"), "/var/crypto-config/genesis.block", podName, containerName);
        }
    }
}