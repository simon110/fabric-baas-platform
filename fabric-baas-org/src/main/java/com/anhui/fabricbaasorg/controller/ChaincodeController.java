package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.entity.CommittedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import com.anhui.fabricbaasorg.request.BasePeerRequest;
import com.anhui.fabricbaasorg.request.ChaincodeApproveRequest;
import com.anhui.fabricbaasorg.request.ChaincodeCommitRequest;
import com.anhui.fabricbaasorg.request.ChaincodeInstallRequest;
import com.anhui.fabricbaasorg.service.ChaincodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/chaincode")
@Api(tags = "链码管理模块", value = "链码管理相关接口")
public class ChaincodeController {
    @Autowired
    private ChaincodeService chaincodeService;

    @Secured({Authority.ADMIN})
    @PostMapping("/install")
    @ApiOperation("安装链码到指定Peer节点")
    public void install(@Valid @RequestPart ChaincodeInstallRequest request, @RequestPart MultipartFile chaincodePackage) throws Exception {
        chaincodeService.install(request.getPeerName(), request.getChaincodeLabel(), chaincodePackage);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/approve")
    @ApiOperation("赞同指定参数的链码安装到指定通道上")
    public void approve(@Valid @RequestBody ChaincodeApproveRequest request) throws Exception {
        chaincodeService.approve(request.getPeerName(), request.getChannelName(), request.getInstalledChaincodeIdentifier(), request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/commit")
    @ApiOperation("让指定参数的链码在指定通道上生效（需要通道里所有的组织都安装并赞同）")
    public void commit(@Valid @RequestBody ChaincodeCommitRequest request) throws Exception {
        chaincodeService.commit(request.getPeerName(), request.getChannelName(), request.getEndorserPeers(), request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryInstalledChaincodes")
    @ApiOperation("查询组织端已安装的所有链码")
    public PaginationQueryResult<InstalledChaincodeEntity> queryInstalledChaincodes(@Valid @RequestBody PaginationQueryRequest request) {
        Page<InstalledChaincodeEntity> page = chaincodeService.queryInstalledChaincodes(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryCommittedChaincodes")
    @ApiOperation("查询组织端已生效的所有链码")
    public PaginationQueryResult<CommittedChaincodeEntity> queryCommittedChaincodes(@Valid @RequestBody PaginationQueryRequest request) {
        Page<CommittedChaincodeEntity> page = chaincodeService.queryCommittedChaincodes(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getAllInstalledChaincodesOnPeer")
    @ApiOperation("查询指定Peer上已安装的所有链码")
    public ListResult<InstalledChaincodeEntity> queryAllInstalledChaincodesOnPeer(@Valid @RequestBody BasePeerRequest request) {
        return new ListResult<>(chaincodeService.getAllInstalledChaincodesOnPeer(request.getPeerName()));
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getAllCommittedChaincodesOnPeer")
    @ApiOperation("查询指定Peer上已生效的所有链码")
    public ListResult<CommittedChaincodeEntity> queryAllCommittedChaincodesOnPeer(@Valid @RequestBody BasePeerRequest request) {
        return new ListResult<>(chaincodeService.getAllCommittedChaincodesOnChannel(request.getPeerName()));
    }
}

