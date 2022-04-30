package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.request.LoginRequest;
import com.anhui.fabricbaascommon.response.LoginResult;
import com.anhui.fabricbaasorg.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
@Api(tags = "用户管理模块", value = "用户管理相关接口")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @ApiOperation(value = "管理员登录并获取口令")
    public LoginResult login(@Valid @RequestBody LoginRequest request) throws Exception {
        return userService.login(request.getOrganizationName(), request.getPassword());
    }
}

