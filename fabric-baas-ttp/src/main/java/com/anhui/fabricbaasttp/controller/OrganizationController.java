package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaasttp.entity.OrganizationEntity;
import com.anhui.fabricbaasttp.entity.RegistrationEntity;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.service.OrganizationService;
import com.anhui.fabricbaasweb.response.EmptyResult;
import com.anhui.fabricbaasweb.response.LoginResult;
import com.anhui.fabricbaasweb.response.PaginationQueryResult;
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
@RequestMapping("/api/v1/organization")
@Api(tags = "组织管理模块", value = "组织管理相关接口")
public class OrganizationController {
    @Autowired
    private OrganizationService organizationService;

    @PostMapping("/login")
    @ApiOperation(value = "组织登录并获取口令")
    public LoginResult login(@Valid @RequestBody LoginRequest request) throws Exception {
        return organizationService.login(request);
    }

    @PostMapping("/applyRegistration")
    @ApiOperation(value = "提交注册申请")
    public EmptyResult applyRegistration(@Valid @RequestBody RegistrationApplyRequest request) throws Exception {
        organizationService.applyRegistration(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/handleRegistration")
    @ApiOperation(value = "接受或拒绝注册申请（同时发送邮件提醒）")
    public EmptyResult handleRegistration(@Valid @RequestBody RegistrationHandleRequest request) throws Exception {
        organizationService.handleRegistration(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryRegistrations")
    @ApiOperation(value = "对所有注册信息进行条件查询（包括已接受、已拒绝或未处理的）")
    public PaginationQueryResult<RegistrationEntity> queryRegistrations(@Valid @RequestBody RegistrationQueryRequest request) {
        return organizationService.queryRegistrations(request);
    }

    @Secured({Authority.ADMIN, Authority.USER})
    @PostMapping("/queryOrganizations")
    @ApiOperation(value = "查询所有已注册的组织信息")
    public PaginationQueryResult<OrganizationEntity> queryOrganizations(@Valid @RequestBody OrganizationQueryRequest request) {
        return organizationService.queryOrganizations(request);
    }
}
