package com.anhui.fabricbaascommon.fabric;

import com.anhui.fabricbaascommon.bean.CAConfig;
import com.anhui.fabricbaascommon.bean.Certfile;
import com.anhui.fabricbaascommon.configuration.DockerClientConfiguration;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.service.DockerService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
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
class CAUtilsTest {
    private static final String[] ORGANIZATION_NAMES = {"TestOrgA", "TestOrgB", "TestOrgC", "TestOrgD"};
    private static final String[] ORGANIZATION_DOMAINS = {"orga.example.com", "orgb.example.com", "orgc.example.com", "orgd.example.com"};
    private static final String[] ORGANIZATION_COUNTRY_CODES = {"CN", "US", "UK", "JP"};
    private static final String[] ORGANIZATION_STATES_OR_PROVINCES = {"Beijing", "NewYork", "London", "Tokyo"};
    private static final String[] ORGANIZATION_LOCALITIES = {"Chaoyang", "Queen", "Greenwich", "Chiyoda"};
    private static final String BASE_DIR_PATH = "example/certfile";

    private DockerService startDockerService() throws DockerException, InterruptedException {
        DockerClientConfiguration dockerClientConfiguration = new DockerClientConfiguration();
        DockerClient dockerClient = dockerClientConfiguration.dockerClient();
        return new DockerService(dockerClient);
    }

    @Test
    public void generateCertfiles() throws DockerException, InterruptedException, IOException, CAException, CertfileException {
        String adminUsername = "admin";
        String commonPassword = "OMX0LmIyXdt8CC9U";
        String address = "127.0.0.1:7054";
        int typeCertfileCount = 3;

        DockerService dockerService = startDockerService();
        for (int i = 0; i < ORGANIZATION_NAMES.length; i++) {
            List<String> csrHosts = Arrays.asList("localhost", ORGANIZATION_DOMAINS[i]);
            // 启动CA容器
            CAEntity ca = new CAEntity();
            ca.setOrganizationName(ORGANIZATION_NAMES[i]);
            ca.setCountryCode(ORGANIZATION_COUNTRY_CODES[i]);
            ca.setStateOrProvince(ORGANIZATION_STATES_OR_PROVINCES[i]);
            ca.setLocality(ORGANIZATION_LOCALITIES[i]);
            ca.setDomain(ORGANIZATION_DOMAINS[i]);
            CAConfig caConfig = CAUtils.buildCAConfig(ca);
            dockerService.startCAServer(caConfig, adminUsername, commonPassword);
            TimeUnit.SECONDS.sleep(15);
            File caTlsCert = new File("docker/fabric-ca/tls-cert.pem");
            Assertions.assertTrue(caTlsCert.exists());

            // 注册管理员证书
            File adminCertfileDir = ResourceUtils.createTempDir();
            Certfile adminCertfile = new Certfile(adminUsername, commonPassword, CertfileType.ADMIN);
            CAUtils.enroll(adminCertfileDir, caTlsCert, caConfig.getCaName(), address, adminCertfile, csrHosts);
            File adminCertfileZip = new File(String.format("%s/%s/root.zip", BASE_DIR_PATH, ca.getOrganizationName()));
            ZipUtils.zip(adminCertfileZip, CertfileUtils.getCertfileMSPDir(adminCertfileDir), CertfileUtils.getCertfileTLSDir(adminCertfileDir));
            Assertions.assertTrue(adminCertfileZip.exists());

            // 注册并登记四种类型的证书
            for (String type : CertfileType.ALL) {
                for (int j = 0; j < typeCertfileCount; j++) {
                    Certfile certfile = new Certfile();
                    certfile.setCaPassword(commonPassword);
                    certfile.setCaUsertype(type);
                    certfile.setCaUsername(String.format("%s-%s%d", ca.getOrganizationName(), type, j));
                    CAUtils.register(adminCertfileDir, caTlsCert, caConfig.getCaName(), certfile);

                    File certfileDir = ResourceUtils.createTempDir();
                    CAUtils.enroll(certfileDir, caTlsCert, caConfig.getCaName(), address, certfile, csrHosts);
                    File certfileZip = new File(String.format("%s/%s/%s%d.zip", BASE_DIR_PATH, ca.getOrganizationName(), type, j));
                    ZipUtils.zip(certfileZip, CertfileUtils.getCertfileMSPDir(certfileDir), CertfileUtils.getCertfileTLSDir(certfileDir));
                    Assertions.assertTrue(certfileZip.exists());
                    FileUtils.deleteDirectory(certfileDir);
                }
            }

            FileUtils.deleteDirectory(adminCertfileDir);
            dockerService.cleanCAServer();
        }
    }
}