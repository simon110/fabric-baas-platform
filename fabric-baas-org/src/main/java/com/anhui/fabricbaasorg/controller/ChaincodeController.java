package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.bean.ChaincodeApproval;
import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaasorg.request.ChaincodeExecuteQueryRequest;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.service.ChaincodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chaincode")
@Api(tags = "链码管理模块", value = "链码管理相关接口")
public class ChaincodeController {
    @Autowired
    private ChaincodeService chaincodeService;

    @Secured({Authority.ADMIN})
    @PostMapping("/install")
    @ApiOperation("安装链码到指定Peer节点")
    public UniqueResult<String> install(@Valid @RequestPart ChaincodeInstallRequest request, @RequestPart MultipartFile chaincodePackage) throws Exception {
        String packageId = chaincodeService.install(request.getPeerName(), request.getChaincodeLabel(), chaincodePackage);
        return new UniqueResult<>(packageId);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/approve")
    @ApiOperation("赞同指定参数的链码安装到指定通道上")
    public void approve(@Valid @RequestBody ChaincodeApproveRequest request) throws Exception {
        chaincodeService.approve(request.getPeerName(), request.getInstalledChaincodeIdentifier(), request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getChaincodeApprovals")
    @ApiOperation("查询指定链码的投票情况")
    public ListResult<ChaincodeApproval> getChaincodeApprovals(@Valid @RequestBody ChaincodeCheckRequest request) throws Exception {
        List<ChaincodeApproval> chaincodeApprovals = chaincodeService.getChaincodeApprovals(request);
        return new ListResult<>(chaincodeApprovals);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/commit")
    @ApiOperation("让指定参数的链码在指定通道上生效（需要通道里所有的组织都安装并赞同）")
    public void commit(@Valid @RequestBody ChaincodeCommitRequest request) throws Exception {
        chaincodeService.commit(request.getEndorserPeers(), request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/queryApprovedChaincodes")
    @ApiOperation("查询组织端已投票的所有链码")
    public PaginationQueryResult<ApprovedChaincodeEntity> queryApprovedChaincodes(@Valid @RequestBody PaginationQueryRequest request) {
        Page<ApprovedChaincodeEntity> page = chaincodeService.queryApprovedChaincodes(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
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
    public PaginationQueryResult<ApprovedChaincodeEntity> queryCommittedChaincodes(@Valid @RequestBody PaginationQueryRequest request) {
        Page<ApprovedChaincodeEntity> page = chaincodeService.queryCommittedChaincodes(request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getAllInstalledChaincodesOnPeer")
    @ApiOperation("查询指定Peer上已安装的所有链码")
    public ListResult<InstalledChaincodeEntity> queryAllInstalledChaincodesOnPeer(@Valid @RequestBody BasePeerRequest request) {
        return new ListResult<>(chaincodeService.getAllInstalledChaincodesOnPeer(request.getPeerName()));
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getAllCommittedChaincodesOnChannel")
    @ApiOperation("查询指定Peer上已生效的所有链码")
    public ListResult<ApprovedChaincodeEntity> queryAllCommittedChaincodesOnChannel(@Valid @RequestBody BaseChannelRequest request) {
        return new ListResult<>(chaincodeService.getAllCommittedChaincodesOnChannel(request.getChannelName()));
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/executeQuery")
    @ApiOperation("对指定智能合约进行查询操作")
    public UniqueResult<String> executeQuery(@Valid @RequestBody ChaincodeExecuteQueryRequest request) throws Exception {
        String result = chaincodeService.executeQuery(
                request.getChaincodeName(),
                request.getChannelName(),
                request.getFunctionName(),
                request.getParams(),
                request.getPeerName()
        );
        return new UniqueResult<>(result);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/executeInvoke")
    @ApiOperation("对指定智能合约进行调用操作")
    public void executeInvoke(@Valid @RequestBody ChaincodeExecuteInvokeRequest request) throws Exception {
        chaincodeService.executeInvoke(
                request.getChaincodeName(),
                request.getChannelName(),
                request.getFunctionName(),
                request.getParams(),
                request.getPeerName(),
                request.getEndorserPeers()
        );
    }
}

