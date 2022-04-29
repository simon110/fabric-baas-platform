package com.anhui.fabricbaasttp.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaascommon.response.ResourceResult;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.response.ChannelQueryOrdererResult;
import com.anhui.fabricbaasttp.response.ChannelQueryPeerResult;
import com.anhui.fabricbaasttp.response.InvitationCodeResult;
import com.anhui.fabricbaasttp.service.ChannelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/channel")
@Api(tags = "通道管理模块", value = "通道管理相关接口")
public class ChannelController {
    @Autowired
    private ChannelService channelService;

    @Secured({Authority.USER})
    @PostMapping("/createChannel")
    @ApiOperation("创建通道")
    public EmptyResult createChannel(@Valid @RequestBody ChannelCreateRequest request) throws Exception {
        channelService.createChannel(request);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/addOrderer")
    @ApiOperation("向通道中添加Orderer（必须是已经存在于网络中的Orderer）")
    public EmptyResult addOrderer(@Valid @RequestBody ChannelAddOrdererRequest request) throws Exception {
        channelService.addOrderer(request);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/joinChannel")
    @ApiOperation("将组织的Peer节点加入到通道中")
    public EmptyResult joinChannel(@Valid @RequestPart ChannelJoinRequest request,
                                   @ApiParam(value = "Peer的证书压缩包（包含msp和tls两个文件夹）") @RequestPart MultipartFile peerCertZip) throws Exception {
        channelService.joinChannel(request, peerCertZip);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/submitInvitationCodes")
    @ApiOperation("向通道中添加组织（必须是已经在网络中的组织）")
    public EmptyResult submitInvitationCodes(@Valid @RequestBody ChannelSubmitInvitationCodesRequest request) throws Exception {
        channelService.submitInvitationCodes(request);
        return new EmptyResult();
    }

    @Secured({Authority.USER})
    @PostMapping("/generateInvitationCode")
    @ApiOperation("生成加入通道的邀请码")
    public InvitationCodeResult generateInvitationCode(@Valid @RequestBody ChannelGenerateInvitationCodeRequest request) throws Exception {
        return channelService.generateInvitationCode(request);
    }

    @Secured({Authority.USER})
    @PostMapping("/setAnchorPeer")
    @ApiOperation("设置组织在通道中的锚节点（原有的锚节点不受影响）")
    public EmptyResult setAnchorPeer(@Valid @RequestBody ChannelSetAnchorPeerRequest request) throws Exception {
        channelService.setAnchorPeer(request);
        return new EmptyResult();
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryPeerTlsCert")
    @ApiOperation("查询当前组织所参与的任意网络中指定Peer节点的tls/ca.crt")
    public ResourceResult queryPeerTlsCert(@Valid @RequestBody ChannelQueryPeerTlsCertRequest request) throws Exception {
        return channelService.queryPeerTlsCert(request);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryPeers")
    @ApiOperation("查询当前组织所参与的任意网络中所有Peer节点")
    public ChannelQueryPeerResult queryPeers(@Valid @RequestBody ChannelQueryPeerRequest request) throws Exception {
        return channelService.queryPeers(request);
    }

    @Secured({Authority.USER, Authority.ADMIN})
    @PostMapping("/queryOrderers")
    @ApiOperation("查询当前组织所参与的任意网络中所有Orderer节点")
    public ChannelQueryOrdererResult queryOrderers(@Valid @RequestBody ChannelQueryOrdererRequest request) throws Exception {
        return channelService.queryOrderers(request);
    }
}
