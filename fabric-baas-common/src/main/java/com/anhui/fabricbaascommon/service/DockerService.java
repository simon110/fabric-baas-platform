package com.anhui.fabricbaascommon.service;

import com.anhui.fabricbaascommon.bean.CAInfo;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaascommon.util.YamlUtils;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DockerService {
    private final static String FABRIC_CA_SERVER_DOCKER_COMPOSE = ResourceUtils.getWorkingDir() + "/docker/docker-compose-fabric-ca.yaml";
    private final static String FABRIC_CA_SERVER_CONFIG = ResourceUtils.getWorkingDir() + "/docker/fabric-ca/fabric-ca-server-config.yaml";

    @Autowired
    private DockerClient dockerClient;

    private static boolean waitForFile(File file, int maxCheckCount, int checkIntervalMs) throws InterruptedException {
        for (int i = 0; i < maxCheckCount; i++) {
            if (file.exists()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(checkIntervalMs);
        }
        return false;
    }

    /**
     * @return 当前服务器是否存在已经启动的CA服务容器
     */
    public boolean checkCAServer() throws DockerException, InterruptedException {
        // List<Container> containers = dockerClient.listContainers();
        // 默认只能获取正在运行的容器
        // 以下代码可以获取所有容器
        List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containers) {
            String image = container.image();
            if (image.contains("hyperledger/fabric-ca") && container.portsAsString().contains("7054")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据传入的CAServer在当前机器上部署一个CA服务容器
     *
     * @param caInfo 部署的CA服务的参数
     */
    @SuppressWarnings("unchecked")
    public void startCAServer(CAInfo caInfo, String adminUsername, String adminPassword) throws IOException, InterruptedException, DockerException {
        File dockerComposeFile = new File(FABRIC_CA_SERVER_DOCKER_COMPOSE);
        File caConfigFile = new File(FABRIC_CA_SERVER_CONFIG);
        // 修改Docker Compose配置文件
        Map<String, Object> dockerComposeYaml = YamlUtils.load(dockerComposeFile);
        Map<String, Object> containers = (Map<String, Object>) dockerComposeYaml.get("services");
        Map<String, Object> container = (Map<String, Object>) containers.get("ca-server");
        List<String> environmentVars = (List<String>) container.get("environment");
        for (int i = 0; i < environmentVars.size(); i++) {
            String var = environmentVars.get(i);
            if (var.startsWith("FABRIC_CA_SERVER_CA_NAME=")) {
                environmentVars.set(i, "FABRIC_CA_SERVER_CA_NAME=" + caInfo.getCaName());
            }
        }
        String dockerCommand = String.format("sh -c 'fabric-ca-server start -b %s:%s -d'", adminUsername, adminPassword);
        container.put("command", dockerCommand);
        String dockerComposeContent = YamlUtils.save(dockerComposeYaml, dockerComposeFile);
        log.info("生成docker-compose-fabric-ca.yaml：\n" + dockerComposeContent);

        // 修改Fabric CA Server配置文件
        Map<String, Object> caServerConfigYaml = YamlUtils.load(caConfigFile);
        Map<String, Object> caOption = (Map<String, Object>) caServerConfigYaml.get("ca");
        caOption.put("name", caInfo.getCaName());
        Map<String, Object> registryOption = (Map<String, Object>) caServerConfigYaml.get("registry");
        Map<String, Object> identitiesOption = ((List<Map<String, Object>>) registryOption.get("identities")).get(0);
        identitiesOption.put("name", adminUsername);
        identitiesOption.put("pass", adminPassword);
        Map<String, Object> csrOption = (Map<String, Object>) caServerConfigYaml.get("csr");
        csrOption.put("cn", caInfo.getCsrCommonName());
        csrOption.put("hosts", caInfo.getCsrHosts());
        Map<String, Object> csrNameOption = ((List<Map<String, Object>>) csrOption.get("names")).get(0);
        csrNameOption.put("C", caInfo.getCsrCountryCode());
        csrNameOption.put("ST", caInfo.getCsrStateOrProvince());
        csrNameOption.put("OU", caInfo.getCsrOrganizationUnit());
        csrNameOption.put("L", caInfo.getCsrLocality());
        csrNameOption.put("O", caInfo.getCsrOrganizationName());
        String fabricCaConfigContent = YamlUtils.save(caServerConfigYaml, caConfigFile);
        log.info("生成fabric-ca-server-config.yaml：\n" + fabricCaConfigContent);

        // 运行Fabric CA Server容器（如果已经启动则更新）
        CommandUtils.exec("docker-compose", "-f", FABRIC_CA_SERVER_DOCKER_COMPOSE, "up", "-d");

        // 检查是否启动成功（通过判断tls-cert.pem是否生成）
        File tlsCert = new File("docker/fabric-ca/tls-cert.pem");
        if (!waitForFile(tlsCert, 10, 1000)) {
            // 如果启动失败则删除容器并抛出异常
            log.info("CA服务启动失败，即将清除容器");
            CommandUtils.exec("docker-compose", "-f", FABRIC_CA_SERVER_DOCKER_COMPOSE, "down");
            throw new DockerException("CA服务容器启动失败");
        }
    }
}
