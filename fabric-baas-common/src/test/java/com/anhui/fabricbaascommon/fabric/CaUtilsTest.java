package com.anhui.fabricbaascommon.fabric;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.DockerClientConfiguration;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.service.CaContainerService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.SimpleFileUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
class CaUtilsTest {
    private static final String[] ORGANIZATION_NAMES = {"TestOrgA", "TestOrgB", "TestOrgC", "TestOrgD"};
    private static final String[] ORGANIZATION_DOMAINS = {"orga.example.com", "orgb.example.com", "orgc.example.com", "orgd.example.com"};
    private static final String[] ORGANIZATION_COUNTRY_CODES = {"CN", "US", "UK", "JP"};
    private static final String[] ORGANIZATION_STATES_OR_PROVINCES = {"Beijing", "NewYork", "London", "Tokyo"};
    private static final String[] ORGANIZATION_LOCALITIES = {"Chaoyang", "Queen", "Greenwich", "Chiyoda"};
    private static final String BASE_DIR_PATH = "example/fabric";

    private CaContainerService startDockerService() throws DockerException, InterruptedException {
        DockerClientConfiguration dockerClientConfiguration = new DockerClientConfiguration();
        DockerClient dockerClient = dockerClientConfiguration.dockerClient();
        return new CaContainerService(dockerClient);
    }

    @Test
    public void generateCertfiles() throws DockerException, InterruptedException, IOException, CaException, CertfileException {
        String adminUsername = "admin";
        String commonPassword = "OMX0LmIyXdt8CC9U";
        String address = "localhost:7054";
        int typeCertfileCount = 3;

        CaContainerService caContainerService = startDockerService();
        for (int i = 0; i < ORGANIZATION_NAMES.length; i++) {
            List<String> csrHosts = Arrays.asList("localhost", ORGANIZATION_DOMAINS[i]);
            // 启动CA容器
            CaEntity ca = new CaEntity();
            ca.setOrganizationName(ORGANIZATION_NAMES[i]);
            ca.setCountryCode(ORGANIZATION_COUNTRY_CODES[i]);
            ca.setStateOrProvince(ORGANIZATION_STATES_OR_PROVINCES[i]);
            ca.setLocality(ORGANIZATION_LOCALITIES[i]);
            ca.setDomain(ORGANIZATION_DOMAINS[i]);
            CsrConfig CSRConfig = CaUtils.buildCsrConfig(ca);
            caContainerService.startCaContainer(CSRConfig, adminUsername, commonPassword);
            TimeUnit.SECONDS.sleep(15);
            File caTlsCert = new File("docker/fabric-ca/tls-cert.pem");
            Assertions.assertTrue(caTlsCert.exists());

            // 注册管理员证书
            File adminCertfileDir = SimpleFileUtils.createTempDir();
            CertfileEntity adminCertfile = new CertfileEntity(adminUsername, commonPassword, CertfileType.ADMIN);
            CaUtils.enroll(adminCertfileDir, caTlsCert, CSRConfig.getCaName(), address, adminCertfile, csrHosts);
            File adminCertfileZip = new File(String.format("%s/%s/root.zip", BASE_DIR_PATH, ca.getOrganizationName()));
            ZipUtils.zip(adminCertfileZip, CertfileUtils.getMspDir(adminCertfileDir), CertfileUtils.getTlsDir(adminCertfileDir));
            Assertions.assertTrue(adminCertfileZip.exists());

            // 注册并登记四种类型的证书
            for (String type : CertfileType.ALL) {
                for (int j = 0; j < typeCertfileCount; j++) {
                    CertfileEntity certfile = new CertfileEntity();
                    certfile.setCaPassword(commonPassword);
                    certfile.setCaUsertype(type);
                    certfile.setCaUsername(String.format("%s-%s%d", ca.getOrganizationName(), type, j));
                    CaUtils.register(adminCertfileDir, caTlsCert, CSRConfig.getCaName(), certfile);

                    File certfileDir = SimpleFileUtils.createTempDir();
                    CaUtils.enroll(certfileDir, caTlsCert, CSRConfig.getCaName(), address, certfile, csrHosts);
                    File certfileZip = new File(String.format("%s/%s/%s%d.zip", BASE_DIR_PATH, ca.getOrganizationName(), type, j));
                    ZipUtils.zip(certfileZip, CertfileUtils.getMspDir(certfileDir), CertfileUtils.getTlsDir(certfileDir));
                    Assertions.assertTrue(certfileZip.exists());
                    FileUtils.deleteDirectory(certfileDir);
                }
            }

            FileUtils.deleteDirectory(adminCertfileDir);
            caContainerService.cleanCaContainer();
        }
    }
}