package com.anhui.fabricbaascommon.service;

import com.anhui.fabricbaascommon.bean.CAConfig;
import com.anhui.fabricbaascommon.bean.Certfile;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CAUtils;
import com.anhui.fabricbaascommon.repository.CARepo;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CAService {
    private final static File FABRIC_CA_ADMIN_CERTFILE_DIR = new File(ResourceUtils.getWorkingDir() + "/fabric/admin");
    private final static File FABRIC_CA_SERVER_CERT = new File(ResourceUtils.getWorkingDir() + "/docker/fabric-ca/tls-cert.pem");
    private final static String FABRIC_CA_SERVER_ADDR = "localhost:7054";

    @Autowired
    private CertfileRepo certfileRepo;
    @Autowired
    private CARepo caRepo;
    @Autowired
    private FabricConfiguration fabricConfig;

    public void register(String username, String password, String usertype) throws CertfileException, CAException, IOException, InterruptedException {
        Optional<CertfileEntity> certfileOptional = certfileRepo.findById(username);
        if (certfileOptional.isPresent()) {
            throw new CertfileException("证书已注册，请勿重复操作");
        }
        if (!CertfileUtils.checkCerts(FABRIC_CA_ADMIN_CERTFILE_DIR)) {
            throw new CertfileException("CA管理员的证书未初始化");
        }

        log.info("正在注册证书...");
        Certfile certfile = new Certfile(username, password, usertype);

        CAUtils.register(FABRIC_CA_ADMIN_CERTFILE_DIR, FABRIC_CA_SERVER_CERT, getCAName(), certfile);

        CertfileEntity entity = new CertfileEntity(username, password, usertype);
        log.info("证书信息：" + entity);
        certfileRepo.save(entity);
    }

    public void enroll(File targetCertfileDir, String username, List<String> csrHosts) throws CertfileException, CAException, IOException, InterruptedException {
        Optional<CertfileEntity> certfileOptional = certfileRepo.findById(username);
        if (certfileOptional.isEmpty()) {
            throw new CertfileException("相应的证书未注册：" + username);
        }
        CertfileEntity entity = certfileOptional.get();
        log.info("正在登记证书...");
        Certfile certfile = new Certfile(entity.getCaUsername(), entity.getCaPassword(), entity.getCaUsertype());
        CAUtils.enroll(targetCertfileDir,
                FABRIC_CA_SERVER_CERT,
                getCAName(),
                FABRIC_CA_SERVER_ADDR,
                certfile, csrHosts);
    }

    public void initAdminCertfile(CAConfig caConfig) throws IOException, InterruptedException, CertfileException, CAException {
        enroll(FABRIC_CA_ADMIN_CERTFILE_DIR,
                fabricConfig.getCaAdminUsername(),
                caConfig.getCsrHosts());
        log.info("CA服务管理员证书初始化成功");
    }

    public File getAdminCertfileDir() {
        return FABRIC_CA_ADMIN_CERTFILE_DIR;
    }

    public String getAdminOrgName() throws CAException {
        Optional<CAEntity> caOptional = caRepo.findFirstByNameIsNotNull();
        if (caOptional.isPresent()) {
            return caOptional.get().getName();
        }
        throw new CAException("未找到CA信息，请确认系统已经初始化");
    }

    public String getCAName() throws CAException {
        return getAdminOrgName() + "CA";
    }

    public CAConfig buildCAConfig(CAEntity ca) {
        CAConfig caConfig = new CAConfig();
        caConfig.setCaName(ca.getName() + "CA");
        caConfig.setCsrCommonName("ca." + ca.getDomain());
        caConfig.setCsrOrganizationName(ca.getName());
        caConfig.setCsrOrganizationUnit("");
        caConfig.setCsrCountryCode(ca.getCountryCode());
        caConfig.setCsrStateOrProvince(ca.getStateOrProvince());
        caConfig.setCsrLocality(ca.getLocality());
        caConfig.setCsrHosts(Arrays.asList("localhost", ca.getDomain()));
        return caConfig;
    }
}
