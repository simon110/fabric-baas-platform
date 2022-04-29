package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.ConfigtxOrderer;
import com.anhui.fabricbaascommon.bean.ConfigtxOrganization;
import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.constant.ApplStatus;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.*;
import com.anhui.fabricbaascommon.fabric.ChannelUtils;
import com.anhui.fabricbaascommon.fabric.ConfigtxUtils;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.response.ResourceResult;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.service.MinIOService;
import com.anhui.fabricbaascommon.util.*;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.constant.MinIOBucket;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.entity.ParticipationEntity;
import com.anhui.fabricbaasttp.repository.NetworkRepo;
import com.anhui.fabricbaasttp.repository.ParticipationRepo;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.util.IdentifierGenerator;
import com.anhui.fabricbaasweb.util.SecurityUtils;
import com.spotify.docker.client.exceptions.NetworkNotFoundException;
import com.spotify.docker.client.exceptions.NodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Service
@Slf4j
public class NetworkService {
    @Autowired
    private MinIOService minioService;
    @Autowired
    private NetworkRepo networkRepo;
    @Autowired
    private CAService caService;
    @Autowired
    private ParticipationRepo participationRepo;
    @Autowired
    private FabricConfiguration fabricConfig;
    @Autowired
    private FabricEnvService fabricEnvService;

    public void assertOrganizationInNetwork(NetworkEntity network, String organizationName) throws OrganizationException {
        if (!network.getOrganizationNames().contains(organizationName)) {
            throw new OrganizationException("当前组织不是网络的合法成员");
        }
    }

    private static void assertOrderersNotInNetwork(List<Node> orderers, NetworkEntity network) throws NodeException {
        Set<String> set = new TreeSet<>();
        network.getOrderers().forEach(node -> set.add(node.getAddr()));
        for (Node orderer : orderers) {
            String addr = orderer.getAddr();
            if (set.contains(addr)) {
                throw new NodeException("该网络中已存在Orderer：" + addr);
            }
        }
    }

    private static void assertOrderersNotDuplicated(List<Node> orderers) throws NodeException {
        Set<String> set = new TreeSet<>();
        for (Node node : orderers) {
            String addr = node.getAddr();
            if (set.contains(addr)) {
                throw new NodeException("存在重复的Orderer地址");
            }
            set.add(addr);
        }
    }

    /**
     * 如果网络存在则返回相应的实体，否则抛出异常。
     */
    public NetworkEntity getNetworkOrThrowException(String networkName) throws NetworkNotFoundException {
        Optional<NetworkEntity> networkOptional = networkRepo.findById(networkName);
        if (networkOptional.isEmpty()) {
            throw new NetworkNotFoundException("不存在相应名称的网络");
        }
        return networkOptional.get();
    }

    /**
     * @param network Orderer所属的网络
     * @return Orderer节点的名称（默认为Orderer加查找到的下标，例如Orderer0）
     */
    private String getOrdererIdentifierOrThrowException(NetworkEntity network, Node orderer) throws NodeNotFoundException {
        List<Orderer> orderers = network.getOrderers();
        for (int i = 0, size = orderers.size(); i < size; i++) {
            Orderer endpoint = orderers.get(i);
            if (endpoint.getHost().equals(orderer.getHost()) &&
                    endpoint.getPort() == orderer.getPort()) {
                return IdentifierGenerator.ofOrderer(network.getName(), i);
            }
        }
        throw new NodeNotFoundException("未找到相应的Orderer");
    }

    private String getOrdererOrgNameOrThrowException(NetworkEntity network, Node orderer) throws NodeNotFoundException {
        List<Orderer> orderers = network.getOrderers();
        for (Orderer endpoint : orderers) {
            if (endpoint.getHost().equals(orderer.getHost()) &&
                    endpoint.getPort() == orderer.getPort()) {
                return endpoint.getOrganizationName();
            }
        }
        throw new NodeNotFoundException("未找到相应的Orderer");
    }

