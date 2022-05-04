package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.Network;
import com.anhui.fabricbaasorg.bean.Participation;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.request.*;
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

    @Secured({Authority.ADMIN})
    @PostMapping("/create")
    @ApiOperation("向可信第三方请求创建网络")
    public void create(@Valid @RequestBody NetworkCreateRequest request) throws Exception {
        networkService.create(request.getNetworkName(), request.getConsortiumName(), request.getOrderers());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/addOrderer")
    @ApiOperation("向可信第三方请求添加Orderer")
    public void addOrderer(@Valid @RequestBody NetworkAddOrdererRequest request) throws Exception {
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
        ttpNetworkApi.handleParticipation(request.getNetworkName(), request.getOrganizationName(), request.isAccepted());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryParticipations")
    @ApiOperation("查询加入网络申请")
    public PaginationQueryResult<Participation> queryParticipations(@Valid @RequestBody ParticipationQueryRequest request) throws Exception {
        return ttpNetworkApi.queryParticipations(request.getNetworkName(), request.getStatus(), request.getPage(), request.getPageSize());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getParticipatedNetwork")
    @ApiOperation("查询已经加入的网络")
    public ListResult<Network> getParticipatedNetworks() throws Exception {
        return new ListResult<>(networkService.getParticipatedNetworks());
    }
}
