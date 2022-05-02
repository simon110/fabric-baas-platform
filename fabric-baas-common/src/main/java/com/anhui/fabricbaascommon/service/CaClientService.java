package com.anhui.fabricbaascommon.service;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CaClientService {
    private final static File FABRIC_CA_ROOT_CERTFILE_DIR = new File(MyFileUtils.getWorkingDir() + "/fabric/root");
    private final static File FABRIC_CA_SERVER_CERT = new File(MyFileUtils.getWorkingDir() + "/docker/fabric-ca/tls-cert.pem");

    @Autowired
    private CertfileRepo certfileRepo;
    @Autowired
    private CaRepo caRepo;
    @Autowired
    private FabricConfiguration fabricConfig;


    public void register(String username, String password, String usertype) throws CertfileException, CaException, IOException, InterruptedException {
        if (certfileRepo.existsById(username)) {
            throw new CertfileException("证书已注册，请勿重复操作");
        }
        assertRootCertfile();

        CertfileEntity certfile = new CertfileEntity(username, password, usertype);
        CaUtils.register(FABRIC_CA_ROOT_CERTFILE_DIR, FABRIC_CA_SERVER_CERT, getCaName(), certfile);
        certfileRepo.save(certfile);
    }

    public void enroll(File targetCertfileDir, String username, List<String> csrHosts) throws CertfileException, CaException, IOException, InterruptedException {
        CertfileEntity certfile = findCertfileOrThrowEx(username);
        String caServerAddr = "localhost:" + fabricConfig.getCaServerPort();
        CaUtils.enroll(targetCertfileDir, FABRIC_CA_SERVER_CERT, getCaName(), caServerAddr, certfile, csrHosts);
    }


    public CertfileEntity findCertfileOrThrowEx(String username) throws CertfileException {
        Optional<CertfileEntity> certfileOptional = certfileRepo.findById(username);
        if (certfileOptional.isEmpty()) {
            throw new CertfileException("相应的证书未注册：" + username);
        }
        return certfileOptional.get();
    }

    public CaEntity findCaEntityOrThrowEx() throws CaException {
        Optional<CaEntity> caOptional = caRepo.findFirstByOrganizationNameIsNotNull();
        if (caOptional.isPresent()) {
            return caOptional.get();
        }
        throw new CaException("未找到CA信息，请确认系统已经初始化");
    }

    public String getCaOrganizationName() throws CaException {
        return findCaEntityOrThrowEx().getOrganizationName();
    }

    public String getCaOrganizationDomain() throws CaException {
        return findCaEntityOrThrowEx().getDomain();
    }

    public String getCaName() throws CaException {
        return getCaOrganizationName() + "CA";
    }
    
    public void initRootCertfile(CsrConfig csrConfig) throws IOException, InterruptedException, CaException {
        boolean mkdirs = FABRIC_CA_ROOT_CERTFILE_DIR.mkdirs();
        CertfileEntity rootCertfile = new CertfileEntity();
        rootCertfile.setCaUsername(fabricConfig.getRootCaUsername());
        rootCertfile.setCaPassword(fabricConfig.getRootCaPassword());
        rootCertfile.setCaUsertype(CertfileType.ADMIN);
        String caServerAddr = "localhost:" + fabricConfig.getCaServerPort();
        CaUtils.enroll(FABRIC_CA_ROOT_CERTFILE_DIR, FABRIC_CA_SERVER_CERT, csrConfig.getCaName(), caServerAddr, rootCertfile, csrConfig.getCsrHosts());
        log.info("CA服务管理员证书初始化成功");
    }

    public void assertRootCertfile() throws CertfileException {
        if (!CertfileUtils.checkCertfile(FABRIC_CA_ROOT_CERTFILE_DIR)) {
            throw new CertfileException("CA管理员的证书未初始化");
        }
    }

    public File getRootCertfileDir() {
        return FABRIC_CA_ROOT_CERTFILE_DIR;
    }

    public void getRootCertfileZip(File output) throws IOException, CertfileException {
        CertfileUtils.assertCertfile(FABRIC_CA_ROOT_CERTFILE_DIR);
        ZipUtils.zip(output,
                CertfileUtils.getMspDir(FABRIC_CA_ROOT_CERTFILE_DIR),
                CertfileUtils.getTlsDir(FABRIC_CA_ROOT_CERTFILE_DIR)
        );
    }
}
