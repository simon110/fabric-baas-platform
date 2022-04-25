package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaasorg.request.SystemInitRequest;
import com.anhui.fabricbaasorg.response.ClusterNodeQueryResult;
import com.anhui.fabricbaasorg.service.SystemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/system")
@Api(tags = "系统管理模块", value = "系统管理相关接口")
public class SystemController {
    @Autowired
    private SystemService systemService;

    @Secured({Authority.ADMIN})
    @PostMapping("/getClusterNodeNames")
    @ApiOperation("获取集群的所有物理节点")
    public ClusterNodeQueryResult getClusterNodeNames() throws Exception {
        return systemService.getClusterNodeNames();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/init")
    @ApiOperation("对系统管理员的账户密码和CA服务进行配置")
    public EmptyResult init(@Valid @RequestPart SystemInitRequest request,
                            @ApiParam(value = "连接K8S的Yaml配置文件") @RequestPart MultipartFile clusterConfig) throws Exception {
        systemService.init(request, clusterConfig);
        return new EmptyResult();
    }
}


