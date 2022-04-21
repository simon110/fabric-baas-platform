package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.CAInfo;
import com.anhui.fabricbaascommon.bean.Certfile;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CAUtils;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaasttp.configuration.FabricConfiguration;
import com.anhui.fabricbaasttp.entity.CertfileEntity;
import com.anhui.fabricbaasttp.entity.TTPEntity;
import com.anhui.fabricbaasttp.repository.CertfileRepo;
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

    @Autowired
    private FabricConfiguration fabricConfiguration;
    @Autowired
    private CertfileRepo certfileRepo;

    public CAInfo generateCAInfo(TTPEntity ttp) {
        CAInfo caInfo = new CAInfo();
        caInfo.setCaName(fabricConfiguration.getCaName());
        caInfo.setCsrCommonName("ca." + ttp.getDomain());
        caInfo.setCsrOrganizationName(ttp.getName());
        caInfo.setCsrOrganizationUnit("");
        caInfo.setCsrCountryCode(ttp.getCountryCode());
        caInfo.setCsrStateOrProvince(ttp.getStateOrProvince());
        caInfo.setCsrLocality(ttp.getLocality());
        caInfo.setCsrHosts(Arrays.asList("localhost,", ttp.getDomain()));
        return caInfo;
    }

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
        CAUtils.register(FABRIC_CA_ADMIN_CERTFILE_DIR, FABRIC_CA_SERVER_CERT, fabricConfiguration.getCaName(), certfile);

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
        CAUtils.enroll(targetCertfileDir, FABRIC_CA_SERVER_CERT, fabricConfiguration.getCaName(),
                fabricConfiguration.getCaAddr(), certfile, csrHosts);
    }

    public void initAdminCertfile(CAInfo caInfo) throws IOException, InterruptedException, CertfileException, CAException {
        enroll(FABRIC_CA_ADMIN_CERTFILE_DIR,
                fabricConfiguration.getCaAdminUsername(),
                caInfo.getCsrHosts());
        log.info("CA服务管理员证书初始化成功");
    }
}
