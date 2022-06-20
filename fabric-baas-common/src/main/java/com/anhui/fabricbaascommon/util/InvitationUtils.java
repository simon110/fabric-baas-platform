package com.anhui.fabricbaascommon.util;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.bean.Invitation;
import org.springframework.util.Base64Utils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class InvitationUtils {
    private static SecretKeySpec AES_SECRET_KEY_SPEC;

    static {
        try {
            // TODO: 在项目中添加token相关的配置
            String token = UUID.randomUUID().toString();
            AES_SECRET_KEY_SPEC = AesUtils.generateSecretKey(token);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String getCode(Invitation invitation) throws Exception {
        String str = String.join("---",
                invitation.getInvitorOrgName(),
                invitation.getInviteeOrgName(),
                invitation.getChannelName(),
                invitation.getTimestamp().toString());
        byte[] encryptedStr = AesUtils.encrypt(str.getBytes(StandardCharsets.UTF_8), AES_SECRET_KEY_SPEC);
        return Base64Utils.encodeToString(encryptedStr);
    }

    public static Invitation parseCode(String code) throws Exception {
        byte[] encryptedData = Base64Utils.decodeFromString(code);
        byte[] decryptedData = AesUtils.decrypt(encryptedData, AES_SECRET_KEY_SPEC);
        String str = new String(decryptedData, StandardCharsets.UTF_8);
        String[] properties = str.split("---");
        Assert.isTrue(properties.length == 4);

        Invitation invitation = new Invitation();
        invitation.setInvitorOrgName(properties[0]);
        invitation.setInviteeOrgName(properties[1]);
        invitation.setChannelName(properties[2]);
        invitation.setTimestamp(Long.parseLong(properties[3]));
        return invitation;
    }
}
