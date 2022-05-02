package com.anhui.fabricbaascommon.fabric;


import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class CaUtils {
    public static void register(
            File adminCertfileDir,
            File caTlsCert,
            String caName,
            CertfileEntity certfile)
            throws IOException, InterruptedException, CertfileException, CaException {
        assert !StringUtils.isBlank(certfile.getCaUsername());
        assert !StringUtils.isBlank(certfile.getCaPassword());
        assert CertfileType.exists(certfile.getCaUsertype());

        MyFileUtils.assertFileExists(caTlsCert);
        CertfileUtils.assertCertfile(adminCertfileDir);
        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-ca-register.sh",
                adminCertfileDir.getCanonicalPath(),
                caTlsCert.getCanonicalPath(), caName,
                certfile.getCaUsername(),
                certfile.getCaPassword(),
                certfile.getCaUsertype());
        if (!str.contains("Password: ") && !str.contains("is already registered")) {
            throw new CaException("注册证书失败");
        }
    }

    public static void enroll(
            File certfileDir,
            File caTlsCert,
            String caName,
            String caAddr,
            CertfileEntity certfile,
            List<String> csrHosts)
            throws IOException, InterruptedException, CaException {
        assert !StringUtils.isBlank(certfile.getCaUsername());
        assert !StringUtils.isBlank(certfile.getCaPassword());
        assert csrHosts.size() >= 2;

        MyFileUtils.assertFileExists(caTlsCert);
        CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-ca-enroll.sh",
                certfileDir.getCanonicalPath(),
                caName, caAddr,
                caTlsCert.getCanonicalPath(),
                certfile.getCaUsername(),
                certfile.getCaPassword(),
                String.join(",", csrHosts)
        );
        if (!CertfileUtils.checkCertfile(certfileDir)) {
            FileUtils.deleteDirectory(certfileDir);
            throw new CaException("登记证书失败");
        }
    }

    public static CsrConfig buildCsrConfig(CaEntity ca) {
        CsrConfig CSRConfig = new CsrConfig();
        CSRConfig.setCaName(ca.getOrganizationName() + "CA");
        CSRConfig.setCsrCommonName("ca." + ca.getDomain());
        CSRConfig.setCsrOrganizationName(ca.getOrganizationName());
        CSRConfig.setCsrOrganizationUnit("");
        CSRConfig.setCsrCountryCode(ca.getCountryCode());
        CSRConfig.setCsrStateOrProvince(ca.getStateOrProvince());
        CSRConfig.setCsrLocality(ca.getLocality());
        CSRConfig.setCsrHosts(buildCsrHosts(ca.getDomain()));
        return CSRConfig;
    }

    public static List<String> buildCsrHosts(String... domains) {
        List<String> csrHosts = new ArrayList<>(domains.length + 2);
        csrHosts.add("localhost");
        csrHosts.add("127.0.0.1");
        Collections.addAll(csrHosts, domains);
        return csrHosts;
    }
}

