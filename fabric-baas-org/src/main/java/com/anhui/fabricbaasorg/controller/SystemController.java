package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaasorg.request.ClusterNodeQueryRequest;
import com.anhui.fabricbaasorg.request.SystemInitRequest;
import com.anhui.fabricbaasorg.service.KubernetesService;
import com.anhui.fabricbaasorg.service.SystemService;
import io.fabric8.kubernetes.api.model.Node;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
@Api(tags = "系统管理模块", value = "系统管理相关接口")
public class SystemController {
    @Autowired
    private SystemService systemService;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private KubernetesService kubernetesService;

    @Secured({Authority.ADMIN})
    @PostMapping("/getClusterNodeNames")
    @ApiOperation("获取集群的所有物理节点的名称")
    public ListResult<String> getClusterNodeNames() throws Exception {
        List<String> nodeNames = kubernetesService.getAllNodeNames();
        return new ListResult<>(nodeNames);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getClusterNode")
    @ApiOperation("获取集群指定物理节点的信息")
    public UniqueResult<Node> getClusterNode(@Valid @RequestBody ClusterNodeQueryRequest request) throws Exception {
        return new UniqueResult<>(kubernetesService.getNode(request.getNodeName()));
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/init")
    @ApiOperation("对系统管理员的账户密码和CA服务进行配置")
    public void init(
            @Valid @RequestPart SystemInitRequest request,
            @ApiParam(value = "连接Kubernetes集群的Yaml配置文件") @RequestPart MultipartFile kubernetesConfig) throws Exception {
        systemService.init(request.getOrg(), request.getRemoteUser(), request.getAdminPassword(), kubernetesConfig);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/isAvailable")
    @ApiOperation("判断当前系统是否已经初始化")
    public UniqueResult<Boolean> isAvailable() {
        return new UniqueResult<>(systemService.isAvailable());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getOrganizationName")
    @ApiOperation("获取组织用户的名称")
    public UniqueResult<String> getOrganizationName() throws CaException {
        return new UniqueResult<>(caClientService.getCaOrganizationName());
    }
}
