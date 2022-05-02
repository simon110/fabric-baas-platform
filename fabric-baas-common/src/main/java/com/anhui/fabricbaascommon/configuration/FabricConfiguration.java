package com.anhui.fabricbaascommon.configuration;

import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Optional;


@PropertySource("classpath:fabricbaascommon.properties")
@Configuration
@ConfigurationProperties(prefix = "fabric")
@Data
@Slf4j
public class FabricConfiguration {
    private String systemChannelName;
    private String rootCaUsername;
    private String rootCaPassword;
    private int caServerPort;

    @Autowired
    private CertfileRepo certfileRepo;

    @Bean
    public CommandLineRunner adminCertfileInitializer() {
        return args -> {
            log.info("正在检查CA管理员信息...");
            Optional<CertfileEntity> adminCertfileOptional = certfileRepo.findById(rootCaUsername);
            if (adminCertfileOptional.isEmpty()) {
                log.info("正在初始化CA管理员信息...");
                CertfileEntity certfile = new CertfileEntity(rootCaUsername, rootCaPassword, CertfileType.ADMIN);
                certfileRepo.save(certfile);
            }
        };
    }
}