    private void addOrganizationToConsortium(NetworkEntity network, String newOrgName) throws Exception {
        // 从Orderer拉取通道配置
        File oldConfig = ResourceUtils.createTempFile("json");
        File newConfig = ResourceUtils.createTempFile("json");
        File orgConfig = ResourceUtils.createTempFile("json");
        File newOrgCertfileZip = ResourceUtils.createTempFile("zip");
        File newOrgCertfileDir = ResourceUtils.createTempDir();
        File envelope = ResourceUtils.createTempFile("pb");

        // 随机选择一个Orderer并拉取配置文件
        Orderer orderer = RandomUtils.select(network.getOrderers());
        log.info("随机从网络中选择了Orderer：" + orderer.getAddr());
        CoreEnv ordererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(orderer);
        log.info("生成Orderer的环境变量：" + ordererCoreEnv);
        ChannelUtils.fetchConfig(ordererCoreEnv, fabricConfig.getSystemChannelName(), oldConfig);
        log.info("从网络的系统通道拉取配置：" + oldConfig.toString());

        // 从MinIO下载新组织的证书并解压
        String organizationCertfileId = IdentifierGenerator.ofCertfile(network.getName(), newOrgName);
        minioService.getAsFile(MinIOBucket.ORGANIZATION_CERTFILE_BUCKET_NAME, organizationCertfileId, newOrgCertfileZip);
        ZipUtils.unzip(newOrgCertfileZip, newOrgCertfileDir);
        log.info("将组织的管理员证书解压到了临时目录：" + newOrgCertfileDir);

        // 生成新组织的配置的JSON
        ConfigtxOrganization newConfigtxOrganization = new ConfigtxOrganization();
        newConfigtxOrganization.setName(newOrgName);
        newConfigtxOrganization.setId(newOrgName);
        newConfigtxOrganization.setMspDir(new File(newOrgCertfileDir + "/msp"));
        File orgConfigtxDir = ResourceUtils.createTempDir();
        File orgConfigtxYaml = new File(orgConfigtxDir + "/configtx.yaml");
        ConfigtxUtils.generateOrgConfigtx(orgConfigtxYaml, newConfigtxOrganization);
        log.info("生成组织的configtx.yaml配置文件：" + orgConfigtxYaml.getAbsolutePath());
        ConfigtxUtils.convertOrgConfigtxToJson(orgConfig, orgConfigtxDir, newOrgName);
        log.info("将组织的configtx.yaml配置文件转换为json：" + orgConfig.getAbsolutePath());

        // 将新组织信息写入拉取下来的通道配置中
        FileUtils.copyFile(oldConfig, newConfig);
        ChannelUtils.appendOrganizationToSysChannelConfig(newOrgName, orgConfig, newConfig);
        log.info("将组织的configtx.yaml配置文件合并到原来的configtx.yaml中：" + newConfig.getAbsolutePath());

        // 对比新旧配置文件生成Envelope，并对Envelope进行签名
        log.info("正在生成提交到通道的Envelope并签名：" + envelope.getAbsolutePath());
        ChannelUtils.generateEnvelope(fabricConfig.getSystemChannelName(), envelope, oldConfig, newConfig);
        // ChannelUtils.signEnvelope(ordererCoreEnv.getMSPEnv(), envelope);

        // 将签名后的Envelope提交到Orderer
        log.info("正在将Envelope提交到系统通道：" + fabricConfig.getSystemChannelName());
        ChannelUtils.submitChannelUpdate(ordererCoreEnv.getMSPEnv(), ordererCoreEnv.getTLSEnv(), fabricConfig.getSystemChannelName(), envelope);
    }

