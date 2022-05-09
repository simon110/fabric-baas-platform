package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.LoginRequest;
import com.anhui.fabricbaascommon.response.LoginResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaasttp.entity.OrganizationEntity;
import com.anhui.fabricbaasttp.entity.RegistrationEntity;
import com.anhui.fabricbaasttp.request.OrganizationQueryRequest;
import com.anhui.fabricbaasttp.request.RegistrationApplyRequest;
import com.anhui.fabricbaasttp.request.RegistrationHandleRequest;
import com.anhui.fabricbaascommon.request.StatusBasedPaginationQueryRequest;
import com.anhui.fabricbaasttp.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    @Autowired
    private CaClientService caClientService;

    @PostMapping("/login")
    @ApiOperation(value = "组织登录并获取口令")
    public LoginResult login(@Valid @RequestBody LoginRequest request) throws Exception {
        String token = organizationService.login(request.getOrganizationName(), request.getPassword());
        return new LoginResult(token);
    }

    @PostMapping("/applyRegistration")
    @ApiOperation(value = "提交注册申请")
    public void applyRegistration(@Valid @RequestBody RegistrationApplyRequest request) throws Exception {
        organizationService.applyRegistration(
                request.getOrganizationName(),
                request.getPassword(),
                request.getDescription(),
                request.getEmail(),
                request.getApiServer()
        );
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/handleRegistration")
    @ApiOperation(value = "接受或拒绝注册申请（同时发送邮件提醒）")
    public void handleRegistration(@Valid @RequestBody RegistrationHandleRequest request) throws Exception {
        organizationService.handleRegistration(request.getOrganizationName(), request.isAllowed());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryRegistrations")
    @ApiOperation(value = "对所有注册信息进行条件查询（包括已接受、已拒绝或未处理的）")
    public PaginationQueryResult<RegistrationEntity> queryRegistrations(@Valid @RequestBody StatusBasedPaginationQueryRequest request) {
        Page<RegistrationEntity> page = organizationService.queryRegistrations(request.getStatus(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN, Authority.USER})
    @PostMapping("/queryOrganizations")
    @ApiOperation(value = "查询所有已注册的组织信息")
    public PaginationQueryResult<OrganizationEntity> queryOrganizations(@Valid @RequestBody OrganizationQueryRequest request) {
        Page<OrganizationEntity> page = organizationService.queryOrganizations(request.getOrganizationNameKeyword(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN, Authority.USER})
    @PostMapping("/getOrdererOrganizationName")
    @ApiOperation("获取维护该系统的组织的名称")
    public UniqueResult<String> getOrdererOrganizationName() throws Exception {
        return new UniqueResult<>(caClientService.getCaOrganizationName());
    }
}