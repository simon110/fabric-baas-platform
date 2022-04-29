package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaasttp.request.SystemInitRequest;
import com.anhui.fabricbaasttp.service.SystemService;
import com.anhui.fabricbaascommon.response.EmptyResult;
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
@RequestMapping("/api/v1/system")
@Api(tags = "系统管理模块", value = "系统管理相关接口")
public class SystemController {
    @Autowired
    private SystemService systemService;

    @Secured({Authority.ADMIN})
    @PostMapping("/init")
    @ApiOperation("对系统管理员的账户密码和CA服务进行配置")
    public EmptyResult init(@Valid @RequestBody SystemInitRequest request) throws Exception {
        systemService.init(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/isInitialized")
    @ApiOperation("系统当前是否已经初始化")
    public UniqueResult<Boolean> isInitialized() {
        return new UniqueResult<>(systemService.isInitialized());
    }
}