package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import com.anhui.fabricbaascommon.response.LoginResult;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import com.anhui.fabricbaasweb.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResult login(String organizationName, String password) throws IncorrectPasswordException {
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(organizationName);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new IncorrectPasswordException("密码错误");
        }
        String token = jwtUtils.generateToken(userDetails);
        log.info("用户{}登录成功：{}", userDetails.getUsername(), token);
        return new LoginResult(token);
    }
}