    public ResourceResult queryOrdererTlsCert(NetworkQueryOrdererTlsCertRequest request) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());
        // 检查当前组织是否位于该网络中
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInNetwork(network, curOrgName);
        String ordererName = getOrdererIdentifierOrThrowException(network, request.getOrderer());

        // 检查Orderer证书
        File ordererCertfileDir = CertfileUtils.getCertfileDir(ordererName, CertfileType.ORDERER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        String downloadUrl = String.format("/download/certfile/%s.crt", UUID.randomUUID());
        FileUtils.copyFile(new File(ordererCertfileDir + "/tls/ca.crt"), new File("static" + downloadUrl));

        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public ResourceResult queryOrdererCert(NetworkQueryOrdererCertRequest request) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());
        // 检查当前组织是否位于该网络中
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInNetwork(network, curOrgName);

        // 检查Orderer是否属于该组织
        String ordererOrgName = getOrdererOrgNameOrThrowException(network, request.getOrderer());
        if (!ordererOrgName.equals(curOrgName)) {
            throw new OrganizationException("不允许下载其他组织的Orderer证书");
        }
        String ordererName = getOrdererIdentifierOrThrowException(network, request.getOrderer());

        // 检查Orderer证书
        File ordererCertfileDir = CertfileUtils.getCertfileDir(ordererName, CertfileType.ORDERER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        String downloadUrl = String.format("/download/certfile/%s.zip", UUID.randomUUID());
        ZipUtils.zip(new File("static" + downloadUrl),
                CertfileUtils.getCertfileMSPDir(ordererCertfileDir),
                CertfileUtils.getCertfileTLSDir(ordererCertfileDir)
        );

        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public ResourceResult addOrderer(NetworkAddOrdererRequest request) throws Exception {
        // 检查网络是否存在
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());

        // 检查当前组织是否位于该网络中
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInNetwork(network, curOrgName);

        // 检查Orderer节点是否已经存在于网络中
        assertOrderersNotInNetwork(Collections.singletonList(request.getOrderer()), network);

        // 为新Orderer注册证书并登记
        int newOrdererNo = network.getOrderers().size();
        String caUsername = IdentifierGenerator.ofOrderer(network.getName(), newOrdererNo);
        String caPassword = PasswordUtils.generatePassword();
        try {
            caService.register(caUsername, caPassword, CertfileType.ORDERER);
        } catch (CertfileException e) {
            // 此处可以假设抛出的异常不是因为缺少CA管理员证书
            log.warn("账户已注册：" + caUsername);
        }

        List<String> csrHosts = Arrays.asList("localhost", request.getOrderer().getHost());
        File newOrdererCertfileDir = ResourceUtils.createTempDir();
        caService.enroll(newOrdererCertfileDir, caUsername, csrHosts);
        log.info("将证书登记到临时目录：" + newOrdererCertfileDir.getAbsolutePath());

        // 从现有的网络中随机选择一个Orderer节点
        Orderer selectedOrderer = RandomUtils.select(network.getOrderers());
        log.info("随机选中Orderer节点：" + selectedOrderer);

        // 拉取通道的配置文件（以Orderer组织管理员的身份）
        CoreEnv selectedOrdererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(selectedOrderer);
        log.info("生成Orderer的环境变量：" + selectedOrdererCoreEnv);
        File oldChannelConfig = ResourceUtils.createTempFile("json");
        ChannelUtils.fetchConfig(selectedOrdererCoreEnv, fabricConfig.getSystemChannelName(), oldChannelConfig);
        log.info("拉取系统通道的配置：" + oldChannelConfig.getAbsolutePath());


        // 向通道配置文件中添加新Orderer的定义
        ConfigtxOrderer configtxOrderer = new ConfigtxOrderer();
        configtxOrderer.setHost(request.getOrderer().getHost());
        configtxOrderer.setPort(request.getOrderer().getPort());
        File newOrdererTlsServerCert = new File(newOrdererCertfileDir + "/tls/server.crt");
        configtxOrderer.setClientTlsCert(newOrdererTlsServerCert);
        configtxOrderer.setServerTlsCert(newOrdererTlsServerCert);

        File newChannelConfig = ResourceUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendOrdererToChannelConfig(configtxOrderer, newChannelConfig);
        log.info("将新的Orderer信息添加到通道配置：" + newChannelConfig.getAbsolutePath());

        // 计算新旧JSON配置文件之间的差异得到Envelope，并对其进行签名
        File envelope = ResourceUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(fabricConfig.getSystemChannelName(), envelope, oldChannelConfig, newChannelConfig);
        // ChannelUtils.signEnvelope(selectedOrdererCoreEnv.getMSPEnv(), envelope);
        log.info("生成并使用Orderer身份对Envelope文件进行签名：" + envelope.getAbsolutePath());

        // 将Envelope提交到现有的Orderer节点
        ChannelUtils.submitChannelUpdate(
                selectedOrdererCoreEnv.getMSPEnv(),
                selectedOrdererCoreEnv.getTLSEnv(),
                fabricConfig.getSystemChannelName(),
                envelope
        );

        // 将新Orderer的分别保存到MinIO并复制到证书目录下
        File ordererCertfileZip = ResourceUtils.createTempFile("zip");
        ZipUtils.zip(ordererCertfileZip,
                new File(newOrdererCertfileDir + "/msp"),
                new File(newOrdererCertfileDir + "/tls")
        );
        minioService.putFile(MinIOBucket.ORDERER_CERTFILE_BUCKET_NAME, caUsername, ordererCertfileZip);
        log.info("正在上传新Orderer的证书到MinIO...");
        File formalCertfileDir = CertfileUtils.getCertfileDir(caUsername, CertfileType.ORDERER);
        ZipUtils.unzip(ordererCertfileZip, formalCertfileDir);
        log.info("将新Orderer的证书解压到正式目录：" + formalCertfileDir);

        // 将更新后的信息保存到数据库
        Orderer newOrderer = new Orderer();
        newOrderer.setOrganizationName(curOrgName);
        newOrderer.setHost(request.getOrderer().getHost());
        newOrderer.setPort(request.getOrderer().getPort());
        newOrderer.setCaUsername(caUsername);
        newOrderer.setCaPassword(caPassword);
        network.getOrderers().add(newOrderer);
        networkRepo.save(network);
        log.info("更新网络的信息：" + network);

        String downloadUrl = String.format("/download/certfile/%s.zip", UUID.randomUUID());
        FileUtils.copyFile(ordererCertfileZip, new File("static" + downloadUrl));
        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public ResourceResult createNetwork(NetworkCreateRequest request, MultipartFile adminCertZip) throws Exception {
        // 检查是否存在重复的Orderer地址
        assertOrderersNotDuplicated(request.getOrderers());

        // 检查相同名称的网络是否已存在
        if (networkRepo.existsById(request.getNetworkName())) {
            throw new NetworkException("相同名称的网络已经存在：" + request.getNetworkName());
        }

        // 获取当前登录用户身份
        String curOrgName = SecurityUtils.getUsername();
        File orgCertfileDir = ResourceUtils.createTempDir();
        File organizationCertfileZip = ResourceUtils.createTempFile("zip");
        log.info(String.format("正在将组织%s上传的网络管理员证书写入到：", curOrgName) + organizationCertfileZip.getAbsolutePath());
        FileUtils.writeByteArrayToFile(organizationCertfileZip, adminCertZip.getBytes());
        log.info(String.format("正在将组织%s上传的网络管理员证书解压到：", curOrgName) + orgCertfileDir.getAbsolutePath());
        ZipUtils.unzip(organizationCertfileZip, orgCertfileDir);
        log.info(String.format("正在检查组织%s上传的网络管理员证书：", curOrgName) + orgCertfileDir.getAbsolutePath());
        CertfileUtils.assertCertfile(orgCertfileDir);

        List<Node> orderers = request.getOrderers();
        List<ConfigtxOrderer> configtxOrderers = new ArrayList<>();
        List<Orderer> ordererEndpoints = new ArrayList<>();
        List<String> ordererCertfileDirs = new ArrayList<>();
        log.info("正在生成Orderer证书...");
        for (int i = 0, size = orderers.size(); i < size; i++) {
            Node orderer = orderers.get(i);
            // 注册并登记Orderer证书
            // 账户名称例如SampleNetwork-Orderer0
            // 账户密码例如13473cf3bc515bccbb81fa235ed33ff9
            String caUsername = IdentifierGenerator.ofOrderer(request.getNetworkName(), i);
            String caPassword = PasswordUtils.generatePassword();
            caService.register(caUsername, caPassword, CertfileType.ORDERER);

            List<String> csrHosts = Arrays.asList("localhost", orderer.getHost());
            File ordererCertfileDir = ResourceUtils.createTempDir();
            ordererCertfileDirs.add(ordererCertfileDir.getCanonicalPath());
            caService.enroll(ordererCertfileDir, caUsername, csrHosts);

            // 增加configtx的Orderer定义
            // 注意此处的TLS证书路径是configtx.yaml的相对路径
            File tlsSeverCert = new File(ordererCertfileDir + "/tls/server.crt");
            configtxOrderers.add(new ConfigtxOrderer(orderer.getHost(), orderer.getPort(), tlsSeverCert, tlsSeverCert));

            // 生成保存到数据库的Orderer信息
            Orderer node = new Orderer();
            node.setOrganizationName(curOrgName);
            node.setCaUsername(caUsername);
            node.setCaPassword(caPassword);
            node.setHost(orderer.getHost());
            node.setPort(orderer.getPort());
            ordererEndpoints.add(node);
        }

        // 生成configtx.yaml文件和创世区块
        String ordererOrgName = caService.getAdminOrganizationName();
        ConfigtxOrganization ordererConfigtxOrg = new ConfigtxOrganization(ordererOrgName, ordererOrgName,
                new File(caService.getAdminCertfileDir() + "/msp"));
        ConfigtxOrganization currentConfigtxOrg = new ConfigtxOrganization(curOrgName, curOrgName,
                new File(orgCertfileDir + "/msp"));
        List<ConfigtxOrganization> configtxOrganizations = Collections.singletonList(currentConfigtxOrg);

        File configtxDir = ResourceUtils.createTempDir();
        File configtxYaml = new File(configtxDir + "/configtx.yaml");
        ConfigtxUtils.generateConfigtx(configtxYaml, request.getConsortiumName(), configtxOrderers, ordererConfigtxOrg, configtxOrganizations);
        log.info(String.format("生成配置文件%s：", configtxYaml.getAbsoluteFile()) + configtxYaml.getAbsolutePath());

        File sysChannelGenesis = ResourceUtils.createTempFile("block");
        ConfigtxUtils.generateGenesisBlock("OrdererGenesis", fabricConfig.getSystemChannelName(), sysChannelGenesis, configtxDir);
        log.info("生成创世区块：" + sysChannelGenesis.getAbsolutePath());

        // 保存组织证书、Orderer证书和创世区块至MinIO
        String organizationCertfileId = IdentifierGenerator.ofCertfile(request.getNetworkName(), curOrgName);
        log.info(String.format("正在将组织%s上传的网络管理员证书保存至MinIO和证书目录", curOrgName));
        minioService.putBytes(MinIOBucket.ORGANIZATION_CERTFILE_BUCKET_NAME, organizationCertfileId, adminCertZip.getBytes());
        ZipUtils.unzip(organizationCertfileZip, CertfileUtils.getCertfileDir(organizationCertfileId, CertfileType.ADMIN));

        for (int i = 0, size = ordererCertfileDirs.size(); i < size; i++) {
            // 将证书压缩
            String ordererCertfileDir = ordererCertfileDirs.get(i);
            File ordererCertZip = ResourceUtils.createTempFile("zip");
            ZipUtils.zip(ordererCertZip, new File(ordererCertfileDir + "/msp"), new File(ordererCertfileDir + "/tls"));

            // 将证书保存到正式的证书目录和MinIO
            String ordererId = IdentifierGenerator.ofOrderer(request.getNetworkName(), i);
            log.info(String.format("正在将网络的Orderer证书%s保存至MinIO和证书目录：", ordererId));
            ZipUtils.unzip(ordererCertZip, CertfileUtils.getCertfileDir(ordererId, CertfileType.ORDERER));
            minioService.putFile(MinIOBucket.ORDERER_CERTFILE_BUCKET_NAME, ordererId, ordererCertZip);
        }
        log.info("正在将网络的创世区块保存至MinIO：" + request.getNetworkName());
        minioService.putFile(MinIOBucket.SYS_CHANNEL_GENESIS_BLOCK_BUCKET_NAME, request.getNetworkName(), sysChannelGenesis);

        // 保存网络信息到数据库
        NetworkEntity network = new NetworkEntity();
        network.setName(request.getNetworkName());
        network.setConsortiumName(request.getConsortiumName());
        network.setOrderers(ordererEndpoints);
        network.setOrganizationNames(Collections.singletonList(curOrgName));
        log.info("保存网络信息：" + network);
        networkRepo.save(network);

        ParticipationEntity participation = new ParticipationEntity();
        participation.setNetworkName(request.getNetworkName());
        participation.setDescription("网络创建组织");
        participation.setStatus(ApplStatus.ACCEPTED);
        participation.setTimestamp(System.currentTimeMillis());
        participation.setOrganizationName(curOrgName);
        participation.setApprovals(Collections.emptyList());
        log.info("保存网络参与权限：" + participation);
        participationRepo.save(participation);

        // 返回创世区块下载路径
        String downloadUrl = String.format("/download/block/%s.block", UUID.randomUUID());
        File block = new File("static" + downloadUrl);
        FileUtils.copyFile(sysChannelGenesis, block);
        log.info("创世区块下载路径：" + downloadUrl);

        return new ResourceResult(downloadUrl);
    }

    public PaginationQueryResult<NetworkEntity> queryNetworks(NetworkQueryRequest request) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize(), sort);
        List<NetworkEntity> networks;
        int totalPages;
        boolean isNetworkNameKeywordBlank = StringUtils.isBlank(request.getNetworkNameKeyword());
        boolean isOrganizationNameKeyKeywordBlank = StringUtils.isBlank(request.getOrganizationNameKeyword());
        if (isNetworkNameKeywordBlank && isOrganizationNameKeyKeywordBlank) {
            Page<NetworkEntity> page = networkRepo.findAll(pageable);
            totalPages = page.getTotalPages();
            networks = page.toList();
        } else if (isOrganizationNameKeyKeywordBlank) {
            // 按网络名称关键词进行搜索
            Page<NetworkEntity> page = networkRepo.findAllByNameLike(request.getNetworkNameKeyword(), pageable);
            totalPages = page.getTotalPages();
            networks = page.toList();
        } else {
            List<ParticipationEntity> allowedParticipations;
            if (isNetworkNameKeywordBlank) {
                // 按组织名称关键词进行搜索
                allowedParticipations = participationRepo.findAllByOrganizationNameLikeAndStatus(request.getOrganizationNameKeyword(), ApplStatus.ACCEPTED);
            } else {
                // 同时根据组织名称和网络名称进行搜索
                allowedParticipations = participationRepo.findAllByNetworkNameLikeAndOrganizationNameLikeAndStatus(request.getNetworkNameKeyword(), request.getOrganizationNameKeyword(), ApplStatus.ACCEPTED);
            }
            Set<String> networkNameSet = new TreeSet<>();
            allowedParticipations.forEach(participation -> networkNameSet.add(participation.getNetworkName()));
            List<String> networkNames = new ArrayList<>(networkNameSet);
            if (networkNames.size() > request.getPageSize()) {
                int startIndex = (request.getPage() - 1) * request.getPageSize();
                int endIndex = Integer.min(startIndex + request.getPageSize(), networkNames.size());
                networkNames = networkNames.subList(startIndex, endIndex);
            }
            networks = new ArrayList<>();
            networkNames.forEach(networkName -> {
                Optional<NetworkEntity> networkOptional = networkRepo.findById(networkName);
                assert networkOptional.isPresent();
                networks.add(networkOptional.get());
            });
            totalPages = (int) Math.ceil((double) networkNameSet.size() / request.getPageSize());
        }

        PaginationQueryResult<NetworkEntity> result = new PaginationQueryResult<>();
        result.setTotalPages(totalPages);
        result.setItems(networks);
        return result;
    }

    public void applyParticipation(ParticipationApplyRequest request, MultipartFile adminCertZip) throws Exception {
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());
        String curOrgName = SecurityUtils.getUsername();
        // 检查是否存在未处理的加入网络申请
        Optional<ParticipationEntity> participationOptional = participationRepo.findFirstByNetworkNameAndOrganizationNameAndStatus(request.getNetworkName(), curOrgName, ApplStatus.UNHANDLED);
        if (participationOptional.isPresent()) {
            throw new DuplicatedOperationException("该组织存在未处理的加入该网络的申请");
        }
        // 检查组织是否已经在网络中
        if (network.getOrganizationNames().contains(curOrgName)) {
            throw new OrganizationException("组织已存在于网络中，请勿重复加入");
        }
        // 检查证书格式是否正确
        CertfileUtils.assertCertfileZip(adminCertZip);
        // 保存管理员证书至MinIO
        String orgCertId = IdentifierGenerator.ofCertfile(request.getNetworkName(), curOrgName);
        log.info("正在将证书文件保存至MinIO：" + orgCertId);
        minioService.putBytes(MinIOBucket.ORGANIZATION_CERTFILE_BUCKET_NAME, orgCertId, adminCertZip.getBytes());

        // 将加入网络申请保存到MongoDB
        ParticipationEntity participation = new ParticipationEntity();
        participation.setNetworkName(request.getNetworkName());
        participation.setDescription(request.getDescription());
        participation.setStatus(ApplStatus.UNHANDLED);
        participation.setTimestamp(System.currentTimeMillis());
        participation.setOrganizationName(curOrgName);
        participation.setApprovals(Collections.emptyList());
        log.info("正在将申请信息保存到数据库：" + participation);
        participationRepo.save(participation);
    }

    public void handleParticipation(ParticipationHandleRequest request) throws Exception {
        // 找到相应的申请
        Optional<ParticipationEntity> participationOptional = participationRepo.findFirstByNetworkNameAndOrganizationNameAndStatus(request.getNetworkName(), request.getOrganizationName(), ApplStatus.UNHANDLED);
        if (participationOptional.isEmpty()) {
            throw new ParticipationException("该组织不存在待处理的加入网络申请");
        }
        ParticipationEntity participation = participationOptional.get();
        String curOrgName = SecurityUtils.getUsername();
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());

        // 确定操作者是该网络中的成员
        assertOrganizationInNetwork(network, curOrgName);

        // 确定操作者没有重复操作
        if (participation.getApprovals().contains(request.getOrganizationName())) {
            throw new DuplicatedOperationException("请勿重复操作");
        }
        log.info("当前网络中已经同意申请的组织包括：" + participation.getApprovals());

        if (request.isAllowed()) {
            // 如果操作者赞成
            List<String> approvals = participation.getApprovals();
            approvals.add(curOrgName);
            if (approvals.size() == network.getOrganizationNames().size()) {
                addOrganizationToConsortium(network, request.getOrganizationName());
                // 如果达到条件则更新系统通道配置
                participation.setStatus(ApplStatus.ACCEPTED);
                participation.setTimestamp(System.currentTimeMillis());
                // 将证书解压到指定目录
                File certfileZip = ResourceUtils.createTempFile("zip");
                String certfileId = IdentifierGenerator.ofCertfile(network.getName(), request.getOrganizationName());
                minioService.getAsFile(MinIOBucket.ORGANIZATION_CERTFILE_BUCKET_NAME, certfileId, certfileZip);
                ZipUtils.unzip(certfileZip, CertfileUtils.getCertfileDir(certfileId, CertfileType.ADMIN));
                // 将组织加入到网络
                network.getOrganizationNames().add(request.getOrganizationName());
                // TODO: 发送邮件通知
            }
        } else {
            // 如果操作者拒绝
            participation.setStatus(ApplStatus.REJECTED);
        }
        // 更新MongoDB中的数据
        log.info("正在更新网络参与权信息：" + participation);
        participationRepo.save(participation);
        log.info("正在更新网络信息：" + network);
        networkRepo.save(network);
    }

    public PaginationQueryResult<ParticipationEntity> queryParticipations(ParticipationQueryRequest request) throws NetworkNotFoundException {
        getNetworkOrThrowException(request.getNetworkName());

        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize(), sort);
        Page<ParticipationEntity> page = participationRepo.findAllByNetworkNameAndStatus(request.getNetworkName(), request.getStatus(), pageable);

        PaginationQueryResult<ParticipationEntity> result = new PaginationQueryResult<>();
        result.setItems(page.toList());
        result.setTotalPages(page.getTotalPages());
        return result;
    }

    public ResourceResult queryGenesisBlock(NetworkQueryGenesisBlockRequest request) throws Exception {
        NetworkEntity network = getNetworkOrThrowException(request.getNetworkName());
        String organizationName = SecurityUtils.getUsername();
        assertOrganizationInNetwork(network, organizationName);

        Orderer orderer = network.getOrderers().get(0);
        CoreEnv ordererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(orderer);

        String downloadUrl = String.format("/download/block/%s.block", UUID.randomUUID());
        File block = new File("static" + downloadUrl);
        ChannelUtils.fetchGenesisBlock(ordererCoreEnv, fabricConfig.getSystemChannelName(), block);

        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }
}
