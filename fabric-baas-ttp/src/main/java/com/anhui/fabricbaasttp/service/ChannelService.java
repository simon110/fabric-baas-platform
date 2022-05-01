package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.*;
import com.anhui.fabricbaascommon.fabric.ChannelUtils;
import com.anhui.fabricbaascommon.fabric.ConfigtxUtils;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.MinioService;
import com.anhui.fabricbaascommon.util.*;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.bean.Peer;
import com.anhui.fabricbaasttp.constant.MinioBucket;
import com.anhui.fabricbaasttp.entity.ChannelEntity;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.repository.ChannelRepo;
import com.anhui.fabricbaasttp.util.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TODO: 对传入通道名称不能为系统通道名称
 * TODO: 让不同网络里面的通道可以重名
 */
@Service
@Slf4j
public class ChannelService {
    @Autowired
    private NetworkService networkService;
    @Autowired
    private ChannelRepo channelRepo;
    @Autowired
    private MinioService minioService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private FabricEnvService fabricEnvService;

    private void assertOrganizationInChannel(ChannelEntity channel, String organizationName, boolean expected) throws OrganizationException {
        if (channel.getOrganizationNames().contains(organizationName) != expected) {
            throw new OrganizationException("组织在通道中的存在不符合断言：" + organizationName);
        }
    }

    public ChannelEntity findChannelOrThrowEx(String channelName) throws ChannelException {
        Optional<ChannelEntity> channelOptional = channelRepo.findById(channelName);
        if (channelOptional.isEmpty()) {
            throw new ChannelException("未找到相应名称的通道：" + channelName);
        }
        return channelOptional.get();
    }

    private void assertPeerInChannel(ChannelEntity channel, Node peer, boolean expected) throws NodeException {
        Set<String> set = new HashSet<>();
        channel.getPeers().forEach(p -> set.add(p.getAddr()));
        String addr = peer.getAddr();
        if (set.contains(addr) != expected) {
            throw new NodeException("Peer在通道中的存在不符合断言：" + addr);
        }
    }

    private void assertPeerOwnership(ChannelEntity channel, Node peer, String organizationName) throws NodeException {
        String targetAddr = peer.getAddr();
        boolean result = false;
        List<Peer> peers = channel.getPeers();
        for (int i = 0; i < peers.size() && !result; i++) {
            Peer channelPeer = peers.get(i);
            result = channelPeer.getAddr().equals(targetAddr) && organizationName.equals(channelPeer.getOrganizationName());
        }
        if (!result) {
            throw new NodeException("Peer节点不属于该组织：" + organizationName);
        }
    }

    private void signEnvelopeWithOrganizations(String networkName, List<String> organizationNames, File envelope) throws IOException, InterruptedException, EnvelopeException {
        for (String organizationName : organizationNames) {
            MspEnv organizationMspDir = fabricEnvService.buildPeerMspEnv(networkName, organizationName);
            log.info("正在使用{}在{}网络的管理员证书对Envelope进行签名：{}", organizationName, networkName, envelope.getAbsolutePath());
            ChannelUtils.signEnvelope(organizationMspDir, envelope);
        }
    }

    private void assertInvitationCodes(List<String> invitationCodes, List<String> invitorOrgNames, String inviteeOrgName, String channelName) throws Exception {
        Set<String> actualInvitorSet = new HashSet<>();
        Set<String> givenInvitorSet = new HashSet<>(invitorOrgNames);
        log.info("邀请码应包含组织：" + givenInvitorSet);
        for (String invitationCode : invitationCodes) {
            Invitation invitation = InvitationUtils.parseCode(invitationCode);
            if (!invitation.getChannelName().equals(channelName)) {
                throw new InvitationException("邀请码所包含的通道名称错误");
            }
            if (!invitation.getInviteeOrgName().equals(inviteeOrgName)) {
                throw new InvitationException("邀请码所包含的受邀组织错误");
            }
            // TODO: 增加时间检查
            actualInvitorSet.add(invitation.getInvitorOrgName());
        }
        log.info("邀请码实际包含的组织：" + actualInvitorSet);
        if (!actualInvitorSet.equals(givenInvitorSet)) {
            throw new InvitationException("必须包含所有通道中的组织的邀请码");
        }
    }

