package com.anhui.fabricbaascommon.fabric;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
public class CertfileUtils {
    public static boolean checkMSPDir(File mspDir) {
        return new File(mspDir + "/keystore/key.pem").exists() &&
                new File(mspDir + "/signcerts/cert.pem").exists() &&
                new File(mspDir + "/cacerts/ca.pem").exists();
    }

    public static boolean checkTLSDir(File tlsDir) {
        return new File(tlsDir + "/ca.crt").exists() &&
                new File(tlsDir + "/server.crt").exists() &&
                new File(tlsDir + "/server.key").exists();
    }

    /**
     * @param dir 该目录必须存在且包含msp和tls文件夹
     * @return 目录中是否包含关键的证书文件
     */
    public static boolean checkCertfile(File dir) {
        return checkMSPDir(new File(dir + "/msp")) && checkTLSDir(new File(dir + "/tls"));
    }

    public static void packageCertfile(File dir, File output) throws CertfileException, IOException {
        assertCertfile(dir);
        ZipUtils.zip(output, getMspDir(dir), getTlsDir(dir));
    }

    public static void assertCertfile(File dir) throws CertfileException {
        if (!checkCertfile(dir)) {
            throw new CertfileException("证书格式不正确");
        }
    }

    public static void arrangeRawCertfile(File dir) throws IOException {
        File mspDir = getMspDir(dir);
        File tlsDir = getTlsDir(dir);
        Assert.isTrue(mspDir.isDirectory());
        Assert.isTrue(tlsDir.isDirectory());
        FileUtils.copyDirectoryToDirectory(new File(dir + "/tls/tlscacerts"), mspDir);

        // 重命名MSP秘钥和证书文件
        File[] caCerts = new File(mspDir + "/cacerts").listFiles();
        assert caCerts != null && caCerts.length == 1;
        File targetCaCert = new File(mspDir + "/cacerts/ca.pem");
        if (!targetCaCert.exists()) {
            FileUtils.moveFile(caCerts[0], targetCaCert);
        }

        File[] keystore = new File(mspDir + "/keystore").listFiles();
        assert keystore != null && keystore.length == 1;
        File targetKeystore = new File(mspDir + "/keystore/key.pem");
        if (!targetKeystore.exists()) {
            FileUtils.moveFile(keystore[0], targetKeystore);
        }

        File[] signCerts = new File(mspDir + "/signcerts").listFiles();
        assert signCerts != null && signCerts.length == 1;
        File targetSignCert = new File(mspDir + "/signcerts/cert.pem");
        if (!targetSignCert.exists()) {
            FileUtils.moveFile(signCerts[0], targetSignCert);
        }

        // 复制TLS秘钥和证书文件
        File[] tlsCaCerts = new File(tlsDir + "/tlscacerts").listFiles();
        assert tlsCaCerts != null && tlsCaCerts.length == 1;
        File targetTlsCaCert = new File(tlsDir + "/ca.crt");
        if (!targetTlsCaCert.exists()) {
            FileUtils.copyFile(tlsCaCerts[0], targetTlsCaCert);
        }

        File[] tlsKeystore = new File(tlsDir + "/keystore").listFiles();
        assert tlsKeystore != null && tlsKeystore.length == 1;
        File targetTlsKeystore = new File(tlsDir + "/server.key");
        if (!targetTlsKeystore.exists()) {
            FileUtils.copyFile(tlsKeystore[0], targetTlsKeystore);
        }

        File[] tlsSignCerts = new File(tlsDir + "/signcerts").listFiles();
        assert tlsSignCerts != null && tlsSignCerts.length == 1;
        File targetTlsSignCert = new File(tlsDir + "/server.crt");
        if (!targetTlsSignCert.exists()) {
            FileUtils.copyFile(tlsSignCerts[0], targetTlsSignCert);
        }

        // 生成MSP配置文件
        File mspConfigTemplate = new File(MyFileUtils.getWorkingDir() + "/fabric/template/fabric-ca-msp-config.yaml");
        File mspConfig = new File(mspDir + "/config.yaml");
        Assert.isTrue(mspConfigTemplate.exists());
        Assert.isFalse(mspConfig.exists());
        FileUtils.copyFile(mspConfigTemplate, mspConfig);
    }

    /**
     * 检查完成后返回压缩包的随机路径，如果检查不通过则抛出异常。
     */
    public static void assertCertfileZip(MultipartFile zip) throws IOException, CertfileException {
        // 创建临时文件
        File tempDir = MyFileUtils.createTempDir();
        File certfileZip = MyFileUtils.createTempFile("zip");

        // 将文件写入临时目录并解压
        log.info("正在将证书文件写入：" + certfileZip.getAbsoluteFile());
        FileUtils.writeByteArrayToFile(certfileZip, zip.getBytes());

        // 检查清除写入的文件
        try {
            log.info("正在将证书文件解压到：" + tempDir.getAbsoluteFile());
            ZipUtils.unzip(certfileZip, tempDir);
            log.info("正在检查证书文件：" + tempDir.getAbsoluteFile());
            CertfileUtils.assertCertfile(tempDir);
        } finally {
            FileUtils.deleteQuietly(certfileZip);
            FileUtils.deleteDirectory(tempDir);
        }
    }

    /**
     * 注意该方法不检查对应路径的内容是否存在以及是否正确
     *
     * @param name CA账户名称
     * @param type CA账户类型
     * @return CA账户证书应该被存放的位置
     */
    public static File getCertfileDir(String name, String type) {
        Assert.isTrue(type.equals(CertfileType.ORDERER) ||
                type.equals(CertfileType.ADMIN) ||
                type.equals(CertfileType.CLIENT) ||
                type.equals(CertfileType.PEER));
        return new File(String.format("%s/fabric/certfile/%s/%s", MyFileUtils.getWorkingDir(), type, name));
    }

    public static File getMspDir(File certfileDir) {
        return new File(certfileDir + "/msp");
    }

    public static File getTlsDir(File certfileDir) {
        return new File(certfileDir + "/tls");
    }

    public static File getTlsCaCert(File certfileDir) {
        return new File(certfileDir + "/tls/ca.crt");
    }

    public static File getTlsServerCert(File certfileDir) {
        return new File(certfileDir + "/tls/server.crt");
    }
}
