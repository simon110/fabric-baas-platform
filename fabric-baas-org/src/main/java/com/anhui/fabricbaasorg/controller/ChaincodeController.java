package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaasorg.request.ChaincodeApproveRequest;
import com.anhui.fabricbaasorg.request.ChaincodeCommitRequest;
import com.anhui.fabricbaasorg.request.ChaincodeInstallRequest;
import com.anhui.fabricbaasorg.response.CommittedChaincodeQueryResult;
import com.anhui.fabricbaasorg.response.InstalledChaincodeQueryResult;
import com.anhui.fabricbaasorg.service.ChaincodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
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
    public EmptyResult install(@Valid @RequestPart ChaincodeInstallRequest request, @RequestPart MultipartFile chaincodePackage) throws Exception {
        chaincodeService.install(request, chaincodePackage);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/approve")
    @ApiOperation("赞同指定参数的链码安装到指定通道上")
    public EmptyResult approve(@Valid @RequestBody ChaincodeApproveRequest request) throws Exception {
        chaincodeService.approve(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/commit")
    @ApiOperation("让指定参数的链码在指定通道上生效（需要通道里所有的组织都安装并赞同）")
    public EmptyResult commit(@Valid @RequestBody ChaincodeCommitRequest request) throws Exception {
        chaincodeService.commit(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getInstalledChaincodes")
    @ApiOperation("查询指定Peer上已安装的所有链码")
    public InstalledChaincodeQueryResult getInstalledChaincodes() throws Exception {
        return chaincodeService.getInstalledChaincodes();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getCommittedChaincodes")
    @ApiOperation("查询指定Peer上已生效的所有链码")
    public CommittedChaincodeQueryResult getCommittedChaincodes() throws Exception {
        return chaincodeService.getCommittedChaincodes();
    }
}

