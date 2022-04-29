package com.anhui.fabricbaasttp.service;


import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.service.CaContainerService;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.repository.CaRepo;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaasttp.request.SystemInitRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class SystemService {
    @Autowired
    private AdminConfiguration adminConfiguration;
    @Autowired
    private FabricConfiguration fabricConfiguration;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private CaContainerService caContainerService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CaRepo caRepo;

    /**
     * 初始化主要包括两个任务：
     * 1. 更新管理员的密码
     * 2. 启动CA服务容器
     * 3. 登记管理员证书
     */
    public void init(SystemInitRequest req) throws Exception {
        if (caContainerService.checkCaContainer()) {
            throw new DuplicatedOperationException("CA服务已进入运行状态，请勿重复初始化系统");
        }
        if (caRepo.count() != 0) {
            throw new DuplicatedOperationException("系统中已存在TTP信息，请勿重复初始化系统");
        }
        CaEntity ttp = req.getTtp();
        log.info("可信第三方信息：" + ttp);
        CsrConfig CSRConfig = CaUtils.buildCsrConfig(ttp);
        log.info("生成CA服务信息：" + CSRConfig);
        // 启动CA容器并尝试初始化管理员证书
        caContainerService.startCaContainer(CSRConfig, fabricConfiguration.getRootCaUsername(), fabricConfiguration.getRootCaPassword());
        caRepo.save(ttp);

        log.info("正在初始化CA服务管理员证书...");
        caClientService.initRootCertfile(CSRConfig);

        Optional<UserEntity> adminOptional = userRepo.findById(adminConfiguration.getDefaultUsername());
        adminOptional.ifPresent(admin -> {
            if (req.getAdminPassword() != null && !StringUtils.isBlank(req.getAdminPassword())) {
                String encodedPassword = passwordEncoder.encode(req.getAdminPassword());
                admin.setPassword(encodedPassword);
                log.info("正在修改系统管理员密码...");
                userRepo.save(admin);
            }
            log.info("正在保存TTP信息...");
        });
    }

    public UniqueResult<Boolean> isInitialized() {
        return new UniqueResult<>(caRepo.count() == 0);
    }
}

