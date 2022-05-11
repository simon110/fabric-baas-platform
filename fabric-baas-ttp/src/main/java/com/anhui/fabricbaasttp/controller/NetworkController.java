package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import com.anhui.fabricbaascommon.request.ParticipationApplyRequest;
import com.anhui.fabricbaascommon.request.ParticipationHandleRequest;
import com.anhui.fabricbaascommon.request.ParticipationQueryRequest;
import com.anhui.fabricbaascommon.response.*;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.entity.ParticipationEntity;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.service.NetworkService;
import com.anhui.fabricbaasweb.util.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

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
        String currentOrganizationName = SecurityUtils.getUsername();
        String sysChannelGenesisDownloadUrl = networkService.createNetwork(currentOrganizationName, request.getNetworkName(), request.getConsortiumName(), request.getOrderers(), adminCertZip);
        return new ResourceResult(sysChannelGenesisDownloadUrl);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryNetworks")
    @ApiOperation("查询网络信息")
    public PaginationQueryResult<NetworkEntity> queryNetworks(@Valid @RequestBody NetworkQueryRequest request) {
        Page<NetworkEntity> page = networkService.queryNetworks(request.getNetworkNameKeyword(), request.getOrganizationNameKeyword(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.USER})
    @PostMapping("/applyParticipation")
    @ApiOperation("申请加入网络")
    public void applyParticipation(
            @Valid @RequestPart ParticipationApplyRequest request,
            @ApiParam(value = "CA管理员的证书压缩包（包含msp和tls两个文件夹）") @RequestPart MultipartFile adminCertZip) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        networkService.applyParticipation(currentOrganizationName, request.getNetworkName(), request.getDescription(), adminCertZip);
    }

    @Secured({Authority.USER})
    @PostMapping("/handleParticipation")
    @ApiOperation("处理加入网络请求（网络中所有组织都必须同意）")
    public void handleParticipation(@Valid @RequestBody ParticipationHandleRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        networkService.handleParticipation(currentOrganizationName, request.getNetworkName(), request.getOrganizationName(), request.isAllowed());
    }

    @Secured({Authority.USER})
    @PostMapping("/addOrderer")
    @ApiOperation("向网络中添加Orderer")
    public ResourceResult addOrderer(@Valid @RequestBody NetworkOrdererRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String downloadUrl = networkService.addOrderer(currentOrganizationName, request.getNetworkName(), request.getOrderer());
        return new ResourceResult(downloadUrl);
    }

    @Secured({Authority.USER})
    @PostMapping("/queryParticipations")
    @ApiOperation("查询加入网络申请")
    public PaginationQueryResult<ParticipationEntity> queryParticipations(@Valid @RequestBody ParticipationQueryRequest request) throws Exception {
        Page<ParticipationEntity> page = networkService.queryParticipations(request.getNetworkName(), request.getStatus(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }

    @Secured({Authority.USER})
    @PostMapping("/queryGenesisBlock")
    @ApiOperation("查询当前组织所参与的任意网络的创世区块")
    public ResourceResult queryGenesisBlock(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String downloadUrl = networkService.queryGenesisBlock(currentOrganizationName, request.getNetworkName());
        return new ResourceResult(downloadUrl);
    }

    @Secured({Authority.USER})
    @PostMapping("/queryOrdererTlsCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Orderer节点的tls/ca.crt")
    public ResourceResult queryOrdererTlsCert(@Valid @RequestBody NetworkOrdererRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String downloadUrl = networkService.queryOrdererTlsCert(currentOrganizationName, request.getNetworkName(), request.getOrderer());
        return new ResourceResult(downloadUrl);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrdererCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Orderer节点的证书（包括MSP和TLS，只有所属组织可以下载）")
    public ResourceResult queryOrdererCert(@Valid @RequestBody NetworkOrdererRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String downloadUrl = networkService.queryOrdererCert(currentOrganizationName, request.getNetworkName(), request.getOrderer());
        return new ResourceResult(downloadUrl);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrganizations")
    @ApiOperation("查询指定网络中所有的组织")
    public ListResult<String> queryOrganizations(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        List<String> organizationNames = networkService.queryOrganizations(request.getNetworkName());
        return new ListResult<>(organizationNames);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/getNetwork")
    @ApiOperation("查询指定网络的信息")
    public UniqueResult<NetworkEntity> getNetwork(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        return new UniqueResult<>(networkService.findNetworkOrThrowEx(request.getNetworkName()));
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryNetworkChannels")
    @ApiOperation("查询指定网络中的通道信息")
    public ListResult<String> queryChannels(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        List<String> channelNames = networkService.queryChannels(request.getNetworkName());
        return new ListResult<>(channelNames);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrderers")
    @ApiOperation("查询指定网络中的Orderer信息")
    public ListResult<Orderer> queryOrderers(@Valid @RequestBody BaseNetworkRequest request) throws Exception {
        List<Orderer> orderers = networkService.queryOrderers(request.getNetworkName());
        return new ListResult<>(orderers);
    }
}
