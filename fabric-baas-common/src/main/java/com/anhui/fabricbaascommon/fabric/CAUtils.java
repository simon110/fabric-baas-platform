package com.anhui.fabricbaascommon.fabric;


import com.anhui.fabricbaascommon.bean.CSRConfig;
import com.anhui.fabricbaascommon.bean.Certfile;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CAUtils {
    public static void register(
            File adminCertfileDir,
            File caTlsCert,
            String caName,
            Certfile certfile)
            throws IOException, InterruptedException, CertfileException, CAException {
        assert !StringUtils.isBlank(certfile.getCaUsername());
        assert !StringUtils.isBlank(certfile.getCaPassword());
        assert CertfileType.exists(certfile.getCaUsertype());

        ResourceUtils.assertFileExists(caTlsCert);
        CertfileUtils.assertCertfile(adminCertfileDir);
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-ca-register.sh",
                adminCertfileDir.getCanonicalPath(),
                caTlsCert.getCanonicalPath(), caName,
                certfile.getCaUsername(),
                certfile.getCaPassword(),
                certfile.getCaUsertype());
        if (!str.contains("Password: ") && !str.contains("is already registered")) {
            throw new CAException("注册证书失败");
        }
    }

    public static void enroll(
            File certfileDir,
            File caTlsCert,
            String caName,
            String caAddr,
            Certfile certfile,
            List<String> csrHosts)
            throws IOException, InterruptedException, CAException {
        assert !StringUtils.isBlank(certfile.getCaUsername());
        assert !StringUtils.isBlank(certfile.getCaPassword());
        assert csrHosts.size() >= 2;

        ResourceUtils.assertFileExists(caTlsCert);
        CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-ca-enroll.sh",
                certfileDir.getCanonicalPath(),
                caName, caAddr,
                caTlsCert.getCanonicalPath(),
                certfile.getCaUsername(),
                certfile.getCaPassword(),
                String.join(",", csrHosts)
        );
        if (!CertfileUtils.checkCertfile(certfileDir)) {
            FileUtils.deleteDirectory(certfileDir);
            throw new CAException("登记证书失败");
        }
    }

    public static CSRConfig buildCsrConfig(CAEntity ca) {
        CSRConfig CSRConfig = new CSRConfig();
        CSRConfig.setCaName(ca.getOrganizationName() + "CA");
        CSRConfig.setCsrCommonName("ca." + ca.getDomain());
        CSRConfig.setCsrOrganizationName(ca.getOrganizationName());
        CSRConfig.setCsrOrganizationUnit("");
        CSRConfig.setCsrCountryCode(ca.getCountryCode());
        CSRConfig.setCsrStateOrProvince(ca.getStateOrProvince());
        CSRConfig.setCsrLocality(ca.getLocality());
        CSRConfig.setCsrHosts(Arrays.asList("localhost", ca.getDomain()));
        return CSRConfig;
    }
}

