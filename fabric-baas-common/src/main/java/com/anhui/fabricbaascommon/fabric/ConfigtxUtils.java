package com.anhui.fabricbaascommon.fabric;

import com.anhui.fabricbaascommon.bean.ConfigtxOrderer;
import com.anhui.fabricbaascommon.bean.ConfigtxOrganization;
import com.anhui.fabricbaascommon.exception.ConfigtxException;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaascommon.util.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigtxUtils {
    private static Map<String, Object> buildOrdererOrg(
            ConfigtxOrganization ordererOrganization,
            List<ConfigtxOrderer> configtxOrderers) {
        Map<String, Object> organization = new HashMap<>();
        organization.put("Name", ordererOrganization.getName());
        organization.put("ID", ordererOrganization.getId());
        organization.put("MSPDir", ordererOrganization.getMspDir().getAbsolutePath());

        Map<String, Object> policies = new HashMap<>();
        Map<String, Object> readersPolicy = new HashMap<>();
        readersPolicy.put("Type", "Signature");
        readersPolicy.put("Rule", String.format("OR('%s.member')", ordererOrganization.getId()));
        policies.put("Readers", readersPolicy);

        Map<String, Object> writersPolicy = new HashMap<>();
        writersPolicy.put("Type", "Signature");
        writersPolicy.put("Rule", String.format("OR('%s.member')", ordererOrganization.getId()));
        policies.put("Writers", writersPolicy);

        Map<String, Object> adminsPolicy = new HashMap<>();
        adminsPolicy.put("Type", "Signature");
        adminsPolicy.put("Rule", String.format("OR('%s.admin')", ordererOrganization.getId()));
        policies.put("Admins", adminsPolicy);

        organization.put("Policies", policies);
        List<String> ordererEndpoints = new ArrayList<>();
        for (ConfigtxOrderer orderer : configtxOrderers) {
            String ordererEndpoint = orderer.getHost() + ":" + orderer.getPort();
            ordererEndpoints.add(ordererEndpoint);
        }
        organization.put("OrdererEndpoints", ordererEndpoints);
        return organization;
    }

    private static Map<String, Object> buildOrg(ConfigtxOrganization configtxOrganization) {
        Map<String, Object> organization = new HashMap<>();
        organization.put("Name", configtxOrganization.getName());
        organization.put("ID", configtxOrganization.getId());
        organization.put("MSPDir", configtxOrganization.getMspDir().getAbsolutePath());

        Map<String, Object> policies = new HashMap<>();
        Map<String, Object> readersPolicy = new HashMap<>();
        readersPolicy.put("Type", "Signature");
        readersPolicy.put("Rule", String.format("OR('%s.admin', '%s.peer', '%s.client')", configtxOrganization.getId(), configtxOrganization.getId(), configtxOrganization.getId()));
        policies.put("Readers", readersPolicy);

        Map<String, Object> writersPolicy = new HashMap<>();
        writersPolicy.put("Type", "Signature");
        writersPolicy.put("Rule", String.format("OR('%s.admin', '%s.client')", configtxOrganization.getId(), configtxOrganization.getId()));
        policies.put("Writers", writersPolicy);

        Map<String, Object> adminsPolicy = new HashMap<>();
        adminsPolicy.put("Type", "Signature");
        adminsPolicy.put("Rule", String.format("OR('%s.admin')", configtxOrganization.getId()));
        policies.put("Admins", adminsPolicy);

        Map<String, Object> endorsementPolicy = new HashMap<>();
        endorsementPolicy.put("Type", "Signature");
        endorsementPolicy.put("Rule", String.format("OR('%s.peer')", configtxOrganization.getId()));
        policies.put("Endorsement", endorsementPolicy);

        organization.put("Policies", policies);
        return organization;
    }

    /**
     * @param genesisBlock 创世区块的输出路径
     * @param configtxDir  包含了configtx.yaml的目录
     */
    public static void generateGenesisBlock(
            String ordererGenesisName,
            String systemChannelName,
            File genesisBlock,
            File configtxDir)
            throws IOException, ConfigtxException, InterruptedException {
        assert configtxDir.isDirectory();
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-generate-genesis.sh",
                ordererGenesisName,
                systemChannelName,
                genesisBlock.getCanonicalPath(),
                configtxDir.getCanonicalPath());
        if (!genesisBlock.exists()) {
            throw new ConfigtxException("生成创世区块失败：" + str);
        }
    }

    @SuppressWarnings("unchecked")
    public static void generateConfigtx(
            File outputYaml,
            String consortiumName,
            List<ConfigtxOrderer> orderers,
            ConfigtxOrganization ordererOrganization,
            List<ConfigtxOrganization> otherOrganizations)
            throws IOException {
        Map<String, Object> configtx = YamlUtils.load(new File("configtx.yaml"));
        // 编辑Organizations部分的配置
        List<Map<String, Object>> organizations = (List<Map<String, Object>>) configtx.get("Organizations");
        organizations.clear();

        organizations.add(buildOrdererOrg(ordererOrganization, orderers));
        for (ConfigtxOrganization item : otherOrganizations) {
            organizations.add(buildOrg(item));
        }

        // 编辑Orderer部分的配置
        Map<String, Object> orderer = (Map<String, Object>) configtx.get("Orderer");
        List<String> addresses = (List<String>) orderer.get("Addresses");
        addresses.clear();
        for (ConfigtxOrderer item : orderers) {
            addresses.add(item.getHost() + ":" + item.getPort());
        }
        Map<String, Object> etcdRaft = (Map<String, Object>) orderer.get("EtcdRaft");
        List<Map<String, Object>> consenters = (List<Map<String, Object>>) etcdRaft.get("Consenters");
        consenters.clear();
        for (ConfigtxOrderer item : orderers) {
            Map<String, Object> consenter = new HashMap<>();
            consenter.put("Host", item.getHost());
            consenter.put("Port", item.getPort());
            consenter.put("ClientTLSCert", item.getClientTlsCert().getAbsolutePath());
            consenter.put("ServerTLSCert", item.getServerTlsCert().getAbsolutePath());
            consenters.add(consenter);
        }

        // 编辑Profiles部分的配置
        Map<String, Object> profiles = (Map<String, Object>) configtx.get("Profiles");
        Map<String, Object> genesis = (Map<String, Object>) profiles.get("TwoOrgsOrdererGenesis");
        profiles.remove("TwoOrgsChannel");
        profiles.remove("TwoOrgsOrdererGenesis");
        profiles.put("OrdererGenesis", genesis);
        Map<String, Object> genesisOrderer = (Map<String, Object>) genesis.get("Orderer");
        for (Map.Entry<String, Object> entry : orderer.entrySet()) {
            genesisOrderer.put(entry.getKey(), entry.getValue());
        }
        List<Map<String, Object>> ordererOrgs = new ArrayList<>();
        genesisOrderer.put("Organizations", ordererOrgs);
        ordererOrgs.add(organizations.get(0));

        Map<String, Object> genesisConsortiums = (Map<String, Object>) genesis.get("Consortiums");
        genesisConsortiums.clear();
        Map<String, Object> consortium = new HashMap<>();
        List<Map<String, Object>> consortiumOrgs = new ArrayList<>();
        for (int i = 1; i < organizations.size(); i++) {
            consortiumOrgs.add(organizations.get(i));
        }
        consortium.put("Organizations", consortiumOrgs);
        genesisConsortiums.put(consortiumName, consortium);

        YamlUtils.saveWithPrefix(configtx, outputYaml);
    }

    @SuppressWarnings("unchecked")
    public static void appendChannelToConfigtx(
            File targetYaml,
            String channelName,
            List<String> organizationNames)
            throws IOException, ConfigtxException {
        Map<String, Object> templateConfigtx = YamlUtils.load(new File("configtx.yaml"));
        Map<String, Object> profiles = (Map<String, Object>) templateConfigtx.get("Profiles");
        Map<String, Object> channel = (Map<String, Object>) profiles.get("TwoOrgsChannel");

        Map<String, Object> configtx = YamlUtils.load(targetYaml);
        List<Map<String, Object>> organizations = (List<Map<String, Object>>) configtx.get("Organizations");
        profiles = (Map<String, Object>) configtx.get("Profiles");
        if (profiles.containsKey(channelName)) {
            throw new ConfigtxException("通道已经存在于配置文件中");
        }

        // 获取联盟名称
        Map<String, Object> ordererGenesis = (Map<String, Object>) profiles.get("OrdererGenesis");
        Map<String, Object> ordererGenesisConsortiums = (Map<String, Object>) ordererGenesis.get("Consortiums");
        assert ordererGenesisConsortiums.size() == 1;
        String consortiumName = (String) ordererGenesisConsortiums.keySet().toArray()[0];
        // 对通道进行配置
        channel.put("Consortium", consortiumName);
        Map<String, Object> channelApplication = (Map<String, Object>) channel.get("Application");
        List<Map<String, Object>> channelOrganizations = (List<Map<String, Object>>) channelApplication.get("Organizations");
        channelOrganizations.clear();

        // 生成Organizations部分中所有组织名称的集合
        Map<String, Integer> organizationIndices = new HashMap<>();
        for (int i = 0; i < organizations.size(); i++) {
            Map<String, Object> org = organizations.get(i);
            organizationIndices.put((String) org.get("Name"), i);
        }

        for (String organizationName : organizationNames) {
            int organizationIndex = organizationIndices.getOrDefault(organizationName, -1);
            if (organizationIndex == -1) {
                throw new ConfigtxException("未在配置文件中找到相应的组织");
            }
            channelOrganizations.add(organizations.get(organizationIndex));
        }
        profiles.put(channelName, channel);
        YamlUtils.saveWithPrefix(configtx, targetYaml);
    }

    @SuppressWarnings("unchecked")
    public static void generateOrgConfigtx(
            File dstFile,
            ConfigtxOrganization configtxOrganization)
            throws IOException {
        Map<String, Object> orgConfig = YamlUtils.load(new File("organization.yaml"));
        List<Map<String, Object>> organizations = (List<Map<String, Object>>) orgConfig.get("Organizations");
        organizations.clear();
        organizations.add(buildOrg(configtxOrganization));
        YamlUtils.saveWithPrefix(orgConfig, dstFile);
    }

    public static void convertOrgConfigtxToJson(
            File outputJson,
            File configtxDir,
            String organizationName)
            throws ConfigtxException, IOException, InterruptedException {
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-generate-organization-config.sh",
                organizationName,
                configtxDir.getCanonicalPath(),
                outputJson.getCanonicalPath());
        if (!outputJson.exists()) {
            throw new ConfigtxException("将组织配置文件转换为JSON失败：" + str);
        }
    }
}
