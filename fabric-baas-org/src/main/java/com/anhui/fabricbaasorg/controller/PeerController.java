package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.request.PeerStartRequest;
import com.anhui.fabricbaasorg.service.PeerService;
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
@RequestMapping("/api/v1/peer")
@Api(tags = "Peer管理模块", value = "Peer管理相关接口")
public class PeerController {
    @Autowired
    private PeerService peerService;

    @Secured({Authority.ADMIN})
    @PostMapping("/startPeer")
    @ApiOperation("启动Peer节点")
    public void startPeer(@Valid @RequestBody PeerStartRequest request) throws Exception {
        peerService.startPeer(request);
    }


    @Secured({Authority.ADMIN})
    @PostMapping("/queryPeersInCluster")
    @ApiOperation("获取组织所有的Peer节点")
    public PaginationQueryResult<PeerEntity> queryPeersInCluster(PaginationQueryRequest request) {
        Page<PeerEntity> page = peerService.queryPeersInCluster(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }
}
