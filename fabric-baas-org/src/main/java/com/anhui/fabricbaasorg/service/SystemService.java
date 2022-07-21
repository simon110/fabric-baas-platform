package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.CaServerService;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import com.anhui.fabricbaasorg.remote.RemoteHttpClient;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.RemoteUserRepo;
import com.spotify.docker.client.exceptions.DockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@CacheConfig(cacheNames = "org")
public class SystemService {
    @Autowired
    private KubernetesService kubernetesService;
    @Autowired
    private UserService userService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private CaServerService caServerService;
    @Autowired
    private CaRepo caRepo;
    @Autowired
    private RemoteUserRepo remoteUserRepo;
    @Autowired
    private RemoteHttpClient remoteHttpClient;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

    private void initRemoteUser(RemoteUserEntity remoteUser) throws Exception {
        if (remoteUserRepo.count() != 0) {
            throw new DuplicatedOperationException("可信第三方的信息已经存在！");
        }
        remoteHttpClient.init(remoteUser.getApiServer());
        ttpOrganizationApi.login(remoteUser.getOrganizationName(), remoteUser.getPassword());
        remoteUserRepo.save(remoteUser);
    }

    private void initCaService(CaEntity ca) throws DuplicatedOperationException, DockerException, InterruptedException, IOException, CaException {
        if (caRepo.count() != 0) {
            throw new DuplicatedOperationException("CA服务存在，请勿重复初始化系统");
        }
        log.info("可信第三方信息：" + ca);
        CsrConfig csrConfig = CaUtils.buildCsrConfig(ca);
        log.info("生成CA服务信息：" + csrConfig);
        // 启动CA容器并尝试初始化管理员证书
        log.info("正在启动CA服务容器...");
        caServerService.initCaServer(csrConfig);
        log.info("正在初始化CA服务管理员证书...");
        caClientService.initRootCertfile(csrConfig);
        caRepo.save(ca);
    }

    private void initKubernetesService(MultipartFile kubernetesConfig) throws Exception {
        // 将证书导入到KubernetesService中
        File tempKubernetesConfig = MyFileUtils.createTempFile("yaml");
        FileUtils.writeByteArrayToFile(tempKubernetesConfig, kubernetesConfig.getBytes());
        kubernetesService.importAdminConfig(tempKubernetesConfig);
    }

    /**
     * 主要包括以下内容
     * 1. 重置管理员的密码
     * 2. 将集群配置导入到Kubernetes服务
     * 3. 初始化CA服务
     * 4. 初始化TTP远程用户
     */
    @Transactional
    public void init(CaEntity org, RemoteUserEntity remoteUser, String adminPassword, MultipartFile kubernetesConfig) throws Exception {
        if (isAvailable()) {
            throw new DuplicatedOperationException("请勿重复初始化系统");
        }

        try {
            initRemoteUser(remoteUser);
            initKubernetesService(kubernetesConfig);
            initCaService(org);

            // 更新系统管理员密码
            if (adminPassword != null && !StringUtils.isBlank(adminPassword)) {
                userService.setAdminPassword(adminPassword);
            }
        } catch (Exception e) {
            caServerService.cleanCaServer();
            throw e;
        }
    }

    public boolean isAvailable() {
        return caRepo.count() != 0 && caServerService.checkCaServer();
    }
}
