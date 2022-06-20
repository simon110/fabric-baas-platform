package com.anhui.fabricbaascommon.fabric;


import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class CaUtils {
    public static void register(
            File adminCertfileDir,
            File caTlsCert,
            String caName,
            CertfileEntity certfile)
            throws IOException, InterruptedException, CertfileException, CaException {
        Assert.notBlank(certfile.getCaUsername());
        Assert.notBlank(certfile.getCaPassword());
        Assert.isTrue(CertfileType.exists(certfile.getCaUsertype()));

        MyFileUtils.assertFileExists(caTlsCert);
        CertfileUtils.assertCertfile(adminCertfileDir);

        HashMap<String, String> envs = new HashMap<>();
        envs.put("FABRIC_CA_CLIENT_HOME", adminCertfileDir.getCanonicalPath());
        String str = CommandUtils.exec(envs, "fabric-ca-client", "register",
                "--caname", caName,
                "--id.name", certfile.getCaUsername(),
                "--id.secret", certfile.getCaPassword(),
                "--id.type", certfile.getCaUsertype(),
                "--tls.certfiles", caTlsCert.getCanonicalPath()
        );
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
        Assert.notBlank(certfile.getCaUsername());
        Assert.notBlank(certfile.getCaPassword());
        Assert.isTrue(csrHosts.size() >= 2);

        // 如果目录已存在则删除并重新创建
        if (certfileDir.exists()) {
            Assert.isTrue(certfileDir.isDirectory());
            FileUtils.deleteDirectory(certfileDir);
        }
        boolean mkdirs = certfileDir.mkdirs();
        Assert.isTrue(mkdirs);

        // 分别注册MSP和TLS证书
        MyFileUtils.assertFileExists(caTlsCert);
        String caServerUrl = String.format("https://%s:%s@%s", certfile.getCaUsername(), certfile.getCaPassword(), caAddr);
        String csrHostStr = String.join(",", csrHosts);
        HashMap<String, String> envs = new HashMap<>();
        envs.put("FABRIC_CA_CLIENT_HOME", certfileDir.getCanonicalPath());
        CommandUtils.exec(envs, "fabric-ca-client", "enroll",
                "-u", caServerUrl,
                "--caname", caName,
                "--mspdir", CertfileUtils.getMspDir(certfileDir).getCanonicalPath(),
                "--csr.hosts", csrHostStr,
                "--tls.certfiles", caTlsCert.getCanonicalPath()
        );
        CommandUtils.exec(envs, "fabric-ca-client", "enroll",
                "-u", caServerUrl,
                "--caname", caName,
                "--mspdir", CertfileUtils.getTlsDir(certfileDir).getCanonicalPath(),
                "--csr.hosts", csrHostStr,
                "--tls.certfiles", caTlsCert.getCanonicalPath(),
                "--enrollment.profile", "tls"
        );

        // 对下载到本地的证书文件进行整理
        CertfileUtils.arrangeRawCertfile(certfileDir);
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

