package com.anhui.fabricbaascommon.util;

import com.anhui.fabricbaascommon.exception.CertfileException;
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

    public static void assertCertfile(File dir) throws CertfileException {
        if (!checkCertfile(dir)) {
            throw new CertfileException("证书格式不正确");
        }
    }

    public static File getMSPDir(File dir) {
        return new File(dir + "/msp");
    }

    public static File getTLSDir(File dir) {
        return new File(dir + "/tls");
    }

    /**
     * 检查完成后返回压缩包的随机路径，如果检查不通过则抛出异常。
     */
    public static void assertCertfileZip(MultipartFile zip) throws IOException, CertfileException {
        // 创建临时文件
        File tempDir = ResourceUtils.createTempDir();
        File certfileZip = ResourceUtils.createTempFile("zip");

        // 将文件写入临时目录并解压
        log.info("正在将证书文件写入：" + certfileZip.getAbsoluteFile());
        FileUtils.writeByteArrayToFile(certfileZip, zip.getBytes());
        log.info("正在将证书文件解压到：" + tempDir.getAbsoluteFile());
        ZipUtils.unzip(certfileZip, tempDir);
        log.info("正在检查证书文件：" + tempDir.getAbsoluteFile());

        // 检查清除写入的文件
        try {
            CertfileUtils.assertCertfile(tempDir);
        } finally {
            FileUtils.deleteQuietly(certfileZip);
            FileUtils.deleteDirectory(tempDir);
        }
    }
}