    public void createChannel(String currentOrganizationName, String channelName, String networkName) throws Exception {
        // 检查操作的组织是否属于相应的网络
        NetworkEntity network = networkService.findNetworkOrThrowEx(networkName);
        networkService.assertOrganizationInNetwork(network, currentOrganizationName, true);

        // 检查相同名称的通道是否已经存在
        if (channelRepo.existsById(channelName)) {
            throw new ChannelException("相同名称的通道已存在");
        }

        // 默认配置该网络中当前的所有Orderer作为排序节点
        String organizationCertfileId = IdentifierGenerator.generateCertfileId(networkName, currentOrganizationName);
        File organizationCertfileDir = CertfileUtils.getCertfileDir(organizationCertfileId, CertfileType.ADMIN);
        ConfigtxOrganization ordererConfigtxOrg = new ConfigtxOrganization(
                caClientService.getCaOrganizationName(),
                caClientService.getCaOrganizationName(),
                CertfileUtils.getMspDir(caClientService.getRootCertfileDir())
        );
        log.info("生成Orderer组织的配置：" + ordererConfigtxOrg);

        ConfigtxOrganization currentConfigtxOrg = new ConfigtxOrganization(
                currentOrganizationName,
                currentOrganizationName,
                CertfileUtils.getMspDir(organizationCertfileDir)
        );
        List<ConfigtxOrganization> configtxOrganizations = Collections.singletonList(currentConfigtxOrg);
        log.info("生成当前组织的配置：" + currentConfigtxOrg);

        List<ConfigtxOrderer> configtxOrderers = networkService.generateConfigtxOrderers(network.getOrderers());
        log.info("生成通道的Orderer节点配置：" + configtxOrderers);

        // 生成configtx.yaml配置文件
        File configtxDir = SimpleFileUtils.createTempDir();
        File configtxYaml = new File(configtxDir + "/configtx.yaml");
        ConfigtxUtils.generateConfigtx(configtxYaml, network.getConsortiumName(), configtxOrderers, ordererConfigtxOrg, configtxOrganizations);
        log.info("生成基本通道配置文件：" + configtxYaml.getAbsolutePath());
        ConfigtxUtils.appendChannelToConfigtx(configtxYaml, channelName, Collections.singletonList(currentOrganizationName));
        log.info("将新通道加入配置文件：" + channelName);


        // TODO: 选择后面动态加入的Orderer可会引发报错 Error: got unexpected status: SERVICE_UNAVAILABLE -- channel syschannel is not serviced by me
        Orderer orderer = network.getOrderers().get(0);
        log.info("从网络中选择Orderer：" + orderer);

        // 创建通道
        MspEnv organizationMspEnv = fabricEnvService.buildPeerMspEnv(network.getName(), currentOrganizationName);
        TlsEnv ordererTlsEnv = fabricEnvService.buildOrdererTlsEnv(orderer);
        log.info("生成组织的MSP环境变量：" + organizationMspEnv);
        log.info("生成Orderer的TLS环境变量：" + ordererTlsEnv);
        File appChannelGenesis = SimpleFileUtils.createTempFile("block");
        ChannelUtils.createChannel(organizationMspEnv, ordererTlsEnv, configtxDir, channelName, appChannelGenesis);

        // 将通道信息保存至MongoDB
        ChannelEntity channel = new ChannelEntity();
        channel.setName(channelName);
        channel.setNetworkName(networkName);
        channel.setOrganizationNames(Collections.singletonList(currentOrganizationName));
        channel.setPeers(Collections.emptyList());
        channelRepo.save(channel);
        log.info("保存通道信息：" + channel);
    }

