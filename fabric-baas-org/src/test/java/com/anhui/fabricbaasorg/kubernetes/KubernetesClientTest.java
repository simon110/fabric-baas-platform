package com.anhui.fabricbaasorg.kubernetes;

import com.anhui.fabricbaascommon.util.MyFileUtils;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
class KubernetesClientTest {
    @Test
    public void test() throws IOException, InterruptedException {
        File kubernetesConfig = new File("example/kubernetes/admin.conf");
        KubernetesClient kubernetesClient = new KubernetesClient(kubernetesConfig);
        System.out.println(kubernetesClient.getAllNodes());

        File busyboxYaml = new File("example/kubernetes/busybox.yaml");
        try {
            kubernetesClient.applyYaml(busyboxYaml);
            System.out.println(kubernetesClient.getAllPods());
            TimeUnit.SECONDS.sleep(3);

            List<Pod> pods = kubernetesClient.findPodsByKeyword("busybox");
            Assertions.assertEquals(pods.size(), 1);
            Pod busyboxPod = pods.get(0);
            String podName = busyboxPod.getMetadata().getName();
            String containerName = busyboxPod.getStatus().getContainerStatuses().get(0).getName();

            String pingResult = kubernetesClient.executeCommandOnContainer(new String[]{"ping", "orga.example.com", "-c", "3"}, podName, containerName, 5000);
            System.out.println(pingResult);
            pingResult = kubernetesClient.executeCommandOnContainer(new String[]{"ping", "orgb.example.com", "-c", "3"}, podName, containerName, 5000);
            System.out.println(pingResult);
            pingResult = kubernetesClient.executeCommandOnContainer(new String[]{"ping", "orgc.example.com", "-c", "3"}, podName, containerName, 5000);
            System.out.println(pingResult);
            pingResult = kubernetesClient.executeCommandOnContainer(new String[]{"ping", "orgd.example.com", "-c", "3"}, podName, containerName, 5000);
            System.out.println(pingResult);

            File tempBusyboxYaml = MyFileUtils.createTempFile("yaml");
            kubernetesClient.uploadToContainer(busyboxYaml, "/tmp/busybox.yaml", podName, containerName);
            kubernetesClient.downloadFileFromContainer("/tmp/busybox.yaml", tempBusyboxYaml, podName, containerName);
            Assertions.assertEquals(
                    FileUtils.readFileToString(tempBusyboxYaml, StandardCharsets.UTF_8).length(),
                    FileUtils.readFileToString(busyboxYaml, StandardCharsets.UTF_8).length()
            );

            File example = new File("example");
            kubernetesClient.uploadToContainer(example, "/tmp/example", podName, containerName);
            String lsResult = kubernetesClient.executeCommandOnContainer(new String[]{"ls", "/tmp/example"}, podName, containerName, 2000);
            System.out.println(lsResult);
        } finally {
            kubernetesClient.deleteYaml(busyboxYaml);
        }

    }
}