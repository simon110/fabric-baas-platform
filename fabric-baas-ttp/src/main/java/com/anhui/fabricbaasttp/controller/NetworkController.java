package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.response.ResourceResult;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.entity.ParticipationEntity;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.service.NetworkService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/network")
@Api(tags = "网络管理模块", value = "网络管理相关接口")
public class NetworkController {
    @Autowired
    private NetworkService networkService;

    @Secured({Authority.USER})
    @PostMapping("/createNetwork")
    @ApiOperation("创建网络")
    public ResourceResult createNetwork(@Valid @RequestPart NetworkCreateRequest request,
                                        @ApiParam(value = "CA管理员的证书压缩包（包含msp和tls两个文件夹）") @RequestPart MultipartFile adminCertZip) throws Exception {
        return networkService.createNetwork(request, adminCertZip);
    }

    @Secured({Authority.ADMIN, Authority.USER})
    @PostMapping("/queryNetworks")
    @ApiOperation("查询网络信息")
    public PaginationQueryResult<NetworkEntity> queryNetworks(@Valid @RequestBody NetworkQueryRequest request) {
        return networkService.queryNetworks(request);
    }

    @Secured({Authority.USER})
    @PostMapping("/applyParticipation")
    @ApiOperation("申请加入网络")
    public EmptyResult applyParticipation(@Valid @RequestPart ParticipationApplyRequest request,
                                          @ApiParam(value = "CA管理员的证书压缩包（包含msp和tls两个文件夹）") @RequestPart MultipartFile adminCertZip) throws Exception {
        networkService.applyParticipation(request, adminCertZip);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/handleParticipation")
    @ApiOperation("处理加入网络请求（网络中所有组织都必须同意）")
    public EmptyResult handleParticipation(@Valid @RequestBody ParticipationHandleRequest request) throws Exception {
        networkService.handleParticipation(request);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/addOrderer")
    @ApiOperation("向网络中添加Orderer")
    public ResourceResult addOrderer(@Valid @RequestBody NetworkAddOrdererRequest request) throws Exception {
        return networkService.addOrderer(request);
    }

    @Secured({Authority.ADMIN, Authority.USER})
    @PostMapping("/queryParticipations")
    @ApiOperation("查询加入网络申请")
    public PaginationQueryResult<ParticipationEntity> queryParticipations(@Valid @RequestBody ParticipationQueryRequest request) throws Exception {
        return networkService.queryParticipations(request);
    }

    @Secured({Authority.USER})
    @PostMapping("/queryGenesisBlock")
    @ApiOperation("查询当前组织所参与的任意网络的创世区块")
    public ResourceResult queryGenesisBlock(@Valid @RequestBody NetworkQueryGenesisBlockRequest request) throws Exception {
        return networkService.queryGenesisBlock(request);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrdererTlsCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Orderer节点的tls/ca.crt")
    public ResourceResult queryOrdererTlsCert(@Valid @RequestBody NetworkQueryOrdererTlsCertRequest request) throws Exception {
        return networkService.queryOrdererTlsCert(request);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrdererCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Orderer节点的证书（包括MSP和TLS，只有所属组织可以下载）")
    public ResourceResult queryOrdererCert(@Valid @RequestBody NetworkQueryOrdererCertRequest request) throws Exception {
        return networkService.queryOrdererCert(request);
    }
}