    public CoreEnv fetchChannelConfig(ChannelEntity channel, File config) throws NetworkException, CaException, IOException, InterruptedException, ChannelException {
        List<Orderer> orderers = networkService.getNetworkOrderers(channel.getNetworkName());
        assert !orderers.isEmpty();
        // Orderer orderer = RandomUtils.select(orderers);
        Orderer orderer = orderers.get(0);
        log.info("选择Orderer节点：" + orderer);
        CoreEnv ordererCoreEnv = fabricEnvService.buildOrdererCoreEnv(orderer);
        log.info("生成Orderer环境变量：" + ordererCoreEnv);
        ChannelUtils.fetchConfig(ordererCoreEnv, channel.getName(), config);
        log.info("拉取通道配置：" + channel.getName());
        return ordererCoreEnv;
    }

    public CoreEnv fetchChannelGenesis(ChannelEntity channel, File genesis) throws NetworkException, IOException, InterruptedException, ChannelException, CaException {
        // 随机选择一个Orderer
        List<Orderer> orderers = networkService.getNetworkOrderers(channel.getNetworkName());
        Orderer orderer = RandomUtils.select(orderers);
        log.info("随机选择Orderer节点：" + orderer);

        // 拉取通道的启动区块
        CoreEnv ordererCoreEnv = fabricEnvService.buildOrdererCoreEnv(orderer);
        log.info("生成Orderer的环境变量参数：" + ordererCoreEnv);
        ChannelUtils.fetchGenesisBlock(ordererCoreEnv, channel.getName(), genesis);
        log.info("拉取通道{}的启动区块：{}", channel.getName(), genesis.getAbsolutePath());
        return ordererCoreEnv;
    }

    public void submitInvitationCodes(String currentOrganizationName, String channelName, List<String> invitationCodes) throws Exception {
        ChannelEntity channel = findChannelOrThrowEx(channelName);
        assertOrganizationInChannel(channel, currentOrganizationName, false);

        // 对邀请码进行验证
        assertInvitationCodes(invitationCodes, channel.getOrganizationNames(), currentOrganizationName, channel.getName());

        // 拉取指定通道的配置
        File oldChannelConfig = SimpleFileUtils.createTempFile("json");
        CoreEnv ordererCoreEnv = fetchChannelConfig(channel, oldChannelConfig);

        // 生成新组织的配置
        File newOrgConfigtxDir = SimpleFileUtils.createTempDir();
        File newOrgConfigtxYaml = new File(newOrgConfigtxDir + "/configtx.yaml");
        File newOrgConfigtxJson = SimpleFileUtils.createTempFile("json");
        String newOrgCertfileId = IdentifierGenerator.generateCertfileId(channel.getNetworkName(), currentOrganizationName);
        File newOrgCertfileDir = CertfileUtils.getCertfileDir(newOrgCertfileId, CertfileType.ADMIN);

        ConfigtxOrganization configtxOrganization = new ConfigtxOrganization();
        configtxOrganization.setId(currentOrganizationName);
        configtxOrganization.setName(currentOrganizationName);
        configtxOrganization.setMspDir(new File(newOrgCertfileDir + "/msp"));
        ConfigtxUtils.generateOrgConfigtx(newOrgConfigtxYaml, configtxOrganization);
        log.info("生成新组织的配置：" + newOrgConfigtxYaml.getAbsolutePath());
        ConfigtxUtils.convertOrgConfigtxToJson(newOrgConfigtxJson, newOrgConfigtxDir, currentOrganizationName);
        log.info("将新组织的转换为：" + newOrgConfigtxJson.getAbsolutePath());

        // 对通道配置文件进行更新并生成Envelope
        File newChannelConfig = SimpleFileUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendOrganizationToAppChannelConfig(currentOrganizationName, newOrgConfigtxJson, newChannelConfig);
        log.info("将新组织添加到现有的通道配置中：" + newChannelConfig.getAbsolutePath());
        File envelope = SimpleFileUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(channel.getName(), envelope, oldChannelConfig, newChannelConfig);
        log.info("生成向通道添加组织的Envelope：" + envelope.getAbsolutePath());

        // 使用该通道中所有组织的身份对Envelope进行签名（包括未加入的组织）
        List<String> channelOrganizationNames = channel.getOrganizationNames();
        List<String> signerOrgNames = channelOrganizationNames.subList(0, channelOrganizationNames.size() - 1);
        signEnvelopeWithOrganizations(channel.getNetworkName(), signerOrgNames, envelope);

        // 将Envelope提交到Orderer
        // 注意不能用未在通道中的组织的身份来提交通道更新，即使通道中的组织全都签名了也依然会报错
        // 提交更新的组织会同时进行签名，所以提交更新的组织不用参与签名的签名过程
        String lastChannelOrgName = channelOrganizationNames.get(channelOrganizationNames.size() - 1);
        MspEnv organizationMspEnv = fabricEnvService.buildPeerMspEnv(channel.getNetworkName(), lastChannelOrgName);
        log.info("生成提交Envelope的组织的MSP环境变量：" + organizationMspEnv);
        ChannelUtils.submitChannelUpdate(organizationMspEnv, ordererCoreEnv.getTlsEnv(), channel.getName(), envelope);

        // 将组织保存至MongoDB
        channel.getOrganizationNames().add(currentOrganizationName);
        log.info("更新通道信息：" + channel);
        channelRepo.save(channel);
    }

