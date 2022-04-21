package com.anhui.fabricbaascommon.util;

import com.anhui.fabricbaascommon.exception.CertfileException;

import java.io.File;

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
    public static boolean checkCerts(File dir) {
        return checkMSPDir(new File(dir + "/msp")) && checkTLSDir(new File(dir + "/tls"));
    }

    public static void assertCerts(File dir) throws CertfileException {
        if (!checkCerts(dir)) {
            throw new CertfileException("证书格式不正确");
        }
    }

    public static File getMSPDir(File dir) {
        return new File(dir + "/msp");
    }

    public static File getTLSDir(File dir) {
        return new File(dir + "/tls");
    }
}
