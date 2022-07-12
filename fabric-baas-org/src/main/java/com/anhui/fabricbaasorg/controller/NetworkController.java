package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.*;
import com.anhui.fabricbaascommon.response.PageResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.request.OrdererAddRequest;
import com.anhui.fabricbaasorg.request.NetworkCreateRequest;
import com.anhui.fabricbaasorg.service.NetworkService;
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
@RequestMapping("/api/v1/network")
@Api(tags = "网络管理模块", value = "网络管理相关接口")
public class NetworkController {
    @Autowired
    private NetworkService networkService;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private CaClientService caClientService;

    @Secured({Authority.ADMIN})
    @PostMapping("/create")
    @ApiOperation("向可信第三方请求创建网络")
    public void create(@Valid @RequestBody NetworkCreateRequest request) throws Exception {
        networkService.create(request.getNetworkName(), request.getConsortiumName(), request.getOrderers());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/addOrderer")
    @ApiOperation("向可信第三方请求添加Orderer")
    public void addOrderer(@Valid @RequestBody OrdererAddRequest request) throws Exception {
        networkService.addOrderer(request.getNetworkName(), request.getOrdererPort());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/applyParticipation")
    @ApiOperation("向可信第三方申请加入网络")
    public void applyParticipation(@Valid @RequestBody ParticipationApplyRequest request) throws Exception {
        networkService.applyParticipation(request.getNetworkName(), request.getDescription());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/handleParticipation")
    @ApiOperation("向可信第三方发送同意或拒绝加入网络（网络中所有组织都必须同意）")
    public void handleParticipation(@Valid @RequestBody ParticipationHandleRequest request) throws Exception {
        ttpNetworkApi.handleParticipation(request.getNetworkName(), request.getOrganizationName(), request.isAllowed());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryParticipations")
    @ApiOperation("查询加入网络申请")
    public PageResult<Object> queryParticipations(@Valid @RequestBody ParticipationQueryRequest request) throws Exception {
        return ttpNetworkApi.queryParticipations(request.getNetworkName(), request.getStatus(), request.getPage(), request.getPageSize());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryParticipatedNetworks")
    @ApiOperation("查询已经加入的网络")
    public PageResult<Object> queryParticipatedNetworks(@Valid @RequestBody PaginationQueryRequest request) throws Exception {
        return ttpNetworkApi.queryNetworks("", caClientService.getCaOrganizationName(), request.getPage(), request.getPageSize());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getNetwork")
    @ApiOperation("获取网络的详细信息")
    public UniqueResult<Object> getNetwork(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        return new UniqueResult<>(ttpNetworkApi.getNetwork(request.getNetworkName()));
    }
}
