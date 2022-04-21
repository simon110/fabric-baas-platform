package com.anhui.fabricbaascommon.util;

import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AESUtils {
    public static SecretKeySpec readSecretKey(File file) throws IOException {
        byte[] encoded = FileUtils.readFileToByteArray(file);
        return new SecretKeySpec(encoded, "AES");
    }

    public static void saveSecretKey(SecretKeySpec secretKeySpec, File file) throws IOException {
        byte[] bytes = secretKeySpec.getEncoded();
        FileUtils.writeByteArrayToFile(file, bytes);
    }

    public static SecretKeySpec generateSecretKey(String token) throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128, new SecureRandom(token.getBytes()));
        SecretKey secretKey = generator.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }

    public static byte[] encrypt(byte[] bytes, SecretKeySpec secretKeySpec) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(bytes);
    }

    public static byte[] decrypt(byte[] bytes, SecretKeySpec secretKeySpec) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(bytes);
    }
}
