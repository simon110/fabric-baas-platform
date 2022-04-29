package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.CSRConfig;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.fabric.CAUtils;
import com.anhui.fabricbaascommon.repository.CARepo;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.service.DockerService;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaasorg.request.SystemInitRequest;
import com.anhui.fabricbaasorg.response.ClusterNodeQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Optional;

@Slf4j
@Service
public class SystemService {
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private AdminConfiguration adminConfiguration;
    @Autowired
    private FabricConfiguration fabricConfiguration;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CAService caService;
    @Autowired
    private DockerService dockerService;
    @Autowired
    private CARepo caRepo;


    /**
     * 主要包括一下内容
     * 1. 重置管理员的密码
     * 2. 将集群配置导入到KubernetesService
     */
    public void init(SystemInitRequest req, MultipartFile clusterConfig) throws Exception {
        // 将证书导入到KubernetesService中
        File tempClusterConfig = ResourceUtils.createTempFile("yaml");
        FileUtils.writeByteArrayToFile(tempClusterConfig, clusterConfig.getBytes());
        kubernetesService.importAdminConfig(tempClusterConfig);

        // 启动CA服务
        if (caRepo.count() != 0 || dockerService.checkCAServer()) {
            throw new DuplicatedOperationException("CA服务已进入运行状态，请勿重复初始化系统");
        }
        CAEntity org = req.getOrg();
        log.info("可信第三方信息：" + org);
        CSRConfig CSRConfig = CAUtils.buildCsrConfig(org);
        log.info("生成CA服务信息：" + CSRConfig);


        // 启动CA容器并尝试初始化管理员证书
        dockerService.startCAServer(CSRConfig,
                fabricConfiguration.getCaAdminUsername(),
                fabricConfiguration.getCaAdminPassword()
        );
        caRepo.save(org);
        log.info("正在初始化CA服务管理员证书");
        caService.initAdminCertfile(CSRConfig);

        // 更新系统管理员密码
        if (req.getAdminPassword() != null && !StringUtils.isBlank(req.getAdminPassword())) {
            Optional<UserEntity> adminOptional = userRepo.findById(adminConfiguration.getDefaultUsername());
            adminOptional.ifPresent(admin -> {
                log.info("正在初始化管理员信息");
                String encodedPassword = passwordEncoder.encode(req.getAdminPassword());
                admin.setPassword(encodedPassword);
                userRepo.save(admin);
            });
        }
    }

    public ClusterNodeQueryResult getClusterNodeNames() throws Exception {
        ClusterNodeQueryResult result = new ClusterNodeQueryResult();
        result.setClusterNodes(kubernetesService.getNodeNames());
        return result;
    }
}
