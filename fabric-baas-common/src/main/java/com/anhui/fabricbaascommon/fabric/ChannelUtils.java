package com.anhui.fabricbaascommon.fabric;

import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.exception.ChannelException;
import com.anhui.fabricbaascommon.exception.EnvelopeException;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.JsonUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChannelUtils {
    public static void joinChannel(
            CoreEnv coreEnv,
            File channelGenesisBlock)
            throws IOException, ChannelException, InterruptedException {
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-join-channel.sh",
                coreEnv.getMspId(),
                coreEnv.getMspConfig().getAbsolutePath(),
                coreEnv.getAddress(),
                coreEnv.getTlsRootCert().getAbsolutePath(),
                channelGenesisBlock.getCanonicalPath());
        if (!str.toLowerCase().contains("successfully submitted proposal")) {
            throw new ChannelException("加入通道失败" + str);
        }
    }

    public static void fetchConfig(
            CoreEnv coreEnv,
            String channelName,
            File jsonConfig)
            throws ChannelException, IOException, InterruptedException {
        String jsonPath = jsonConfig.getCanonicalPath();
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-fetch-config.sh",
                coreEnv.getMspId(),
                coreEnv.getMspConfig().getAbsolutePath(),
                coreEnv.getAddress(),
                coreEnv.getTlsRootCert().getAbsolutePath(),
                channelName, jsonPath);
        if (!jsonConfig.exists()) {
            throw new ChannelException("获取通道配置失败：" + str);
        }
    }

    public static void fetchGenesisBlock(
            CoreEnv coreEnv,
            String channelName,
            File genesisBlock)
            throws ChannelException, IOException, InterruptedException {
        String blockPath = genesisBlock.getCanonicalPath();
        String str = CommandUtils.exec(ResourceUtils.getWorkingDir() + "/shell/fabric-fetch-genesis.sh",
                coreEnv.getMspId(),
                coreEnv.getMspConfig().getAbsolutePath(),
                coreEnv.getAddress(),
                coreEnv.getTlsRootCert().getAbsolutePath(),
                channelName, blockPath);
        if (!genesisBlock.exists()) {
            throw new ChannelException("获取通道创世区块失败：" + str);
        }
    }

    @SuppressWarnings("unchecked")
    public static void appendOrganizationToSysChannelConfig(
            String newOrgName,
            File newOrgJsonConfig,
            File channelJsonConfig)
            throws IOException, ChannelException {
        Map<String, Object> newOrgConfig = JsonUtils.loadAsMap(newOrgJsonConfig);
        Map<String, Object> channelConfig = JsonUtils.loadAsMap(channelJsonConfig);

        Map<String, Object> channelGroup = (Map<String, Object>) channelConfig.get("channel_group");
        Map<String, Object> groups = (Map<String, Object>) channelGroup.get("groups");
        Map<String, Object> consortiums = (Map<String, Object>) groups.get("Consortiums");
        groups = (Map<String, Object>) consortiums.get("groups");
        assert groups.size() == 1;
        Map<String, Object> consortium = (Map<String, Object>) groups.values().toArray()[0];
        groups = (Map<String, Object>) consortium.get("groups");
        if (groups.containsKey(newOrgName)) {
            throw new ChannelException("组织已存在于通道配置中：" + newOrgName);
        }
        groups.put(newOrgName, newOrgConfig);
        JsonUtils.save(channelJsonConfig, channelConfig);
    }

    @SuppressWarnings("unchecked")
    public static void appendOrganizationToAppChannelConfig(
            String newOrgName,
            File newOrgJsonConfig,
            File channelJsonConfig)
            throws IOException, ChannelException {
        Map<String, Object> newOrgConfig = JsonUtils.loadAsMap(newOrgJsonConfig);
        Map<String, Object> channelConfig = JsonUtils.loadAsMap(channelJsonConfig);

        Map<String, Object> channelGroup = (Map<String, Object>) channelConfig.get("channel_group");
        Map<String, Object> groups = (Map<String, Object>) channelGroup.get("groups");
        Map<String, Object> consortiums = (Map<String, Object>) groups.get("Application");
        groups = (Map<String, Object>) consortiums.get("groups");
        assert !groups.isEmpty();
        if (groups.containsKey(newOrgName)) {
            throw new ChannelException("组织已存在于通道配置中：" + newOrgName);
        }
        groups.put(newOrgName, newOrgConfig);
        JsonUtils.save(channelJsonConfig, channelConfig);
    }

    @SuppressWarnings("unchecked")
    public static void appendOrdererToChannelConfig(
            ConfigtxOrderer newOrderer,
            File channelJsonConfig)
            throws IOException, ChannelException {
        String addr = newOrderer.getAddr();
        String base64ServerTlsCert = Base64Utils.encodeToString(FileUtils.readFileToByteArray(new File(newOrderer.getServerTlsCert().getAbsolutePath())));
        String base64ClientTlsCert = Base64Utils.encodeToString(FileUtils.readFileToByteArray(new File(newOrderer.getClientTlsCert().getAbsolutePath())));

        Map<String, Object> channelConfig = JsonUtils.loadAsMap(channelJsonConfig);
        Map<String, Object> channelGroup = (Map<String, Object>) channelConfig.get("channel_group");
        Map<String, Object> groups = (Map<String, Object>) channelGroup.get("groups");
        Map<String, Object> ordererConfig = (Map<String, Object>) groups.get("Orderer");
        Map<String, Object> ordererConfigGroups = (Map<String, Object>) ordererConfig.get("groups");
        assert ordererConfigGroups.size() == 1;
        Map<String, Object> ordererConfigGroup = (Map<String, Object>) ordererConfigGroups.values().toArray()[0];
        Map<String, Object> ordererConfigGroupValues = (Map<String, Object>) ordererConfigGroup.get("values");
        Map<String, Object> endpoints = (Map<String, Object>) ordererConfigGroupValues.get("Endpoints");
        Map<String, Object> endpointValue = (Map<String, Object>) endpoints.get("value");
        List<String> addresses = (List<String>) endpointValue.get("addresses");
        if (addresses.contains(addr)) {
            throw new ChannelException("该Orderer已经存在于通道配置中");
        }
        addresses.add(addr);

        Map<String, Object> ordererConfigValues = (Map<String, Object>) ordererConfig.get("values");
        Map<String, Object> consensusType = (Map<String, Object>) ordererConfigValues.get("ConsensusType");
        Map<String, Object> consensusTypeValue = (Map<String, Object>) consensusType.get("value");
        Map<String, Object> consensusTypeValueMetadata = (Map<String, Object>) consensusTypeValue.get("metadata");
        List<Object> consenters = (List<Object>) consensusTypeValueMetadata.get("consenters");

        Map<String, Object> newConsenter = new TreeMap<>();
        newConsenter.put("client_tls_cert", base64ClientTlsCert);
        newConsenter.put("server_tls_cert", base64ServerTlsCert);
        newConsenter.put("host", newOrderer.getHost());
        newConsenter.put("port", newOrderer.getPort());
        consenters.add(newConsenter);

        Map<String, Object> values = (Map<String, Object>) channelGroup.get("values");
        Map<String, Object> ordererAddresses = (Map<String, Object>) values.get("OrdererAddresses");
        Map<String, Object> ordererAddressesValue = (Map<String, Object>) ordererAddresses.get("value");
        addresses = (List<String>) ordererAddressesValue.get("addresses");
        assert !addresses.contains(addr);
        addresses.add(addr);

        JsonUtils.save(channelJsonConfig, channelConfig);
    }

    @SuppressWarnings("unchecked")
    public static void appendAnchorPeerToChannelConfig(
            Node newAnchorPeer,
            String organizationName,
            File channelJsonConfig)
            throws IOException, ChannelException {
        Map<String, Object> channelConfig = JsonUtils.loadAsMap(channelJsonConfig);
        Map<String, Object> channelGroup = (Map<String, Object>) channelConfig.get("channel_group");
        Map<String, Object> groups = (Map<String, Object>) channelGroup.get("groups");
        Map<String, Object> consortiums = (Map<String, Object>) groups.get("Application");
        groups = (Map<String, Object>) consortiums.get("groups");
        if (!groups.containsKey(organizationName)) {
            throw new ChannelException("未在配置文件里面找到相应的组织：" + organizationName);
        }
        Map<String, Object> orgConfig = (Map<String, Object>) groups.get(organizationName);
        Map<String, Object> orgConfigValues = (Map<String, Object>) orgConfig.get("values");
        Map<String, Object> anchorPeerConfig = (Map<String, Object>) orgConfigValues.get("AnchorPeers");
        Map<String, Object> anchorPeerValue = (Map<String, Object>) anchorPeerConfig.get("value");
        List<Map<String, Object>> anchorPeers = (List<Map<String, Object>>) anchorPeerValue.get("anchor_peers");
        for (Map<String, Object> anchorPeer : anchorPeers) {
            String anchorPeerHost = (String) anchorPeer.get("host");
            int anchorPeerPort = (Integer) anchorPeer.get("port");
            if (newAnchorPeer.getHost().equals(anchorPeerHost) && newAnchorPeer.getPort() == anchorPeerPort) {
                throw new ChannelException("已存在相同地址的锚节点" + newAnchorPeer.getAddr());
            }
        }
        Map<String, Object> newAnchorPeerObj = new TreeMap<>();
        newAnchorPeerObj.put("host", newAnchorPeer.getHost());
        newAnchorPeerObj.put("port", newAnchorPeer.getPort());
        anchorPeers.add(newAnchorPeerObj);

        JsonUtils.save(channelJsonConfig, channelConfig);
    }

    public static void generateEnvelope(
            String channelName,
            File envelope,
            File oldConfig,
            File newConfig)
            throws EnvelopeException, IOException, InterruptedException {
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-generate-update-envelope.sh",
                channelName,
                oldConfig.getCanonicalPath(),
                newConfig.getCanonicalPath(),
                envelope.getCanonicalPath());
        if (!envelope.exists()) {
            throw new EnvelopeException("生成通道更新Envelope失败：" + str);
        }
    }

    public static void signEnvelope(
            MSPEnv MSPEnv,
            File envelope)
            throws IOException, InterruptedException, EnvelopeException {
        byte[] oldBytes = FileUtils.readFileToByteArray(envelope);
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-sign-envelope.sh",
                MSPEnv.getMspId(),
                MSPEnv.getMspConfig().getAbsolutePath(),
                envelope.getCanonicalPath());
        byte[] newBytes = FileUtils.readFileToByteArray(envelope);
        // 通过对比签名前后的文件内容来确定签名是否成功
        if (Arrays.equals(oldBytes, newBytes)) {
            throw new EnvelopeException("对Envelope的签名失败：" + str);
        }
    }

    public static void submitChannelUpdate(
            MSPEnv organizationMSPEnv,
            TLSEnv ordererTLSEnv,
            String channelName,
            File envelope)
            throws IOException, InterruptedException, ChannelException {
        String str = CommandUtils.exec(
                ResourceUtils.getWorkingDir() + "/shell/fabric-submit-envelope.sh",
                organizationMSPEnv.getMspId(),
                organizationMSPEnv.getMspConfig().getAbsolutePath(),
                ordererTLSEnv.getAddress(),
                ordererTLSEnv.getTlsRootCert().getAbsolutePath(),
                channelName, envelope.getCanonicalPath());
        if (!str.toLowerCase().contains("successfully submitted channel update")) {
            throw new ChannelException("更新通道失败：" + str);
        }
    }

    public static void createChannel(
            MSPEnv organizationMSPEnv,
            TLSEnv ordererTLSEnv,
            File configtxDir,
            String channelName,
            File channelGenesis)
            throws ChannelException, IOException, InterruptedException {
        String str = CommandUtils.exec(ResourceUtils.getWorkingDir() + "/shell/fabric-create-channel.sh",
                organizationMSPEnv.getMspId(),
                organizationMSPEnv.getMspConfig().getAbsolutePath(),
                ordererTLSEnv.getAddress(),
                ordererTLSEnv.getTlsRootCert().getAbsolutePath(),
                configtxDir.getCanonicalPath(),
                channelName, channelGenesis.getCanonicalPath());
        if (!str.toLowerCase().contains("created") || !channelGenesis.exists()) {
            throw new ChannelException("创建通道失败：" + str);
        }
    }
}
