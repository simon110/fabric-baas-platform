package com.anhui.fabricbaascommon.fabric;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.exception.ChannelException;
import com.anhui.fabricbaascommon.exception.EnvelopeException;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.JsonUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ChannelUtils {
    public static ChannelStatus getChannelStatus(
            CoreEnv peerCoreEnv,
            TlsEnv ordererTlsEnv,
            String channelName) throws IOException, InterruptedException, ChannelException {
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getTlsEnv().getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsEnv().getTlsRootCert().getCanonicalPath()
        );

        String str = CommandUtils.exec(envs, "peer", "channel", "getinfo",
                "-c", channelName,
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath()
        );
        if (!str.toLowerCase().contains("blockchain info: ")) {
            throw new ChannelException("查询通道信息失败：" + str);
        }
        String json = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1);
        JSONObject result = JSONUtil.parseObj(json);

        ChannelStatus info = new ChannelStatus();
        info.setChannelName(channelName);
        info.setHeight(result.getInt("height"));
        info.setCurrentBlockHash(result.getStr("currentBlockHash"));
        info.setPreviousBlockHash(result.getStr("previousBlockHash"));
        return info;
    }

    public static void joinChannel(
            CoreEnv coreEnv,
            File channelGenesisBlock)
            throws IOException, ChannelException, InterruptedException {
        Assert.isTrue(channelGenesisBlock.exists());
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", coreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", coreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", coreEnv.getTlsEnv().getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", coreEnv.getTlsEnv().getTlsRootCert().getCanonicalPath()
        );
        String str = CommandUtils.exec(envs, "peer", "channel", "join", "-b", channelGenesisBlock.getCanonicalPath());
        if (!str.toLowerCase().contains("successfully submitted proposal")) {
            throw new ChannelException("加入通道失败" + str);
        }
    }

    public static void fetchConfig(
            CoreEnv coreEnv,
            String channelName,
            File jsonConfig)
            throws ChannelException, IOException, InterruptedException {
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", coreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", coreEnv.getMspConfig().getCanonicalPath()
        );

        File configBlock = MyFileUtils.createTempFile("pb");
        String str = CommandUtils.exec(envs, "peer", "channel", "fetch", "config",
                configBlock.getCanonicalPath(),
                "-o", coreEnv.getTlsEnv().getAddress(),
                "-c", channelName,
                "--cafile", coreEnv.getTlsRootCert().getCanonicalPath()
        );
        if (!configBlock.exists()) {
            throw new ChannelException("获取通道配置区块失败：" + str);
        }

        String cmd = String.format("'configtxlator proto_decode --input %s --type common.Block | jq .data.data[0].payload.data.config'", configBlock.getCanonicalPath());
        String json = CommandUtils.exec(envs, "sh", "-c", cmd);
        JsonUtils.loadAsMap(json);
        FileUtils.writeStringToFile(jsonConfig, json, StandardCharsets.UTF_8);
        if (!jsonConfig.exists()) {
            throw new ChannelException("解析通道配置失败：" + json);
        }
    }

    public static void fetchGenesisBlock(
            CoreEnv coreEnv,
            String channelName,
            File genesisBlock)
            throws ChannelException, IOException, InterruptedException {
        String blockPath = genesisBlock.getCanonicalPath();
        String str = CommandUtils.exec(MyFileUtils.getWorkingDir() + "/shell/fabric-fetch-genesis.sh",
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
        Assert.notEmpty(groups);
        if (groups.containsKey(newOrgName)) {
            throw new ChannelException("组织已存在于通道配置中：" + newOrgName);
        }
        groups.put(newOrgName, newOrgConfig);
        JsonUtils.save(channelJsonConfig, channelConfig);
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
        Assert.isTrue(groups.size() == 1);
        Map<String, Object> consortium = (Map<String, Object>) groups.values().toArray()[0];
        groups = (Map<String, Object>) consortium.get("groups");
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
        byte[] newOrdererTlsCertBytes = FileUtils.readFileToByteArray(newOrderer.getServerTlsCert());
        String base64TlsCert = Base64Utils.encodeToString(newOrdererTlsCertBytes);

        Map<String, Object> channelConfig = JsonUtils.loadAsMap(channelJsonConfig);
        Map<String, Object> channelGroup = (Map<String, Object>) channelConfig.get("channel_group");
        Map<String, Object> groups = (Map<String, Object>) channelGroup.get("groups");
        Map<String, Object> ordererConfig = (Map<String, Object>) groups.get("Orderer");
        Map<String, Object> ordererConfigGroups = (Map<String, Object>) ordererConfig.get("groups");
        Assert.isTrue(ordererConfigGroups.size() == 1);
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
        newConsenter.put("client_tls_cert", base64TlsCert);
        newConsenter.put("server_tls_cert", base64TlsCert);
        newConsenter.put("host", newOrderer.getHost());
        newConsenter.put("port", newOrderer.getPort());
        consenters.add(newConsenter);

        Map<String, Object> values = (Map<String, Object>) channelGroup.get("values");
        Map<String, Object> ordererAddresses = (Map<String, Object>) values.get("OrdererAddresses");
        Map<String, Object> ordererAddressesValue = (Map<String, Object>) ordererAddresses.get("value");
        addresses = (List<String>) ordererAddressesValue.get("addresses");
        Assert.isFalse(addresses.contains(addr));
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
        if (orgConfigValues.containsKey("AnchorPeers")) {
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
        } else {
            Map<String, Object> anchorPeer = new HashMap<>();
            anchorPeer.put("host", newAnchorPeer.getHost());
            anchorPeer.put("port", newAnchorPeer.getPort());
            List<Map<String, Object>> anchorPeers = Collections.singletonList(anchorPeer);
            Map<String, Object> anchorPeerValue = new HashMap<>();
            anchorPeerValue.put("anchor_peers", anchorPeers);
            Map<String, Object> anchorPeerConfig = new HashMap<>();
            anchorPeerConfig.put("mod_policy", "Admins");
            anchorPeerConfig.put("version", "0");
            anchorPeerConfig.put("value", anchorPeerValue);
            orgConfigValues.put("AnchorPeers", anchorPeerConfig);
        }

        JsonUtils.save(channelJsonConfig, channelConfig);
    }

    public static void generateEnvelope(
            String channelName,
            File envelope,
            File oldConfig,
            File newConfig)
            throws EnvelopeException, IOException, InterruptedException {
        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-generate-update-envelope.sh",
                channelName,
                oldConfig.getCanonicalPath(),
                newConfig.getCanonicalPath(),
                envelope.getCanonicalPath());
        if (!envelope.exists()) {
            throw new EnvelopeException("生成通道更新Envelope失败：" + str);
        }
    }

    public static void signEnvelope(
            MspEnv mspEnv,
            File envelope)
            throws IOException, InterruptedException, EnvelopeException {
        Assert.isTrue(envelope.exists());
        Map<String, String> envs = CommandUtils.buildEnvs(
                "CORE_PEER_LOCALMSPID", mspEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", mspEnv.getMspConfig().getCanonicalPath()
        );
        byte[] oldBytes = FileUtils.readFileToByteArray(envelope);
        String str = CommandUtils.exec(envs, "peer", "channel", "signconfigtx", "-f", envelope.getCanonicalPath());
        byte[] newBytes = FileUtils.readFileToByteArray(envelope);
        // 通过对比签名前后的文件内容来确定签名是否成功
        if (Arrays.equals(oldBytes, newBytes)) {
            throw new EnvelopeException("对Envelope的签名失败：" + str);
        }
    }

    public static void submitChannelUpdate(
            MspEnv organizationMspEnv,
            TlsEnv ordererTlsEnv,
            String channelName,
            File envelope)
            throws IOException, InterruptedException, ChannelException {
        Assert.isTrue(envelope.exists());
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", organizationMspEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", organizationMspEnv.getMspConfig().getCanonicalPath()
        );
        String str = CommandUtils.exec(envs, "peer", "channel", "update",
                "-f", envelope.getCanonicalPath(),
                "-c", channelName,
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "true",
                "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath()
        );
        if (!str.toLowerCase().contains("successfully submitted channel update")) {
            throw new ChannelException("更新通道失败：" + str);
        }
    }

    public static void createChannel(
            MspEnv organizationMspEnv,
            TlsEnv ordererTlsEnv,
            File configtxDir,
            String channelName,
            File channelGenesis)
            throws ChannelException, IOException, InterruptedException {
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", organizationMspEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", organizationMspEnv.getMspConfig().getCanonicalPath()
        );

        File channelCreateTx = MyFileUtils.createTempFile("tx");
        String str = CommandUtils.exec(envs, "configtxgen",
                "-profile", channelName,
                "-outputCreateChannelTx", channelCreateTx.getCanonicalPath(),
                "-channelID", channelName,
                "-configPath", configtxDir.getCanonicalPath()
        );
        if (!channelCreateTx.exists()) {
            throw new ChannelException("生成创建通道交易失败：" + str);
        }
        str = CommandUtils.exec(envs, "peer", "channel", "create",
                "-o", ordererTlsEnv.getAddress(),
                "-c", channelName,
                "-f", channelCreateTx.getCanonicalPath(),
                "--outputBlock", channelGenesis.getCanonicalPath(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath()
        );
        if (!channelGenesis.exists()) {
            throw new ChannelException("创建通道失败：" + str);
        }
    }
}
