package com.anhui.fabricbaasttp.service;


import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.exception.OrganizationException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.CaServerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@CacheConfig(cacheNames = "ttp")
public class SystemService {
    @Autowired
    private AdminConfiguration adminConfiguration;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private CaServerService caServerService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CaRepo caRepo;

    private void setPassword(String organizationName, String newPassword) throws OrganizationException {
        Optional<UserEntity> optional = userRepo.findById(organizationName);
        if (optional.isEmpty()) {
            throw new OrganizationException("不存在组织：" + organizationName);
        }
        UserEntity organization = optional.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        organization.setPassword(encodedPassword);
        log.info("正在修改系统管理员密码...");
        userRepo.save(organization);
    }

    /**
     * 初始化主要包括三个任务：
     * 1. 更新管理员的密码（可选）
     * 2. 启动CA服务容器
     * 3. 登记管理员证书
     */
    @Transactional
    @CacheEvict(key = "'SystemService:isAvailable'")
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
            setPassword(adminConfiguration.getDefaultUsername(), newAdminPassword);
        }
    }

    @Cacheable(keyGenerator = "keyGenerator")
    public boolean isAvailable() {
        return caRepo.count() != 0;
    }
}

