package com.anhui.fabricbaascommon.util;

import java.util.UUID;

public class PasswordUtils {
    public static String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
