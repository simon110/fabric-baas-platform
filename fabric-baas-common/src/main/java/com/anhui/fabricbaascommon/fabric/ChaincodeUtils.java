package com.anhui.fabricbaascommon.fabric;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONObject;
import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.exception.ChaincodeException;
import com.anhui.fabricbaascommon.util.CommandUtils;
import com.anhui.fabricbaascommon.util.MyFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChaincodeUtils {
    /**
     * @param channelName 通道名称
     * @param peerCoreEnv 对应Peer的环境变量
     * @return 已提交的链码名称
     * @throws ChaincodeException 查询不到时抛出异常
     */
    public static List<ApprovedChaincode> queryCommittedChaincodes(
            String channelName,
            CoreEnv peerCoreEnv)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        String cmd = String.format("peer lifecycle chaincode querycommitted -C %s | grep 'Name:'", channelName);
        String str = CommandUtils.exec(envs, "sh", "-c", cmd);
        List<ApprovedChaincode> approvedChaincodes = new ArrayList<>();
        if (str.isBlank()) {
            return approvedChaincodes;
        }
        if (!str.startsWith("Name: ")) {
            throw new ChaincodeException("读取已提交的代码失败：" + str);
        }

        // 将shell的返回值str处理为CommittedChaincode
        // Committed chaincode definitions on channel 'mychannel':
        // Name: basic, Version: 1.0, Sequence: 1, Endorsement Plugin: escc, Validation Plugin: vscc
        String[] outputLines = str.split("\n");
        Assert.isTrue(outputLines.length > 0);

        for (String outputLine : outputLines) {
            String[] parts = outputLine.split(", ");
            Assert.isTrue(parts.length == 5);
            Assert.isTrue(parts[0].startsWith("Name: "));
            Assert.isTrue(parts[1].startsWith("Version: "));
            Assert.isTrue(parts[2].startsWith("Sequence: "));
            Assert.isTrue(parts[3].startsWith("Endorsement Plugin: "));
            Assert.isTrue(parts[4].startsWith("Validation Plugin: "));

            ApprovedChaincode approvedChaincode = new ApprovedChaincode();
            approvedChaincode.setChannelName(channelName);
            approvedChaincode.setName(parts[0].replaceFirst("Name: ", ""));
            approvedChaincode.setVersion(parts[1].replaceFirst("Version: ", ""));
            approvedChaincode.setSequence(Integer.parseInt(parts[2].replaceFirst("Sequence: ", "")));
            approvedChaincodes.add(approvedChaincode);
        }
        return approvedChaincodes;
    }

    /**
     * @param peerCoreEnv 对应Peer的环境变量
     * @return 已安卓的链码名称
     * @throws ChaincodeException 查询不到时抛出异常
     */
    public static List<InstalledChaincode> queryInstalledChaincodes(CoreEnv peerCoreEnv)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        String str = CommandUtils.exec(envs, "sh", "-c", "peer lifecycle chaincode queryinstalled | grep 'Package ID:'");
        List<InstalledChaincode> installedChaincodes = new ArrayList<>();
        if (str.isBlank()) {
            return installedChaincodes;
        }
        if (!str.startsWith("Package ID: ")) {
            throw new ChaincodeException("读取已安装的链码失败：" + str);
        }
        //将shell的返回值str处理为installedChaincode
        //Installed chaincodes on peer:
        //Package ID: basic_1.0:dee2d612e15f5059478b9048fa4b3c9f792096554841d642b9b59099fa0e04a4, Label: basic_1.0
        String[] outputLines = str.split("\n");
        for (String outputLine : outputLines) {
            InstalledChaincode installedChaincode = new InstalledChaincode();
            String[] parts = outputLine.strip().split(", ");
            Assert.isTrue(parts[0].startsWith("Package ID: "));
            Assert.isTrue(parts[1].startsWith("Label: "));
            installedChaincode.setIdentifier(parts[0].replaceFirst("Package ID: ", ""));
            installedChaincode.setLabel(parts[1].replaceFirst("Label: ", ""));
            installedChaincodes.add(installedChaincode);
        }
        return installedChaincodes;
    }

    /**
     * @param chaincodePackage 输出的tar文件的路径
     * @param peerCoreEnv      对应Peer的环境变量
     * @return 链码安装成功后返回的Package ID
     * @throws ChaincodeException 安装完成后如果queryInstalledChaincodes的结果没有变化都应该抛出异常
     */
    public static String installChaincode(
            File chaincodePackage,
            CoreEnv peerCoreEnv)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        if (!chaincodePackage.exists()) {
            throw new ChaincodeException("链码文件不存在：" + chaincodePackage.getCanonicalPath());
        }
        // 查询installed链码
        List<InstalledChaincode> oldInstalledChaincodes = queryInstalledChaincodes(peerCoreEnv);
        String str = CommandUtils.exec(envs, "peer", "lifecycle", "chaincode", "install", chaincodePackage.getCanonicalPath());
        // 查询peer链码有无变化
        List<InstalledChaincode> newInstalledChaincodes = queryInstalledChaincodes(peerCoreEnv);
        if (newInstalledChaincodes.size() == oldInstalledChaincodes.size() || !str.toLowerCase().contains("chaincode code package identifier")) {
            throw new ChaincodeException("链码安装失败：" + chaincodePackage);
        }
        return str.substring(str.lastIndexOf(" ") + 1).strip();
    }

    /**
     * @param ordererTlsEnv       Orderer节点的TLS连接信息
     * @param peerCoreEnv         Peer MSP和TLS信息
     * @param channelName         在哪个通道上
     * @param packageId           安装链码后返回的包ID
     * @param chaincodeProperties 链码信息
     * @throws ChaincodeException 执行完成后如果checkReadiness的结果没有发生变化都应该抛出异常
     */
    public static void approveChaincode(
            TlsEnv ordererTlsEnv,
            CoreEnv peerCoreEnv,
            String channelName,
            String packageId,
            BasicChaincodeProperties chaincodeProperties) throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();
        ordererTlsEnv.assertTlsCert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        //检查链码批准情况
        List<ChaincodeApproval> oldCheckCommitReadiness = checkCommittedReadiness(ordererTlsEnv, peerCoreEnv, channelName, chaincodeProperties);
        String str = CommandUtils.exec(envs, "peer", "lifecycle", "chaincode", "approveformyorg",
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath(),
                "--channelID", channelName,
                "--name", chaincodeProperties.getName(),
                "--version", chaincodeProperties.getVersion(),
                "--sequence", chaincodeProperties.getSequence().toString(),
                "--package-id", packageId
        );
        List<ChaincodeApproval> newCheckCommitReadiness = checkCommittedReadiness(ordererTlsEnv, peerCoreEnv, channelName, chaincodeProperties);
        if (newCheckCommitReadiness.equals(oldCheckCommitReadiness)) {
            throw new ChaincodeException("投票失败：" + str);
        }
    }

    /**
     * @param ordererTlsEnv       Orderer节点的TLS连接信息
     * @param peerCoreEnv         Peer MSP和TLS信息
     * @param channelName         在哪个通道上
     * @param chaincodeProperties 链码信息
     * @throws ChaincodeException 查询不到时抛出异常
     */
    public static List<ChaincodeApproval> checkCommittedReadiness(
            TlsEnv ordererTlsEnv,
            CoreEnv peerCoreEnv,
            String channelName,
            BasicChaincodeProperties chaincodeProperties)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();
        ordererTlsEnv.assertTlsCert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        String str = CommandUtils.exec(envs, "peer", "lifecycle", "chaincode", "checkcommitreadiness",
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath(),
                "--channelID", channelName,
                "--name", chaincodeProperties.getName(),
                "--version", chaincodeProperties.getVersion(),
                "--sequence", chaincodeProperties.getSequence().toString()
        );
        if (!str.toLowerCase().startsWith("chaincode definition for chaincode ")) {
            throw new ChaincodeException("检查链码失败：" + str);
        }
        List<ChaincodeApproval> chaincodeApprovals = new ArrayList<>();
        String[] outputLines = str.split("\n");
        for (String line : outputLines) {
            Boolean approval = null;
            if (line.contains(": true")) {
                approval = true;
            } else if (line.contains(": false")) {
                approval = false;
            }
            if (approval != null) {
                ChaincodeApproval chaincodeApproval = new ChaincodeApproval();
                String orgName = line.strip().split(": ")[0];
                chaincodeApproval.setApproved(approval);
                chaincodeApproval.setOrganizationName(orgName);
                chaincodeApprovals.add(chaincodeApproval);
            }
        }
        return chaincodeApprovals;
    }

    /**
     * @param ordererTlsEnv        Orderer节点的TLS连接信息
     * @param committerPeerCoreEnv 当前组织的Peer MSP和TLS信息
     * @param endorserPeerTlsEnvs  其他组织的Peer TLS信息
     * @param channelName          通道名称
     * @param chaincodeProperties  链码信息
     * @throws ChaincodeException 执行完成后如果queryCommitted的结果没有发生变化都应该抛出异常
     */
    public static void commitChaincode(
            TlsEnv ordererTlsEnv,
            CoreEnv committerPeerCoreEnv,
            List<TlsEnv> endorserPeerTlsEnvs,
            String channelName,
            BasicChaincodeProperties chaincodeProperties)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        committerPeerCoreEnv.selfAssert();
        ordererTlsEnv.assertTlsCert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", committerPeerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", committerPeerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", committerPeerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", committerPeerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        List<String> commandList = Arrays.asList("peer", "lifecycle", "chaincode", "commit",
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath(),
                "--channelID", channelName,
                "--name", chaincodeProperties.getName(),
                "--version", chaincodeProperties.getVersion(),
                "--sequence", chaincodeProperties.getSequence().toString()
        );
        // Arrays.asList方法生成的列表不支持add或remove
        commandList = new ArrayList<>(commandList);
        for (TlsEnv otherPeerTlsEnv : endorserPeerTlsEnvs) {
            commandList.add("--peerAddresses");
            commandList.add(otherPeerTlsEnv.getAddress());
            commandList.add("--tlsRootCertFiles");
            commandList.add(otherPeerTlsEnv.getTlsRootCert().getCanonicalPath());
        }
        String[] commands = new String[commandList.size()];
        commandList.toArray(commands);

        List<ApprovedChaincode> oldApprovedChaincodes = queryCommittedChaincodes(channelName, committerPeerCoreEnv);
        String str = CommandUtils.exec(envs, commands);
        List<ApprovedChaincode> newApprovedChaincodes = queryCommittedChaincodes(channelName, committerPeerCoreEnv);
        if (oldApprovedChaincodes.equals(newApprovedChaincodes)) {
            throw new ChaincodeException("提交失败：" + str);
        }
    }

    private static JSONObject buildChaincodeParams(String functionName, List<String> params) {
        JSONObject chaincodeParams = new JSONObject();
        chaincodeParams.set("function", functionName);
        chaincodeParams.set("Args", params);
        return chaincodeParams;
    }

    public static void executeInvoke(
            String chaincodeName,
            String functionName,
            List<String> params,
            String channelName,
            TlsEnv ordererTlsEnv,
            CoreEnv committerPeerCoreEnv,
            List<TlsEnv> endorserTlsEnvs) throws CertfileException, IOException, InterruptedException, ChaincodeException {
        ordererTlsEnv.assertTlsCert();
        for (TlsEnv endorserTlsEnv : endorserTlsEnvs) {
            endorserTlsEnv.assertTlsCert();
        }
        JSONObject chaincodeParams = buildChaincodeParams(functionName, params);


        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", committerPeerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", committerPeerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", committerPeerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", committerPeerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        List<String> commandList = Arrays.asList("peer", "chaincode", "invoke",
                "-o", ordererTlsEnv.getAddress(),
                "--tls", "--cafile", ordererTlsEnv.getTlsRootCert().getCanonicalPath(),
                "-C", channelName,
                "-n", chaincodeName,
                "-c", chaincodeParams.toString()
        );
        commandList = new ArrayList<>(commandList);
        for (TlsEnv endorserTlsEnv : endorserTlsEnvs) {
            commandList.add("--peerAddresses");
            commandList.add(endorserTlsEnv.getAddress());
            commandList.add("--tlsRootCertFiles");
            commandList.add(endorserTlsEnv.getTlsRootCert().getAbsolutePath());
        }
        String[] commands = new String[commandList.size()];
        commandList.toArray(commands);
        String str = CommandUtils.exec(envs, commands);
        if (!str.toLowerCase().contains("invoke successful") || !str.contains("status:200")) {
            throw new ChaincodeException("智能合约调用失败：" + str);
        }
    }

    public static String executeQuery(
            String chaincodeName,
            String functionName,
            List<String> params,
            String channelName,
            CoreEnv peerCoreEnv) throws CertfileException, IOException, InterruptedException, ChaincodeException {
        peerCoreEnv.selfAssert();
        Map<String, String> envs = CommandUtils.buildEnvs(
                "FABRIC_CFG_PATH", MyFileUtils.getWorkingDir(),
                "CORE_PEER_TLS_ENABLED", "true",
                "CORE_PEER_LOCALMSPID", peerCoreEnv.getMspId(),
                "CORE_PEER_MSPCONFIGPATH", peerCoreEnv.getMspConfig().getCanonicalPath(),
                "CORE_PEER_ADDRESS", peerCoreEnv.getAddress(),
                "CORE_PEER_TLS_ROOTCERT_FILE", peerCoreEnv.getTlsRootCert().getCanonicalPath()
        );

        JSONObject chaincodeParams = buildChaincodeParams(functionName, params);
        String str = CommandUtils.exec(envs, "peer", "chaincode", "query",
                "-C", channelName,
                "-n", chaincodeName,
                "-c", chaincodeParams.toString()
        );
        if (str.toLowerCase().contains("error: ") || str.contains("status:500")) {
            throw new ChaincodeException("链码调用失败：" + str);
        }
        return str.strip();
    }
}
