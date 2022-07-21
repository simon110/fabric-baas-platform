package com.anhui.fabricbaasorg.service;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.configuration.AdminConfiguration;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import com.anhui.fabricbaasweb.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserService {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private AdminConfiguration adminConfiguration;

    public String login(String organizationName, String password) throws IncorrectPasswordException {
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(organizationName);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new IncorrectPasswordException("密码错误");
        }
        String token = jwtUtils.generateToken(userDetails);
        log.info("用户{}登录成功：{}", userDetails.getUsername(), token);
        return token;
    }

    public void setAdminPassword(String password) {
        Optional<UserEntity> adminOptional = userRepo.findById(adminConfiguration.getDefaultUsername());
        Assert.isTrue(adminOptional.isPresent());
        adminOptional.ifPresent(admin -> {
            log.info("正在初始化管理员信息");
            String encodedPassword = passwordEncoder.encode(password);
            admin.setPassword(encodedPassword);
            userRepo.save(admin);
        });
    }
}
