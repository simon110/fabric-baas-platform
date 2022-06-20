package com.anhui.fabricbaascommon.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class CommandUtils {
    public static String exec(String... cmd) throws IOException, InterruptedException {
        return exec(Collections.emptyMap(), cmd);
    }

    public static String exec(Map<String, String> envs, String... cmd) throws IOException, InterruptedException {
        log.info("执行命令：" + String.join(" ", cmd));
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> environment = builder.environment();
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            environment.put(entry.getKey(), entry.getValue());
        }
        builder.inheritIO();
        Process process = builder.start();
        process.waitFor();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        log.info("命令输出：\n" + output);
        return output;
    }

    public static Process asyncExec(String... cmd) throws IOException {
        return asyncExec(Collections.emptyMap(), cmd);
    }

    public static Process asyncExec(Map<String, String> envs, String... cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> environment = builder.environment();
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            environment.put(entry.getKey(), entry.getValue());
        }
        return builder.start();
    }
}
