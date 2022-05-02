package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.ConfigtxOrderer;
import com.anhui.fabricbaascommon.bean.ConfigtxOrganization;
import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.constant.ApplStatus;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.*;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.fabric.ChannelUtils;
import com.anhui.fabricbaascommon.fabric.ConfigtxUtils;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.MinioService;
import com.anhui.fabricbaascommon.util.*;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.constant.MinioBucket;
import com.anhui.fabricbaasttp.entity.ChannelEntity;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.entity.ParticipationEntity;
import com.anhui.fabricbaasttp.repository.ChannelRepo;
import com.anhui.fabricbaasttp.repository.NetworkRepo;
import com.anhui.fabricbaasttp.repository.ParticipationRepo;
import com.anhui.fabricbaasttp.util.IdentifierGenerator;
import com.spotify.docker.client.exceptions.NodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class NetworkService {
    @Autowired
    private MinioService minioService;
    @Autowired
    private NetworkRepo networkRepo;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private ParticipationRepo participationRepo;
    @Autowired
    private FabricConfiguration fabricConfig;
    @Autowired
    private FabricEnvService fabricEnvService;
    @Autowired
    private ChannelRepo channelRepo;

    public void assertOrganizationInNetwork(NetworkEntity network, String organizationName, boolean expected) throws OrganizationException {
        if (network.getOrganizationNames().contains(organizationName) == expected) {
            throw new OrganizationException("组织在网络中的存在性不符合断言：" + organizationName);
        }
    }

    private static void assertOrdererInNetwork(NetworkEntity network, Node orderer, boolean expected) throws NodeException {
        Set<String> set = new TreeSet<>();
        network.getOrderers().forEach(o -> set.add(o.getAddr()));
        String addr = orderer.getAddr();
        if (set.contains(addr) != expected) {
            throw new NodeException("Orderer在网络中的存在不符合断言：" + addr);
        }
    }

    private static void assertOrderersNotDuplicated(List<Node> orderers) throws NodeException {
        Set<String> set = new TreeSet<>();
        for (Node node : orderers) {
            String addr = node.getAddr();
            if (set.contains(addr)) {
                throw new NodeException("存在重复的Orderer地址：" + addr);
            }
            set.add(addr);
        }
    }

    /**
     * 如果网络存在则返回相应的实体，否则抛出异常。
     */
    public NetworkEntity findNetworkOrThrowEx(String networkName) throws NetworkException {
        Optional<NetworkEntity> networkOptional = networkRepo.findById(networkName);
        if (networkOptional.isEmpty()) {
            throw new NetworkException("不存在相应名称的网络");
        }
        return networkOptional.get();
    }

    private String findOrdererOrganizationNameOrThrowEx(NetworkEntity network, Node orderer) throws NodeNotFoundException {
        String targetAddr = orderer.getAddr();
        List<Orderer> orderers = network.getOrderers();
        for (Orderer endpoint : orderers) {
            if (endpoint.getAddr().equals(targetAddr)) {
                return endpoint.getOrganizationName();
            }
        }
        throw new NodeNotFoundException("未找到相应的Orderer：" + targetAddr);
    }

    private ParticipationEntity findUnhandledParticipationOrThrowEx(String networkName, String organizationName) throws ParticipationException {
        Optional<ParticipationEntity> participationOptional = participationRepo.findFirstByNetworkNameAndOrganizationNameAndStatus(networkName, organizationName, ApplStatus.UNHANDLED);
        if (participationOptional.isEmpty()) {
            throw new ParticipationException("该组织不存在待处理的加入网络申请：" + organizationName);
        }
        return participationOptional.get();
    }

    public CoreEnv fetchSystemChannelConfig(NetworkEntity network, File config) throws CaException, IOException, InterruptedException, ChannelException {
        // Orderer orderer = RandomUtils.select(network.getOrderers());
        Orderer orderer = network.getOrderers().get(0);
        log.info("从网络中选择了Orderer：" + orderer.getAddr());
        CoreEnv ordererCoreEnv = fabricEnvService.buildOrdererCoreEnv(orderer);
        log.info("生成Orderer的环境变量：" + ordererCoreEnv);
        ChannelUtils.fetchConfig(ordererCoreEnv, fabricConfig.getSystemChannelName(), config);
        log.info("从网络的系统通道拉取配置：" + config.getAbsolutePath());
        return ordererCoreEnv;
    }

    private void addOrganizationToConsortium(NetworkEntity network, String newOrgName) throws Exception {
        // 从Orderer拉取通道配置
        File oldConfig = MyFileUtils.createTempFile("json");
        File newConfig = MyFileUtils.createTempFile("json");
        File orgConfig = MyFileUtils.createTempFile("json");
        File newOrgConfigtxDir = MyFileUtils.createTempDir();
        File newOrgConfigtxYaml = new File(newOrgConfigtxDir + "/configtx.yaml");
        File newOrgCertfileZip = MyFileUtils.createTempFile("zip");
        File newOrgCertfileDir = MyFileUtils.createTempDir();
        File envelope = MyFileUtils.createTempFile("pb");

        // 拉取网络系统通道的配置文件
        CoreEnv ordererCoreEnv = fetchSystemChannelConfig(network, oldConfig);

        // 从MinIO下载新组织的证书并解压
        String newOrgCertfileId = IdentifierGenerator.generateCertfileId(network.getName(), newOrgName);
        minioService.getAsFile(MinioBucket.ADMIN_CERTFILE_BUCKET_NAME, newOrgCertfileId, newOrgCertfileZip);
        ZipUtils.unzip(newOrgCertfileZip, newOrgCertfileDir);
        log.info("将组织的管理员证书解压到了临时目录：" + newOrgCertfileDir);

        // 生成新组织的配置的JSON
        ConfigtxOrganization newConfigtxOrganization = new ConfigtxOrganization();
        newConfigtxOrganization.setName(newOrgName);
        newConfigtxOrganization.setId(newOrgName);
        newConfigtxOrganization.setMspDir(CertfileUtils.getMspDir(newOrgCertfileDir));
        log.info("新组织的配置信息：" + newConfigtxOrganization);

        ConfigtxUtils.generateOrgConfigtx(newOrgConfigtxYaml, newConfigtxOrganization);
        log.info("生成组织的配置文件：" + newOrgConfigtxYaml.getAbsolutePath());
        ConfigtxUtils.convertOrgConfigtxToJson(orgConfig, newOrgConfigtxDir, newOrgName);
        log.info("将组织的配置文件转换为：" + orgConfig.getAbsolutePath());

        // 将新组织信息写入拉取下来的通道配置中
        FileUtils.copyFile(oldConfig, newConfig);
        ChannelUtils.appendOrganizationToSysChannelConfig(newOrgName, orgConfig, newConfig);
        log.info("将新组织的配置合并到原有配置文件中：" + newConfig.getAbsolutePath());

        // 对比新旧配置文件生成Envelope，并对Envelope进行签名
        log.info("正在生成提交到通道的Envelope并签名：" + envelope.getAbsolutePath());
        ChannelUtils.generateEnvelope(fabricConfig.getSystemChannelName(), envelope, oldConfig, newConfig);
        // ChannelUtils.signEnvelope(ordererCoreEnv.getMspEnv(), envelope);

        // 将签名后的Envelope提交到Orderer
        log.info("正在将Envelope提交到系统通道：" + fabricConfig.getSystemChannelName());
        ChannelUtils.submitChannelUpdate(ordererCoreEnv.getMspEnv(), ordererCoreEnv.getTlsEnv(), fabricConfig.getSystemChannelName(), envelope);
    }

    public List<Orderer> getNetworkOrderers(String networkName) throws NetworkException {
        return findNetworkOrThrowEx(networkName).getOrderers();
    }

    public String queryOrdererTlsCert(String currentOrganizationName, String networkName, Node orderer) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        assertOrdererInNetwork(network, orderer, true);
        assertOrganizationInNetwork(network, currentOrganizationName, true);

        // 检查Orderer证书
        String ordererId = IdentifierGenerator.generateOrdererId(networkName, orderer);
        File ordererCertfileDir = CertfileUtils.getCertfileDir(ordererId, CertfileType.ORDERER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        File ordererTlsCert = CertfileUtils.getTlsCaCert(ordererCertfileDir);
        return MyResourceUtils.release(ordererTlsCert, "crt");
    }

    public String queryOrdererCert(String currentOrganizationName, String networkName, Node orderer) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        // 检查当前组织是否位于该网络中
        assertOrganizationInNetwork(network, currentOrganizationName, true);

        // 检查Orderer是否属于该组织
        String ordererOrgName = findOrdererOrganizationNameOrThrowEx(network, orderer);
        if (!ordererOrgName.equals(currentOrganizationName)) {
            throw new OrganizationException("不允许下载其他组织的Orderer证书！");
        }
        String ordererId = IdentifierGenerator.generateOrdererId(networkName, orderer);

        // 检查Orderer证书
        File ordererCertfileDir = CertfileUtils.getCertfileDir(ordererId, CertfileType.ORDERER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        File ordererCertfileZip = MyFileUtils.createTempFile("zip");
        ZipUtils.zip(ordererCertfileZip,
                CertfileUtils.getMspDir(ordererCertfileDir),
                CertfileUtils.getTlsDir(ordererCertfileDir)
        );
        return MyResourceUtils.release(ordererCertfileZip, "zip");
    }

    public String addOrderer(String currentOrganizationName, String networkName, Node orderer) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        // 检查当前组织是否位于该网络中
        assertOrganizationInNetwork(network, currentOrganizationName, true);
        // 检查Orderer节点是否已经存在于网络中
        assertOrdererInNetwork(network, orderer, false);

        // 为新Orderer注册证书并登记
        String caUsername = IdentifierGenerator.generateOrdererId(network.getName(), orderer);
        String caPassword = PasswordUtils.generate();
        caClientService.register(caUsername, caPassword, CertfileType.ORDERER);

        List<String> csrHosts = CaUtils.buildCsrHosts(orderer.getHost());
        File newOrdererCertfileDir = MyFileUtils.createTempDir();
        caClientService.enroll(newOrdererCertfileDir, caUsername, csrHosts);
        log.info("将证书登记到临时目录：" + newOrdererCertfileDir.getAbsolutePath());

        File oldChannelConfig = MyFileUtils.createTempFile("json");
        CoreEnv selectedOrdererCoreEnv = fetchSystemChannelConfig(network, oldChannelConfig);

        // 向通道配置文件中添加新Orderer的定义
        ConfigtxOrderer configtxOrderer = new ConfigtxOrderer();
        configtxOrderer.setHost(orderer.getHost());
        configtxOrderer.setPort(orderer.getPort());
        File newOrdererTlsServerCert = CertfileUtils.getTlsServerCert(newOrdererCertfileDir);
        configtxOrderer.setClientTlsCert(newOrdererTlsServerCert);
        configtxOrderer.setServerTlsCert(newOrdererTlsServerCert);
        log.info("生成新Orderer节点：" + configtxOrderer);

        File newChannelConfig = MyFileUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendOrdererToChannelConfig(configtxOrderer, newChannelConfig);
        log.info("将新的Orderer信息添加到通道配置：" + newChannelConfig.getAbsolutePath());

        // 计算新旧JSON配置文件之间的差异得到Envelope，并对其进行签名
        File envelope = MyFileUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(fabricConfig.getSystemChannelName(), envelope, oldChannelConfig, newChannelConfig);
        // ChannelUtils.signEnvelope(selectedOrdererCoreEnv.getMspEnv(), envelope);
        log.info("生成并使用Orderer身份对Envelope文件进行签名：" + envelope.getAbsolutePath());

        // 将Envelope提交到现有的Orderer节点
        ChannelUtils.submitChannelUpdate(
                selectedOrdererCoreEnv.getMspEnv(),
                selectedOrdererCoreEnv.getTlsEnv(),
                fabricConfig.getSystemChannelName(),
                envelope
        );

        // 将新Orderer的分别保存到MinIO并复制到证书目录下
        File ordererCertfileZip = MyFileUtils.createTempFile("zip");
        ZipUtils.zip(ordererCertfileZip,
                CertfileUtils.getMspDir(newOrdererCertfileDir),
                CertfileUtils.getTlsDir(newOrdererCertfileDir)
        );


        File formalCertfileDir = CertfileUtils.getCertfileDir(caUsername, CertfileType.ORDERER);
        ZipUtils.unzip(ordererCertfileZip, formalCertfileDir);
        log.info("将新Orderer的证书解压到正式目录：" + formalCertfileDir);
        minioService.putFile(MinioBucket.ORDERER_CERTFILE_BUCKET_NAME, caUsername, ordererCertfileZip);
        log.info("正在上传新Orderer的证书到MinIO...");

        // 将更新后的信息保存到数据库
        Orderer newOrderer = new Orderer();
        newOrderer.setOrganizationName(currentOrganizationName);
        newOrderer.setHost(orderer.getHost());
        newOrderer.setPort(orderer.getPort());
        newOrderer.setCaUsername(caUsername);
        newOrderer.setCaPassword(caPassword);
        network.getOrderers().add(newOrderer);
        networkRepo.save(network);
        log.info("更新网络的信息：" + network);

        return MyResourceUtils.release(ordererCertfileZip, "zip");
    }

    public String createNetwork(
            String currentOrganizationName,
            String networkName,
            String consortiumName,
            List<Node> orderers,
            MultipartFile adminCertZip)
            throws Exception {
        // 检查是否存在重复的Orderer地址
        assertOrderersNotDuplicated(orderers);

        // 检查相同名称的网络是否已存在
        if (networkRepo.existsById(networkName)) {
            throw new NetworkException("相同名称的网络已经存在：" + networkName);
        }

        File orgCertfileDir = MyFileUtils.createTempDir();
        File organizationCertfileZip = MyFileUtils.createTempFile("zip");
        log.info("正在将组织{}上传的网络管理员证书写入到：{}", currentOrganizationName, organizationCertfileZip.getAbsolutePath());
        FileUtils.writeByteArrayToFile(organizationCertfileZip, adminCertZip.getBytes());
        log.info("正在将组织{}上传的网络管理员证书解压到：{}", currentOrganizationName, orgCertfileDir.getAbsolutePath());
        ZipUtils.unzip(organizationCertfileZip, orgCertfileDir);
        log.info("正在检查组织{}上传的网络管理员证书：{}", currentOrganizationName, orgCertfileDir.getAbsolutePath());
        CertfileUtils.assertCertfile(orgCertfileDir);

        List<ConfigtxOrderer> configtxOrderers = new ArrayList<>();
        List<Orderer> ordererEndpoints = new ArrayList<>();
        List<File> ordererCertfileDirs = new ArrayList<>();
        log.info("正在生成Orderer证书...");
        for (Node orderer : orderers) {
            // 注册并登记Orderer证书
            // 账户名称例如SampleNetwork-orgexamplecom-30500
            // 账户密码例如13473cf3bc515bccbb81fa235ed33ff9
            String caUsername = IdentifierGenerator.generateOrdererId(networkName, orderer);
            String caPassword = PasswordUtils.generate();
            caClientService.register(caUsername, caPassword, CertfileType.ORDERER);

            List<String> csrHosts = CaUtils.buildCsrHosts(orderer.getHost());
            File ordererCertfileDir = MyFileUtils.createTempDir();
            ordererCertfileDirs.add(ordererCertfileDir);
            caClientService.enroll(ordererCertfileDir, caUsername, csrHosts);

            // 增加configtx的Orderer定义
            // 注意此处的TLS证书路径是configtx.yaml的相对路径或绝对路径
            File tlsSeverCert = CertfileUtils.getTlsServerCert(ordererCertfileDir);
            ConfigtxOrderer configtxOrderer = new ConfigtxOrderer(orderer, tlsSeverCert);
            configtxOrderers.add(configtxOrderer);

            // 生成保存到数据库的Orderer信息
            Orderer node = new Orderer();
            node.setOrganizationName(currentOrganizationName);
            node.setCaUsername(caUsername);
            node.setCaPassword(caPassword);
            node.setHost(orderer.getHost());
            node.setPort(orderer.getPort());
            ordererEndpoints.add(node);
        }

        String ordererOrganizationName = caClientService.getCaOrganizationName();
        File rootCertfileMspDir = CertfileUtils.getMspDir(caClientService.getRootCertfileDir());
        File orgCertfileMspDir = CertfileUtils.getMspDir(orgCertfileDir);
        ConfigtxOrganization ordererConfigtxOrg = new ConfigtxOrganization(ordererOrganizationName, ordererOrganizationName, rootCertfileMspDir);
        ConfigtxOrganization currentConfigtxOrg = new ConfigtxOrganization(currentOrganizationName, currentOrganizationName, orgCertfileMspDir);
        List<ConfigtxOrganization> configtxOrganizations = Collections.singletonList(currentConfigtxOrg);
        log.info("生成组织配置：" + configtxOrganizations);

        File configtxDir = MyFileUtils.createTempDir();
        File configtxYaml = new File(configtxDir + "/configtx.yaml");
        ConfigtxUtils.generateConfigtx(configtxYaml, consortiumName, configtxOrderers, ordererConfigtxOrg, configtxOrganizations);
        log.info("生成配置文件：" + configtxYaml.getAbsolutePath());

        File sysChannelGenesis = MyFileUtils.createTempFile("block");
        ConfigtxUtils.generateGenesisBlock(ConfigtxUtils.ORDERER_GENESIS_NAME, fabricConfig.getSystemChannelName(), sysChannelGenesis, configtxDir);
        log.info("生成创世区块：" + sysChannelGenesis.getAbsolutePath());

        log.info("正在将组织{}上传的网络管理员证书保存至MinIO和证书目录", currentOrganizationName);
        String organizationCertfileId = IdentifierGenerator.generateCertfileId(networkName, currentOrganizationName);
        minioService.putBytes(MinioBucket.ADMIN_CERTFILE_BUCKET_NAME, organizationCertfileId, adminCertZip.getBytes());
        ZipUtils.unzip(organizationCertfileZip, CertfileUtils.getCertfileDir(organizationCertfileId, CertfileType.ADMIN));

        assert ordererCertfileDirs.size() == orderers.size();
        for (int i = 0, size = ordererCertfileDirs.size(); i < size; i++) {
            // 将证书压缩
            File ordererCertfileDir = ordererCertfileDirs.get(i);
            File ordererCertZip = MyFileUtils.createTempFile("zip");
            ZipUtils.zip(ordererCertZip,
                    CertfileUtils.getMspDir(ordererCertfileDir),
                    CertfileUtils.getTlsDir(ordererCertfileDir)
            );

            // 将证书保存到正式的证书目录和MinIO
            String ordererId = IdentifierGenerator.generateOrdererId(networkName, orderers.get(i));
            log.info("正在将网络的Orderer证书{}保存至MinIO和证书目录：", ordererId);
            ZipUtils.unzip(ordererCertZip, CertfileUtils.getCertfileDir(ordererId, CertfileType.ORDERER));
            minioService.putFile(MinioBucket.ORDERER_CERTFILE_BUCKET_NAME, ordererId, ordererCertZip);
        }

        // 保存网络信息到数据库
        NetworkEntity network = new NetworkEntity();
        network.setName(networkName);
        network.setConsortiumName(consortiumName);
        network.setOrderers(ordererEndpoints);
        network.setOrganizationNames(Collections.singletonList(currentOrganizationName));
        log.info("保存网络信息：" + network);
        networkRepo.save(network);

        ParticipationEntity participation = new ParticipationEntity();
        participation.setNetworkName(networkName);
        participation.setDescription("网络创建组织");
        participation.setStatus(ApplStatus.ACCEPTED);
        participation.setTimestamp(System.currentTimeMillis());
        participation.setOrganizationName(currentOrganizationName);
        participation.setApprovals(Collections.emptyList());
        log.info("保存网络参与权限：" + participation);
        participationRepo.save(participation);

        // 返回创世区块下载路径
        return MyResourceUtils.release(sysChannelGenesis, "block");
    }

    public Page<NetworkEntity> queryNetworks(String networkNameKeyword, String organizationNameKeyword, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        boolean isNetworkNameKeywordInvalid = networkNameKeyword == null || StringUtils.isBlank(networkNameKeyword);
        boolean isOrganizationNameKeyKeywordInvalid = organizationNameKeyword == null || StringUtils.isBlank(organizationNameKeyword);
        if (isNetworkNameKeywordInvalid && isOrganizationNameKeyKeywordInvalid) {
            return networkRepo.findAll(pageable);
        } else if (isOrganizationNameKeyKeywordInvalid) {
            // 按网络名称关键词进行搜索
            return networkRepo.findAllByNameLike(networkNameKeyword, pageable);
        } else {
            List<ParticipationEntity> allowedParticipations;
            if (isNetworkNameKeywordInvalid) {
                // 按组织名称关键词进行搜索
                allowedParticipations = participationRepo.findAllByOrganizationNameLikeAndStatus(organizationNameKeyword, ApplStatus.ACCEPTED);
            } else {
                // 同时根据组织名称和网络名称进行搜索
                allowedParticipations = participationRepo.findAllByNetworkNameLikeAndOrganizationNameLikeAndStatus(networkNameKeyword, organizationNameKeyword, ApplStatus.ACCEPTED);
            }
            Set<String> networkNameSet = new HashSet<>();
            allowedParticipations.forEach(participation -> networkNameSet.add(participation.getNetworkName()));
            List<String> networkNames = new ArrayList<>(networkNameSet);
            if (networkNames.size() > pageSize) {
                int startIndex = (page - 1) * pageSize;
                int endIndex = Integer.min(startIndex + pageSize, networkNames.size());
                networkNames = networkNames.subList(startIndex, endIndex);
            }
            List<NetworkEntity> networks = new ArrayList<>(networkNames.size());
            networkNames.forEach(networkName -> {
                Optional<NetworkEntity> networkOptional = networkRepo.findById(networkName);
                assert networkOptional.isPresent();
                networks.add(networkOptional.get());
            });
            return new PageImpl<>(networks, pageable, networkNameSet.size());
        }
    }

    public void applyParticipation(
            String currentOrganizationName,
            String networkName,
            String description,
            MultipartFile adminCertZip)
            throws Exception {
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        // 检查是否存在未处理的加入网络申请
        Optional<ParticipationEntity> participationOptional = participationRepo.findFirstByNetworkNameAndOrganizationNameAndStatus(networkName, currentOrganizationName, ApplStatus.UNHANDLED);
        if (participationOptional.isPresent()) {
            throw new DuplicatedOperationException("该组织存在未处理的加入该网络的申请");
        }
        // 检查组织是否已经在网络中
        assertOrganizationInNetwork(network, currentOrganizationName, false);
        // 检查证书格式是否正确
        CertfileUtils.assertCertfileZip(adminCertZip);
        // 保存管理员证书至MinIO
        String orgCertfileId = IdentifierGenerator.generateCertfileId(networkName, currentOrganizationName);
        log.info("正在将证书文件保存至MinIO：" + orgCertfileId);
        minioService.putBytes(MinioBucket.ADMIN_CERTFILE_BUCKET_NAME, orgCertfileId, adminCertZip.getBytes());

        // 将加入网络申请保存到MongoDB
        ParticipationEntity participation = new ParticipationEntity();
        participation.setNetworkName(networkName);
        participation.setDescription(description);
        participation.setStatus(ApplStatus.UNHANDLED);
        participation.setTimestamp(System.currentTimeMillis());
        participation.setOrganizationName(currentOrganizationName);
        participation.setApprovals(Collections.emptyList());
        log.info("正在将申请信息保存到数据库：" + participation);
        participationRepo.save(participation);
    }

    public void handleParticipation(String currentOrganizationName, String networkName, String applierOrganizationName, boolean isAllowed) throws Exception {
        // 找到相应的申请
        ParticipationEntity participation = findUnhandledParticipationOrThrowEx(networkName, applierOrganizationName);
        NetworkEntity network = findNetworkOrThrowEx(networkName);

        // 确定操作者是该网络中的成员
        assertOrganizationInNetwork(network, currentOrganizationName, true);
        assertOrganizationInNetwork(network, applierOrganizationName, false);

        // 确定操作者没有重复操作
        if (participation.getApprovals().contains(currentOrganizationName)) {
            throw new DuplicatedOperationException("请勿重复操作");
        }
        log.info("当前网络中已经同意申请的组织包括：" + participation.getApprovals());

        if (isAllowed) {
            // 如果操作者赞成
            List<String> approvals = participation.getApprovals();
            approvals.add(currentOrganizationName);
            if (approvals.size() == network.getOrganizationNames().size()) {
                addOrganizationToConsortium(network, applierOrganizationName);
                // 如果达到条件则更新系统通道配置
                participation.setStatus(ApplStatus.ACCEPTED);
                participation.setTimestamp(System.currentTimeMillis());
                // 将证书解压到指定目录
                File certfileZip = MyFileUtils.createTempFile("zip");
                String certfileId = IdentifierGenerator.generateCertfileId(network.getName(), applierOrganizationName);
                minioService.getAsFile(MinioBucket.ADMIN_CERTFILE_BUCKET_NAME, certfileId, certfileZip);
                ZipUtils.unzip(certfileZip, CertfileUtils.getCertfileDir(certfileId, CertfileType.ADMIN));
                // 将组织加入到网络
                network.getOrganizationNames().add(applierOrganizationName);
                // TODO: 发送邮件通知
            }
        } else {
            // 如果操作者拒绝则直接全盘否决
            participation.setStatus(ApplStatus.REJECTED);
        }
        // 更新MongoDB中的数据
        log.info("正在更新网络参与权信息：" + participation);
        participationRepo.save(participation);

        log.info("正在更新网络信息：" + network);
        networkRepo.save(network);
    }

    public Page<ParticipationEntity> queryParticipations(String networkName, int status, int page, int pageSize) throws NetworkException {
        findNetworkOrThrowEx(networkName);
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return participationRepo.findAllByNetworkNameAndStatus(networkName, status, pageable);
    }

    public String queryGenesisBlock(String currentOrganizationName, String networkName) throws Exception {
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        assertOrganizationInNetwork(network, currentOrganizationName, true);

        Orderer orderer = network.getOrderers().get(0);
        CoreEnv ordererCoreEnv = fabricEnvService.buildOrdererCoreEnv(orderer);

        File block = MyFileUtils.createTempFile("block");
        ChannelUtils.fetchGenesisBlock(ordererCoreEnv, fabricConfig.getSystemChannelName(), block);
        return MyResourceUtils.release(block, "block");
    }

    public List<String> queryOrganizations(String networkName) throws NetworkException {
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        return network.getOrganizationNames();
    }

    public List<String> queryChannels(String networkName) {
        List<ChannelEntity> channels = channelRepo.findAllByNetworkName(networkName);
        List<String> channelNames = new ArrayList<>(channels.size());
        channels.forEach(channel -> channelNames.add(channel.getName()));
        return channelNames;
    }

    public List<Orderer> queryOrderers(String networkName) throws NetworkException {
        NetworkEntity network = findNetworkOrThrowEx(networkName);
        return network.getOrderers();
    }

    public List<ConfigtxOrderer> generateConfigtxOrderers(List<Orderer> orderers) {
        List<ConfigtxOrderer> configtxOrderers = new ArrayList<>(orderers.size());
        for (Orderer endpoint : orderers) {
            File ordererCertfileDir = CertfileUtils.getCertfileDir(endpoint.getCaUsername(), CertfileType.ORDERER);
            File ordererTlsServerCert = CertfileUtils.getTlsServerCert(ordererCertfileDir);

            ConfigtxOrderer configtxOrderer = new ConfigtxOrderer(endpoint, ordererTlsServerCert);
            configtxOrderers.add(configtxOrderer);
        }
        return configtxOrderers;
    }
}
