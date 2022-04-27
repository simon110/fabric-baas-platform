package com.anhui.fabricbaasorg.kubernetes;

import com.anhui.fabricbaasorg.exception.KubernetesException;
import io.kubernetes.client.openapi.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
class KubernetesClientTest {

    @Test
    public void deployBusybox() throws IOException, KubernetesException, ApiException, InterruptedException {
        File adminConfig = new File("example/kubernetes/admin.conf");
        KubernetesClient kubernetesClient = new KubernetesClient(adminConfig);

        File ordererYaml = new File("example/kubernetes/busybox.yaml");

        kubernetesClient.connect();
        System.out.println(kubernetesClient.getAllNodes());
        System.out.println(kubernetesClient.getAllPods());

        kubernetesClient.applyYaml(ordererYaml);
        TimeUnit.SECONDS.sleep(10);
        System.out.println(kubernetesClient.getPodsByKeyword("busybox"));
        // kubernetesClient.uploadToContainer(new File("/home/ubuntu/Kubernetes/TestOrgA/orderer0/msp"),"/var/crypto-config",);
        kubernetesClient.deleteYaml(ordererYaml);
    }
}