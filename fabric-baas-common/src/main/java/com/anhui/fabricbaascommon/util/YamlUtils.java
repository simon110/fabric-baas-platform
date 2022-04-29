package com.anhui.fabricbaascommon.util;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class YamlUtils {
    private static final Yaml YAML;

    static {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        YAML = new Yaml(dumperOptions);
    }

    /**
     * @param file 要读取的Yaml文件路径
     * @return 读取为Map形式的Yaml文件
     * @throws IOException 读取Yaml文件的过程中异常
     */
    public static Map<String, Object> load(File file) throws IOException {
        String yamlContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return YAML.load(yamlContent);
    }

    /**
     * @param map  要保存为Yaml文件的Map对象
     * @param file 保存的Yaml文件路径
     * @return 保存到文件的内容
     * @throws IOException 写入Yaml文件的过程中异常
     */
    public static String save(Map<String, Object> map, File file) throws IOException {
        String yamlContent = YAML.dump(map);
        FileUtils.writeStringToFile(file, yamlContent, StandardCharsets.UTF_8);
        return yamlContent;
    }

    /**
     * 相较于save方法多了"---\n"的前缀
     *
     * @param map  要保存为Yaml文件的Map对象
     * @param file 保存的Yaml文件路径
     * @return 保存到文件的内容
     * @throws IOException 写入Yaml文件的过程中异常
     */
    public static String saveMultiply(Map<String, Object> map, File file) throws IOException {
        String yamlContent = "---\n" + YAML.dump(map);
        FileUtils.writeStringToFile(file, yamlContent, StandardCharsets.UTF_8);
        return yamlContent;
    }
}
