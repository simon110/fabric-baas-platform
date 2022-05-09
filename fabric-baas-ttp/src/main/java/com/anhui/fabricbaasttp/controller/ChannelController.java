package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import com.anhui.fabricbaascommon.request.ChannelOrganizationRequest;
import com.anhui.fabricbaascommon.request.NetworkChannelRequest;
import com.anhui.fabricbaascommon.request.OrganizationBasedPaginationQueryRequest;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.response.ResourceResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaasttp.bean.Peer;
import com.anhui.fabricbaasttp.entity.ChannelEntity;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.service.ChannelService;
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
@RequestMapping("/api/v1/channel")
@Api(tags = "通道管理模块", value = "通道管理相关接口")
public class ChannelController {
    @Autowired
    private ChannelService channelService;

    @Secured({Authority.USER})
    @PostMapping("/createChannel")
    @ApiOperation("创建通道")
    public void createChannel(@Valid @RequestBody NetworkChannelRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        channelService.createChannel(currentOrganizationName, request.getChannelName(), request.getNetworkName());
    }

    @Secured({Authority.USER})
    @PostMapping("/joinChannel")
    @ApiOperation("将组织的Peer节点加入到通道中")
    public void joinChannel(
            @Valid @RequestPart ChannelPeerRequest request,
            @ApiParam(value = "Peer的证书压缩包（包含msp和tls两个文件夹）") @RequestPart MultipartFile peerCertZip) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        channelService.joinChannel(currentOrganizationName, request.getChannelName(), request.getPeer(), peerCertZip);
    }

    @Secured({Authority.USER})
    @PostMapping("/submitInvitationCodes")
    @ApiOperation("向通道中添加组织（必须是已经在网络中的组织）")
    public void submitInvitationCodes(@Valid @RequestBody ChannelSubmitInvitationCodesRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        channelService.submitInvitationCodes(currentOrganizationName, request.getChannelName(), request.getInvitationCodes());
    }

    @Secured({Authority.USER})
    @PostMapping("/generateInvitationCode")
    @ApiOperation("生成加入通道的邀请码")
    public UniqueResult<String> generateInvitationCode(@Valid @RequestBody ChannelOrganizationRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String invitationCode = channelService.generateInvitationCode(currentOrganizationName, request.getChannelName(), request.getOrganizationName());
        return new UniqueResult<>(invitationCode);
    }

    @Secured({Authority.USER})
    @PostMapping("/setAnchorPeer")
    @ApiOperation("设置组织在通道中的锚节点（原有的锚节点不受影响）")
    public void setAnchorPeer(@Valid @RequestBody ChannelPeerRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        channelService.setAnchorPeer(currentOrganizationName, request.getChannelName(), request.getPeer());
    }

    @Secured({Authority.USER})
    @PostMapping("/queryPeerTlsCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Peer节点的tls/ca.crt")
    public ResourceResult queryPeerTlsCert(@Valid @RequestBody ChannelPeerRequest request) throws Exception {
        String currentOrganizationName = SecurityUtils.getUsername();
        String downloadUrl = channelService.queryPeerTlsCert(currentOrganizationName, request.getChannelName(), request.getPeer());
        return new ResourceResult(downloadUrl);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryPeers")
    @ApiOperation("查询当前组织所参与的任意网络中所有Peer节点")
    public ListResult<Peer> queryPeers(@Valid @RequestBody BaseChannelRequest request) throws Exception {
        List<Peer> peers = channelService.queryPeers(request.getChannelName());
        return new ListResult<>(peers);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/getChannel")
    @ApiOperation("查询指定通道的详细信息")
    public UniqueResult<ChannelEntity> getChannel(@Valid @RequestBody BaseChannelRequest request) throws Exception {
        ChannelEntity channel = channelService.findChannelOrThrowEx(request.getChannelName());
        return new UniqueResult<>(channel);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrganizationChannels")
    @ApiOperation("查询指定组织参与的所有通道")
    public PaginationQueryResult<ChannelEntity> queryOrganizationChannels(@Valid @RequestBody OrganizationBasedPaginationQueryRequest request) {
        Page<ChannelEntity> page = channelService.getOrganizationChannels(request.getOrganizationName(), request.getPage(), request.getPageSize());
        return new PaginationQueryResult<>(page.getTotalPages(), page.getContent());
    }
}
