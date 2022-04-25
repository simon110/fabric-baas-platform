package com.anhui.fabricbaasorg.util;

import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FabricYamlUtils {
    /**
     * 下标在前面的替换选项会优先被替换。
     *
     * @param originalContent 要替换的原始文本
     * @param replacements    多个替换选项，每个替换选项包含键和值
     * @return 进行关键词替换后的文本
     */
    private static String replace(String originalContent, String[][] replacements) {
        StringBuilder builder = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        for (int i = 0; i < originalContent.length(); ++i) {
            temp.append(originalContent.charAt(i));
            for (String[] replacement : replacements) {
                if (replacement[0].length() <= temp.length()) {
                    int j = temp.length() - replacement[0].length();
                    if (temp.substring(j).equals(replacement[0])) {
                        builder.append(temp.subSequence(0, j));
                        builder.append(replacement[1]);
                        temp.delete(0, temp.length());
                        break;
                    }
                }
            }
        }
        if (temp.length() != 0) {
            builder.append(temp.toString());
        }
        return builder.toString();
    }

    /**
     * @param configPaths 所有要合并的Yaml文件的路径
     * @param output      合并后的Yaml文件的输出路径
     * @throws IOException 读取或写出Yaml文件时异常
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void mergeConfigs(List<String> configPaths, File output) throws IOException {
        StringBuilder builder = new StringBuilder();
        List<File> files = new ArrayList<>(configPaths.size());
        for (String configPath : configPaths) {
            File file = new File(configPath);
            String config = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            builder.append(config);
            builder.append('\n');
            files.add(file);
        }

        FileUtils.writeStringToFile(output, builder.toString(), StandardCharsets.UTF_8);
        files.forEach(File::delete);
    }

    public static void generateOrdererYaml(OrdererEntity orderer, File output) throws IOException {
        String templatePath = "fabric/template/fabric-orderer.yaml";
        String config = FileUtils.readFileToString(new File(templatePath), StandardCharsets.UTF_8);
        String[][] replacements = new String[][]{
                {"orderer-example-com", orderer.getName()},
                {"kube-node", orderer.getKubeNodeName()},
                {"8050", orderer.getKubeNodePort().toString()}
        };
        config = replace(config, replacements);
        FileUtils.writeStringToFile(output, config, StandardCharsets.UTF_8);
    }

    public static void generatePeerYaml(PeerEntity peer, String domain, File output) throws IOException {
        String templatePath = "fabric/template/fabric-peer.yaml";
        String config = FileUtils.readFileToString(new File(templatePath), StandardCharsets.UTF_8);
        String[][] replacements = new String[][]{
                {"peer-org-example-com", peer.getName()},
                {"peer.org.example.com", domain},
                {"mspid", peer.getOrganizationName() + "MSP"},
                {"couchdb-username", peer.getCouchDBUsername()},
                {"couchdb-password", peer.getCouchDBPassword()},
                {"kube-node", peer.getKubeNodeName()},
                {"8051", peer.getKubeNodePort().toString()},
                {"8053", peer.getKubeEventNodePort().toString()}
        };
        config = replace(config, replacements);
        FileUtils.writeStringToFile(output, config, StandardCharsets.UTF_8);
    }
}
