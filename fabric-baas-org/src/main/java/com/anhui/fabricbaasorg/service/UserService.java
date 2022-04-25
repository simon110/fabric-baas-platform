package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.request.LoginRequest;
import com.anhui.fabricbaascommon.response.LoginResult;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import com.anhui.fabricbaasweb.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtUtils jwtUtils;

    public LoginResult login(LoginRequest request) {
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(request.getOrganizationName());
        String token = jwtUtils.generateToken(userDetails);
        return new LoginResult(token);
    }
}
