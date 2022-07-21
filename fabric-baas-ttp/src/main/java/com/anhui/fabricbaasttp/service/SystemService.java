package com.anhui.fabricbaasttp.service;


import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.CaServerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@CacheConfig(cacheNames = "ttp")
public class SystemService {
    @Autowired
    private AdminConfiguration adminConfiguration;
    @Autowired
    private CaServerService caServerService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private CaRepo caRepo;
    @Autowired
    private OrganizationService organizationService;

    /**
     * 初始化主要包括三个任务：
     * 1. 更新管理员的密码（可选）
     * 2. 启动CA服务容器
     * 3. 登记管理员证书
     */
    @Transactional
    public void init(CaEntity ttp, String newAdminPassword) throws Exception {
        if (isAvailable()) {
            throw new DuplicatedOperationException("CA服务已存在，请勿重复初始化系统");
        }

        CsrConfig csrConfig = CaUtils.buildCsrConfig(ttp);
        log.info("可信第三方信息：" + ttp);
        log.info("生成CA服务信息：" + csrConfig);

        // 启动CA服务并尝试初始化管理员证书
        caServerService.initCaServer(csrConfig);
        caClientService.initRootCertfile(csrConfig);
        caRepo.save(ttp);

        // 在有必要时修改密码
        if (newAdminPassword != null && !StringUtils.isBlank(newAdminPassword)) {
            organizationService.setPassword(adminConfiguration.getDefaultUsername(), newAdminPassword);
        }
    }

    public boolean isAvailable() {
        return caRepo.count() != 0 && caServerService.checkCaServer();
    }
}

