package com.anhui.fabricbaasttp.configuration;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaasttp.entity.UserEntity;
import com.anhui.fabricbaasttp.repository.UserRepo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "admin")
@Data
@Slf4j
public class SystemAdminConfiguration {
    private String defaultUsername;
    private String defaultPassword;

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initSystemAdminPassword() {
        return args -> {
            Optional<UserEntity> adminOptional = userRepo.findById(defaultUsername);
            if (adminOptional.isEmpty()) {
                log.info("正在初始化管理员信息...");
                String encodedPassword = passwordEncoder.encode(defaultPassword);
                List<String> authorities = Collections.singletonList(Authority.ADMIN);
                UserEntity admin = new UserEntity(defaultUsername, encodedPassword, authorities);
                log.info("正在保存管理员信息：" + admin);
                userRepo.save(admin);
            }
        };
    }
}
