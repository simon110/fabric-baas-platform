package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.request.CertGenerateRequest;
import com.anhui.fabricbaasorg.request.CertQueryRequest;
import com.anhui.fabricbaasorg.service.CertService;
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
@RequestMapping("/api/v1/cert")
@Api(tags = "证书管理模块", value = "证书管理相关接口")
public class CertController {
    @Autowired
    private CertService certService;

    @Secured({Authority.ADMIN})
    @PostMapping("/generate")
    @ApiOperation("创建证书")
    public void generate(@Valid @RequestBody CertGenerateRequest request) throws Exception {
        certService.generate(request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/query")
    @ApiOperation("条件查询证书")
    public PaginationQueryResult<CertfileEntity> query(@Valid @RequestBody CertQueryRequest request) throws Exception {
        Page<CertfileEntity> page = certService.query(request.getUsertype(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }
}

