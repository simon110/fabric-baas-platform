package com.anhui.fabricbaascommon.service;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.function.ThrowableSupplier;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.WatcherUtils;
import com.anhui.fabricbaascommon.util.YamlUtils;
import com.spotify.docker.client.exceptions.DockerException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CaServerService {
    private final static File FABRIC_CA_SERVER_CONFIG_TEMPLATE = new File(MyFileUtils.getWorkingDir() + "/fabric/template/fabric-ca-server-config.yaml");
    private final static File FABRIC_CA_SERVER_CONFIG = new File(MyFileUtils.getWorkingDir() + "/fabric/caserver/fabric-ca-server-config.yaml");
    private final static File FABRIC_CA_SERVER_TLSCERT = new File(MyFileUtils.getWorkingDir() + "/fabric/caserver/tls-cert.pem");
    private Process caServerProcess;

    @Autowired
    private FabricConfiguration fabricConfig;
    @Autowired
    private CaRepo caRepo;

    @SneakyThrows
    public CaServerService() {
        autoStartCaServer();
    }

    private void autoStartCaServer() throws IOException {
        Optional<CaEntity> caOptional = caRepo.findFirstByOrganizationNameIsNotNull();
        if (caOptional.isPresent()) {
            CaEntity ca = caOptional.get();
            CsrConfig csrConfig = CaUtils.buildCsrConfig(ca);
            startCaServer(csrConfig);
        }
    }

    /**
     * @return 当前服务器是否存在已经启动的CA服务容器
     */
    public boolean checkCaServer() {
        return caServerProcess != null && caServerProcess.isAlive();
    }

    public void stopCaServer() throws InterruptedException {
        if (checkCaServer()) {
            caServerProcess.destroy();
            // Process.destroy方法返回后进程并不一定马上结束，需要调用Process.waitFor函数进行等待
            caServerProcess.waitFor();
        }
    }

    public void cleanCaServer() throws IOException, InterruptedException {
        stopCaServer();
        File caServerDir = new File(MyFileUtils.getWorkingDir() + "/fabric/caserver");
        FileUtils.deleteDirectory(caServerDir);
        boolean mkdirs = caServerDir.mkdirs();
    }

    @SuppressWarnings("unchecked")
    private void initFabricCaConfigYaml(CsrConfig csrConfig) throws IOException {
        Map<String, Object> caServerConfigYaml = YamlUtils.load(FABRIC_CA_SERVER_CONFIG_TEMPLATE);
        Map<String, Object> caOption = (Map<String, Object>) caServerConfigYaml.get("ca");
        caOption.put("name", csrConfig.getCaName());
        Map<String, Object> registryOption = (Map<String, Object>) caServerConfigYaml.get("registry");
        Map<String, Object> identitiesOption = ((List<Map<String, Object>>) registryOption.get("identities")).get(0);
        identitiesOption.put("name", fabricConfig.getRootCaUsername());
        identitiesOption.put("pass", fabricConfig.getRootCaPassword());
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

    public void startCaServer(CsrConfig csrConfig) throws IOException {
        // 运行Fabric CA Server容器（如果已经启动则更新）
        Map<String, String> envs = new HashMap<>();
        envs.put("FABRIC_CA_HOME", MyFileUtils.getWorkingDir() + "/fabric/caserver");
        envs.put("FABRIC_CA_SERVER_CA_NAME", csrConfig.getCaName());
        envs.put("FABRIC_CA_SERVER_TLS_ENABLED", "true");
        envs.put("FABRIC_CA_SERVER_PORT", "7054");
        log.info("生成用户环境变量：" + envs);
        String bootOption = fabricConfig.getRootCaUsername() + ':' + fabricConfig.getRootCaPassword();
        caServerProcess = CommandUtils.asyncExec(envs, "fabric-ca-server", "start", "-b", bootOption, "-d");
    }

    /**
     * 根据传入的CAServer在当前机器上部署一个CA服务容器
     *
     * @param csrConfig 部署的CA服务的参数
     */
    public void initCaServer(CsrConfig csrConfig) throws IOException, InterruptedException, DockerException {
        // 修改Fabric CA Server配置文件
        initFabricCaConfigYaml(csrConfig);
        startCaServer(csrConfig);
        // 检查是否启动成功（通过判断tls-cert.pem是否生成）
        try {
            ThrowableSupplier<Boolean, Exception> supplier = FABRIC_CA_SERVER_TLSCERT::exists;
            WatcherUtils.waitFor(supplier, 100, 2000);
        } catch (Exception e) {
            log.info("CA服务启动失败，即将清除容器");
            cleanCaServer();
            throw new DockerException("CA服务容器启动失败");
        }
    }
}