    public void joinChannel(String currentOrganizationName, String channelName, Node peer, MultipartFile peerCertZip) throws Exception {
        ChannelEntity channel = findChannelOrThrowEx(channelName);

        // 对证书格式进行检查
        CertfileUtils.assertCertfileZip(peerCertZip);

        // 检查Peer是否在通道中且为该组织的节点
        assertPeerInChannel(channel, peer, false);

        // 生成新Peer的信息
        String newPeerId = IdentifierGenerator.generatePeerId(channel.getName(), peer);
        Peer newPeer = new Peer();
        newPeer.setName(newPeerId);
        newPeer.setHost(peer.getHost());
        newPeer.setPort(peer.getPort());
        newPeer.setOrganizationName(currentOrganizationName);
        log.info("生成新Peer信息：" + newPeer);

        File channelGenesisBlock = SimpleFileUtils.createTempFile("block");
        fetchChannelGenesis(channel, channelGenesisBlock);

        // 解压Peer证书到临时目录
        File peerCertfileZip = SimpleFileUtils.createTempFile("zip");
        FileUtils.writeByteArrayToFile(peerCertfileZip, peerCertZip.getBytes());
        log.info("将用户上传的Peer证书保存至：" + peerCertfileZip.getAbsolutePath());
        File peerCertfileDir = SimpleFileUtils.createTempDir();
        ZipUtils.unzip(peerCertfileZip, peerCertfileDir);
        log.info("将Peer证书解压到：" + peerCertfileDir.getAbsolutePath());
        CertfileUtils.assertCertfile(peerCertfileDir);

        // 尝试将Peer加入到通道中（TLS环境变量需要改为临时目录中的，因为还没保存到正式目录）
        CoreEnv peerCoreEnv = fabricEnvService.buildPeerCoreEnv(channel.getNetworkName(), currentOrganizationName, newPeer);
        peerCoreEnv.setTlsRootCert(CertfileUtils.getTlsCaCert(peerCertfileDir));
        log.info("生成Peer的环境变量：" + peerCoreEnv);
        ChannelUtils.joinChannel(peerCoreEnv, channelGenesisBlock);

        // 将Peer证书保存到MinIO和证书目录
        minioService.putBytes(MinioBucket.PEER_CERTFILE_BUCKET_NAME, newPeer.getName(), peerCertZip.getBytes());
        log.info("将用户上传的Peer证书保存至MinIO：" + peerCertZip.getOriginalFilename());
        File formalPeerCertfileDir = CertfileUtils.getCertfileDir(newPeer.getName(), CertfileType.PEER);
        ZipUtils.unzip(peerCertfileZip, formalPeerCertfileDir);
        log.info("将Peer证书保存到正式目录：" + formalPeerCertfileDir.getAbsolutePath());

        // 更新MongoDB中的信息
        channel.getPeers().add(newPeer);
        channelRepo.save(channel);
        log.info("更新通道信息：" + channel);
    }

