package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.request.BaseOrdererRequest;
import com.anhui.fabricbaasorg.request.OrdererStartRequest;
import com.anhui.fabricbaasorg.service.OrdererService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/orderer")
@Api(tags = "Orderer管理模块", value = "Orderer管理相关接口")
public class OrdererController {
    @Autowired
    private OrdererService ordererService;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;

    @Secured({Authority.ADMIN})
    @PostMapping("/startOrderer")
    @ApiOperation("启动Orderer节点")
    public void startOrderer(@Valid @RequestBody OrdererStartRequest request) throws Exception {
        OrdererEntity orderer = new OrdererEntity(request.getName(), request.getKubeNodeName(), request.getKubeNodePort());
        ordererService.startOrderer(request.getNetworkName(), orderer);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/stopOrderer")
    @ApiOperation("关闭Orderer节点")
    public void stopOrderer(@Valid @RequestBody BaseOrdererRequest request) throws Exception {
        ordererService.stopOrderer(request.getOrdererName());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryOrderersInCluster")
    @ApiOperation("获取组织在集群里所有的Orderer节点")
    public PaginationQueryResult<OrdererEntity> queryOrderersInCluster(@Valid @RequestBody PaginationQueryRequest request) {
        Page<OrdererEntity> page = ordererService.queryOrderersInCluster(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryOrderersInNetwork")
    @ApiOperation("获取组织在网络中所有的Orderer节点")
    public ListResult<NetworkOrderer> queryOrderersInNetwork(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        List<NetworkOrderer> orderers = ttpNetworkApi.queryOrderers(request.getNetworkName());
        return new ListResult<>(orderers);
    }
}
