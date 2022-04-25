package com.anhui.fabricbaasorg.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.Data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 包含集群的链接信息和证书信息
 */
@Data
public class KubernetesClient {
    private ApiClient client;

    public KubernetesClient(File adminConfig) throws IOException {
        FileReader fileReader = new FileReader(adminConfig);
        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(fileReader);
        this.client = ClientBuilder.kubeconfig(kubeConfig).build();
    }

    public void connect() {
        Configuration.setDefaultApiClient(client);
    }
}

