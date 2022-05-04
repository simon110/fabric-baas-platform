package com.anhui.fabricbaascommon.fabric;

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

public class ChaincodeUtils {
    /**
     * @param channelName 通道名称
     * @param peerCoreEnv 对应Peer的环境变量
     * @return 已提交的链码名称
     * @throws ChaincodeException 查询不到时抛出异常
     */
    public static List<CommittedChaincode> queryCommittedChaincodes(
            String channelName,
            CoreEnv peerCoreEnv)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();

        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-query-committed.sh",
                peerCoreEnv.getMspId(),
                peerCoreEnv.getTlsRootCert().getAbsolutePath(),
                peerCoreEnv.getMspConfig().getAbsolutePath(),
                peerCoreEnv.getAddress(),
                channelName);
        List<CommittedChaincode> committedChaincodes = new ArrayList<>();
        if (str.isBlank()) {
            return committedChaincodes;
        }
        if (!str.startsWith("Name: ")) {
            throw new ChaincodeException("读取已提交的代码失败：" + str);
        }

        //将shell的返回值str处理为CommittedChaincode
        //Committed chaincode definitions on channel 'mychannel':
        //Name: basic, Version: 1.0, Sequence: 1, Endorsement Plugin: escc, Validation Plugin: vscc
        String[] outputLines = str.split("\n");
        assert outputLines.length > 0;

        for (String outputLine : outputLines) {
            String[] parts = outputLine.split(", ");
            assert parts.length == 5;
            assert parts[0].startsWith("Name: ");
            assert parts[1].startsWith("Version: ");
            assert parts[2].startsWith("Sequence: ");
            assert parts[3].startsWith("Endorsement Plugin: ");
            assert parts[4].startsWith("Validation Plugin: ");

            CommittedChaincode committedChaincode = new CommittedChaincode();
            committedChaincode.setChannelName(channelName);
            committedChaincode.setName(parts[0].replaceFirst("Name: ", ""));
            committedChaincode.setVersion(parts[1].replaceFirst("Version: ", ""));
            committedChaincode.setSequence(Integer.parseInt(parts[2].replaceFirst("Sequence: ", "")));
            committedChaincodes.add(committedChaincode);
        }
        return committedChaincodes;
    }

    /**
     * @param peerCoreEnv 对应Peer的环境变量
     * @return 已安卓的链码名称
     * @throws ChaincodeException 查询不到时抛出异常
     */
    public static List<InstalledChaincode> queryInstalledChaincodes(CoreEnv peerCoreEnv)
            throws IOException, InterruptedException, CertfileException, ChaincodeException {
        peerCoreEnv.selfAssert();

        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-query-installed.sh",
                peerCoreEnv.getMspId(),
                peerCoreEnv.getTlsRootCert().getAbsolutePath(),
                peerCoreEnv.getMspConfig().getAbsolutePath(),
                peerCoreEnv.getAddress());
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
            assert parts[0].startsWith("Package ID: ");
            assert parts[1].startsWith("Label: ");
            installedChaincode.setIdentifier(parts[0].replaceFirst("Package ID: ", ""));
            installedChaincode.setLabel(parts[1].replaceFirst("Label: ", ""));
            installedChaincodes.add(installedChaincode);
        }
        return installedChaincodes;
    }

    /**
     * @param srcCodeDir     链码源代码（包含go.mod文件那个）
     * @param chaincodeLabel 链码标签
     * @param outputPackage  输出的tar文件的路径
     * @throws ChaincodeException 编译智能合约出错（任何生成tar失败的情况都应该抛出异常）
     */
    public static void buildChaincodePackage(
            File srcCodeDir,
            String chaincodeLabel,
            File outputPackage) throws ChaincodeException, IOException, InterruptedException {
        if (!srcCodeDir.exists() && MyFileUtils.exists(srcCodeDir.getAbsolutePath() + "/go.mod")) {
            throw new ChaincodeException("未找到Chaincode源代码");
        }
        if (outputPackage.exists()) {
            throw new ChaincodeException("目标路径已存在文件");
        }
        CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-package.sh",
                srcCodeDir.getCanonicalPath(),
                chaincodeLabel,
                outputPackage.getCanonicalPath());
        if (!MyFileUtils.exists(outputPackage.getAbsolutePath())) {
            throw new ChaincodeException("链码打包失败：" + outputPackage.getAbsolutePath());
        }
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

        if (!chaincodePackage.exists()) {
            throw new ChaincodeException("链码文件不存在：" + chaincodePackage.getCanonicalPath());
        }
        // 查询installed链码
        List<InstalledChaincode> oldInstalledChaincodes = queryInstalledChaincodes(peerCoreEnv);
        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-install.sh",
                peerCoreEnv.getMspId(),
                peerCoreEnv.getTlsRootCert().getAbsolutePath(),
                peerCoreEnv.getMspConfig().getAbsolutePath(),
                peerCoreEnv.getAddress(),
                chaincodePackage.getCanonicalPath());
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

        //检查链码批准情况
        List<ChaincodeApproval> oldCheckCommitReadiness = checkCommittedReadiness(ordererTlsEnv, peerCoreEnv, channelName, chaincodeProperties);
        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-approve.sh",
                peerCoreEnv.getMspId(),
                peerCoreEnv.getTlsRootCert().getAbsolutePath(),
                peerCoreEnv.getMspConfig().getAbsolutePath(),
                peerCoreEnv.getAddress(),
                packageId,
                chaincodeProperties.getName(),
                chaincodeProperties.getVersion(),
                chaincodeProperties.getSequence().toString(),
                channelName,
                ordererTlsEnv.getAddress(),
                ordererTlsEnv.getTlsRootCert().getAbsolutePath());
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

        String str = CommandUtils.exec(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-check-readiness.sh",
                peerCoreEnv.getMspId(),
                peerCoreEnv.getTlsRootCert().getAbsolutePath(),
                peerCoreEnv.getMspConfig().getAbsolutePath(),
                peerCoreEnv.getAddress(),
                chaincodeProperties.getName(),
                chaincodeProperties.getVersion(),
                chaincodeProperties.getSequence().toString(),
                channelName,
                ordererTlsEnv.getAddress(),
                ordererTlsEnv.getTlsRootCert().getAbsolutePath());
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
        List<String> commandList = Arrays.asList(
                MyFileUtils.getWorkingDir() + "/shell/fabric-chaincode-commit.sh",
                chaincodeProperties.getName(),
                chaincodeProperties.getVersion(),
                chaincodeProperties.getSequence().toString(),
                channelName,
                ordererTlsEnv.getAddress(),
                ordererTlsEnv.getTlsRootCert().getAbsolutePath(),
                committerPeerCoreEnv.getMspId(),
                committerPeerCoreEnv.getMspConfig().getAbsolutePath(),
                committerPeerCoreEnv.getAddress(),
                committerPeerCoreEnv.getTlsRootCert().getAbsolutePath()
        );
        for (TlsEnv otherPeerTlsEnv : endorserPeerTlsEnvs) {
            commandList.add(otherPeerTlsEnv.getAddress());
            commandList.add(otherPeerTlsEnv.getTlsRootCert().getAbsolutePath());
        }
        String[] commands = new String[commandList.size()];
        commandList.toArray(commands);

        List<CommittedChaincode> oldCommittedChaincodes = queryCommittedChaincodes(channelName, committerPeerCoreEnv);
        String str = CommandUtils.exec(commands);
        List<CommittedChaincode> newCommittedChaincodes = queryCommittedChaincodes(channelName, committerPeerCoreEnv);
        if (oldCommittedChaincodes.equals(newCommittedChaincodes)) {
            throw new ChaincodeException("提交失败：" + str);
        }
    }
}
