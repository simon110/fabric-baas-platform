package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.Participation;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.response.OrdererQueryResult;
import com.anhui.fabricbaasorg.response.PeerQueryResult;
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

    @Secured({Authority.ADMIN})
    @PostMapping("/create")
    @ApiOperation("向可信第三方请求创建网络")
    public EmptyResult create(@Valid @RequestBody NetworkCreateRequest request) throws Exception {
        networkService.create(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/addOrderer")
    @ApiOperation("向可信第三方请求添加Orderer")
    public EmptyResult addOrderer(@Valid @RequestBody NetworkAddOrdererRequest request) throws Exception {
        networkService.addOrderer(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/applyParticipation")
    @ApiOperation("向可信第三方申请加入网络")
    public EmptyResult applyParticipation(@Valid @RequestBody ParticipationApplyRequest request) throws Exception {
        networkService.applyParticipation(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/handleParticipation")
    @ApiOperation("向可信第三方发送同意或拒绝加入网络（网络中所有组织都必须同意）")
    public EmptyResult handleParticipation(@Valid @RequestBody ParticipationHandleRequest request) throws Exception {
        networkService.handleParticipation(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryParticipations")
    @ApiOperation("查询加入网络申请")
    public PaginationQueryResult<Participation> queryParticipations(@Valid @RequestBody ParticipationQueryRequest request) throws Exception {
        return networkService.queryParticipations(request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/startOrderer")
    @ApiOperation("启动Orderer节点")
    public EmptyResult startOrderer(@Valid @RequestBody OrdererStartRequest request) throws Exception {
        networkService.startOrderer(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getOrderers")
    @ApiOperation("获取组织所有的Orderer节点")
    public OrdererQueryResult getOrderers() {
        return networkService.buildOrderers();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/startPeer")
    @ApiOperation("启动Peer节点")
    public EmptyResult startPeer(@Valid @RequestBody PeerStartRequest request) throws Exception {
        networkService.startPeer(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getPeers")
    @ApiOperation("获取组织所有的Peer节点")
    public PeerQueryResult getPeers() {
        return networkService.getPeers();
    }
}
