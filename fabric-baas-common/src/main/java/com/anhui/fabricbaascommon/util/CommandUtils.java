package com.anhui.fabricbaascommon.util;


import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CommandUtils {
    public static String exec(String... cmd) throws IOException, InterruptedException {
        log.info("执行命令：" + String.join(" ", cmd));
        StringBuilder builder = new StringBuilder();
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        String str = builder.toString();
        log.info("命令输出：\n" + str);
        return str;
    }
}