    public String generateInvitationCode(String currentOrganizationName, String channelName, String inviteeOrganizationName) throws Exception {
        // 检查组织是否是通道的合法成员
        ChannelEntity channel = findChannelOrThrowEx(channelName);
        assertOrganizationInChannel(channel, currentOrganizationName, true);
        assertOrganizationInChannel(channel, inviteeOrganizationName, false);

        // 检查被邀请的组织是否是合法的网络成员
        NetworkEntity network = networkService.findNetworkOrThrowEx(channel.getNetworkName());
        networkService.assertOrganizationInNetwork(network, currentOrganizationName, true);
        networkService.assertOrganizationInNetwork(network, inviteeOrganizationName, true);

        // 生成邀请码
        Invitation invitation = new Invitation();
        invitation.setInvitorOrgName(currentOrganizationName);
        invitation.setInviteeOrgName(inviteeOrganizationName);
        invitation.setChannelName(channelName);
        invitation.setTimestamp(System.currentTimeMillis());
        log.info("生成邀请信息：" + invitation);
        String invitationCode = InvitationUtils.getCode(invitation);
        log.info("生成邀请码：" + invitationCode);
        return invitationCode;
    }

    public void setAnchorPeer(String currentOrganizationName, String channelName, Node peer) throws Exception {
        ChannelEntity channel = findChannelOrThrowEx(channelName);
        assertOrganizationInChannel(channel, currentOrganizationName, true);

        // 检查Peer是否在通道中且为该组织的节点

        assertPeerInChannel(channel, peer, true);
        assertPeerOwnership(channel, peer, currentOrganizationName);

        // 从通道中随机选择一个Orderer
        Orderer orderer = RandomUtils.select(networkService.getNetworkOrderers(channel.getNetworkName()));
        log.info("随机选择Orderer节点：" + orderer);

        // 拉取指定通道的配置
        File oldChannelConfig = SimpleFileUtils.createTempFile("json");
        CoreEnv ordererCoreEnv = fetchChannelConfig(channel, oldChannelConfig);

        // 对通道配置文件进行更新并生成Envelope
        File newChannelConfig = SimpleFileUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendAnchorPeerToChannelConfig(peer, currentOrganizationName, oldChannelConfig);
        log.info("将锚节点添加到通道配置中：" + newChannelConfig.getAbsolutePath());

        File envelope = SimpleFileUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(channel.getName(), envelope, oldChannelConfig, newChannelConfig);
        log.info("生成更新锚节点的Envelope：" + envelope.getAbsolutePath());

        // 将Envelope提交到Orderer
        MspEnv organizationMspEnv = fabricEnvService.buildPeerMspEnv(channel.getNetworkName(), currentOrganizationName);
        ChannelUtils.submitChannelUpdate(organizationMspEnv, ordererCoreEnv.getTlsEnv(), channel.getName(), envelope);
    }

    public String queryPeerTlsCert(String currentOrganizationName, String channelName, Node peer) throws Exception {
        // 检查通道是否存在
        ChannelEntity channel = findChannelOrThrowEx(channelName);
        // 检查当前组织是否位于该通道中
        assertOrganizationInChannel(channel, currentOrganizationName, true);

        String peerId = IdentifierGenerator.generatePeerId(channelName, peer);
        // 检查Orderer证书
        File ordererCertfileDir = CertfileUtils.getCertfileDir(peerId, CertfileType.PEER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        File ordererTlsCert = CertfileUtils.getTlsCaCert(ordererCertfileDir);
        return ResourceUtils.release(ordererTlsCert, "crt");
    }

    public List<Peer> queryPeers(String channelName) throws Exception {
        return findChannelOrThrowEx(channelName).getPeers();
    }
}
