package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaasorg.request.RemoteUserInitRequest;
import com.anhui.fabricbaasorg.service.RemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/ttp")
@Api(tags = "可信第三方管理模块", value = "可信第三方管理相关接口")
public class RemoteUserController {
    @Autowired
    private RemoteService remoteService;

    @Secured({Authority.ADMIN})
    @PostMapping("/init")
    @ApiOperation("对可信第三方的账户密码和地址进行配置")
    public EmptyResult init(@Valid @RequestBody RemoteUserInitRequest request) throws Exception {
        remoteService.init(request);
        return new EmptyResult();
    }
}
