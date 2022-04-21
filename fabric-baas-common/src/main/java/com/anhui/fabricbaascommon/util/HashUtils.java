package com.anhui.fabricbaascommon.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private final static MessageDigest MESSAGE_DIGEST;

    static {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            messageDigest = null;
            e.printStackTrace();
        }
        MESSAGE_DIGEST = messageDigest;
    }

    public static byte[] sha256(byte[] bytes) {
        return MESSAGE_DIGEST.digest(bytes);
    }
}

