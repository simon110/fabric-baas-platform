package com.anhui.fabricbaascommon.service;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.function.ThrowableSupplier;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.WatcherUtils;
import com.anhui.fabricbaascommon.util.YamlUtils;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CaContainerService {
    private final static File FABRIC_CA_DOCKER_COMPOSE = new File(MyFileUtils.getWorkingDir() + "/docker/docker-compose-fabric-ca.yaml");
    private final static File FABRIC_CA_SERVER_CONFIG = new File(MyFileUtils.getWorkingDir() + "/docker/fabric-ca/fabric-ca-server-config.yaml");
    private final static File FABRIC_CA_SERVER_TLSCERT = new File(MyFileUtils.getWorkingDir() + "/docker/fabric-ca/tls-cert.pem");

    private final DockerClient dockerClient;

    public CaContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * @return 当前服务器是否存在已经启动的CA服务容器
     */
    public boolean checkCaContainer() throws DockerException, InterruptedException {
        // List<Container> containers = dockerClient.listContainers();
        // 默认只能获取正在运行的容器
        // 以下代码可以获取所有容器
        List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        boolean result = false;
        for (int i = 0; i < containers.size() && !result; i++) {
            Container container = containers.get(i);
            String image = container.image();
            result = image.contains("hyperledger/fabric-ca") && container.portsAsString().contains("7054");
        }
        return result;
    }

    public void cleanCaContainer() throws IOException, InterruptedException {
        CommandUtils.exec("docker-compose", "-f", FABRIC_CA_DOCKER_COMPOSE.getAbsolutePath(), "down");
        String fabricCaServerConfig = FileUtils.readFileToString(FABRIC_CA_SERVER_CONFIG, StandardCharsets.UTF_8);

        try {
            File containerVolume = FABRIC_CA_SERVER_CONFIG.getParentFile();
            assert containerVolume.isDirectory();
            FileUtils.deleteDirectory(containerVolume);
            boolean mkdirs = containerVolume.mkdirs();
        } finally {
            FileUtils.writeStringToFile(FABRIC_CA_SERVER_CONFIG, fabricCaServerConfig, StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private void initDockerComposeYaml(CsrConfig csrConfig, String adminUsername, String adminPassword) throws IOException {
        Map<String, Object> dockerComposeYaml = YamlUtils.load(FABRIC_CA_DOCKER_COMPOSE);
        System.out.println(dockerComposeYaml);
        Map<String, Object> containers = (Map<String, Object>) dockerComposeYaml.get("services");
        Map<String, Object> container = (Map<String, Object>) containers.get("fabric-baas-platform-ca");
        List<String> environmentVars = (List<String>) container.get("environment");
        for (int i = 0; i < environmentVars.size(); i++) {
            String var = environmentVars.get(i);
            if (var.startsWith("FABRIC_CA_SERVER_CA_NAME=")) {
                environmentVars.set(i, "FABRIC_CA_SERVER_CA_NAME=" + csrConfig.getCaName());
            }
        }
        String dockerCommand = String.format("sh -c 'fabric-ca-server start -b %s:%s -d'", adminUsername, adminPassword);
        container.put("command", dockerCommand);
        String dockerComposeContent = YamlUtils.save(dockerComposeYaml, FABRIC_CA_DOCKER_COMPOSE);
        System.out.println(dockerComposeContent);
    }

    @SuppressWarnings("unchecked")
    private void initFabricCaConfigYaml(CsrConfig csrConfig, String adminUsername, String adminPassword) throws IOException {
        Map<String, Object> caServerConfigYaml = YamlUtils.load(FABRIC_CA_SERVER_CONFIG);
        Map<String, Object> caOption = (Map<String, Object>) caServerConfigYaml.get("ca");
        caOption.put("name", csrConfig.getCaName());
        Map<String, Object> registryOption = (Map<String, Object>) caServerConfigYaml.get("registry");
        Map<String, Object> identitiesOption = ((List<Map<String, Object>>) registryOption.get("identities")).get(0);
        identitiesOption.put("name", adminUsername);
        identitiesOption.put("pass", adminPassword);
        Map<String, Object> csrOption = (Map<String, Object>) caServerConfigYaml.get("csr");
        csrOption.put("cn", csrConfig.getCsrCommonName());
        csrOption.put("hosts", csrConfig.getCsrHosts());
        Map<String, Object> csrNameOption = ((List<Map<String, Object>>) csrOption.get("names")).get(0);
        csrNameOption.put("C", csrConfig.getCsrCountryCode());
        csrNameOption.put("ST", csrConfig.getCsrStateOrProvince());
        csrNameOption.put("OU", csrConfig.getCsrOrganizationUnit());
        csrNameOption.put("L", csrConfig.getCsrLocality());
        csrNameOption.put("O", csrConfig.getCsrOrganizationName());
        String fabricCaConfigContent = YamlUtils.save(caServerConfigYaml, FABRIC_CA_SERVER_CONFIG);
        System.out.println(fabricCaConfigContent);
    }

    /**
     * 根据传入的CAServer在当前机器上部署一个CA服务容器
     *
     * @param csrConfig 部署的CA服务的参数
     */
    public void startCaContainer(CsrConfig csrConfig, String adminUsername, String adminPassword) throws IOException, InterruptedException, DockerException {
        // 修改Docker Compose配置文件
        initDockerComposeYaml(csrConfig, adminUsername, adminPassword);
        // 修改Fabric CA Server配置文件
        initFabricCaConfigYaml(csrConfig, adminUsername, adminPassword);
        // 运行Fabric CA Server容器（如果已经启动则更新）
        Map<String, String> envs = new HashMap<>();
        envs.put("UID", CommandUtils.exec("id", "-u").strip());
        envs.put("GID", CommandUtils.exec("id", "-g").strip());
        log.info("生成用户环境变量：" + envs);
        CommandUtils.exec(envs, "docker-compose", "-f", FABRIC_CA_DOCKER_COMPOSE.getAbsolutePath(), "up", "-d");

        // 检查是否启动成功（通过判断tls-cert.pem是否生成）
        try {
            ThrowableSupplier<Boolean, Exception> supplier = FABRIC_CA_SERVER_TLSCERT::exists;
            WatcherUtils.waitFor(supplier, 100, 2000);
        } catch (Exception e) {
            log.info("CA服务启动失败，即将清除容器");
            cleanCaContainer();
            throw new DockerException("CA服务容器启动失败");
        }
    }
}
