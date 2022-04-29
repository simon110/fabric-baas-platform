package com.anhui.fabricbaascommon.util;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

public class EcdsaUtils {
    public static boolean verify(PublicKey publicKey, byte[] sig, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sig);
    }

    public static byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public static KeyPair generateKeyPair() throws Exception {
        ECGenParameterSpec spec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(spec, new SecureRandom());
        return generator.generateKeyPair();
    }
}