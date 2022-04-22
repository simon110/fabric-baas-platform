package com.anhui.fabricbaasttp.service;


import com.anhui.fabricbaascommon.bean.CAConfig;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.service.DockerService;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.entity.CAEntity;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.repository.CARepo;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaasttp.request.SystemInitRequest;
import lombok.extern.slf4j.Slf4j;
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
    private DockerService dockerService;
    @Autowired
    private CAService caService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CARepo caRepo;

    /**
     * 初始化主要包括两个任务：
     * 1. 更新管理员的密码
     * 2. 启动CA服务容器
     * 3. 登记管理员证书
     */
    public void init(SystemInitRequest req) throws Exception {
        if (dockerService.checkCAServer()) {
            throw new DuplicatedOperationException("CA服务已进入运行状态，请勿重复初始化系统");
        }
        if (caRepo.count() != 0) {
            throw new DuplicatedOperationException("系统中已存在TTP信息，请勿重复初始化系统");
        }
        CAEntity ttp = req.getTtp();
        log.info("可信第三方信息：" + ttp);
        CAConfig caConfig = caService.buildCAConfig(ttp);
        log.info("生成CA服务信息：" + caConfig);
        // 启动CA容器并尝试初始化管理员证书
        dockerService.startCAServer(caConfig, fabricConfiguration.getCaAdminUsername(), fabricConfiguration.getCaAdminPassword());
        log.info("正在初始化CA服务管理员证书...");
        caService.initAdminCertfile(caConfig);

        Optional<UserEntity> adminOptional = userRepo.findById(adminConfiguration.getDefaultUsername());
        adminOptional.ifPresent(admin -> {
            String encodedPassword = passwordEncoder.encode(req.getAdminPassword());
            admin.setPassword(encodedPassword);
            log.info("正在修改系统管理员密码...");
            userRepo.save(admin);
            log.info("正在保存TTP信息...");
            caRepo.save(ttp);
        });
    }
}

