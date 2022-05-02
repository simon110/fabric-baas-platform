package com.anhui.fabricbaascommon.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class CommandUtils {
    public static String exec(String... cmd) throws IOException, InterruptedException {
        log.info("执行命令：" + String.join(" ", cmd));
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        log.info("命令输出：\n" + output);
        return output;
    }

    public static String exec(Map<String, String> envs, String... cmd) throws IOException, InterruptedException {
        log.info("执行命令：" + String.join(" ", cmd));
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> environment = builder.environment();
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            environment.put(entry.getKey(), entry.getValue());
        }
        Process process = builder.start();
        process.waitFor();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        log.info("命令输出：\n" + output);
        return output;
    }
}
