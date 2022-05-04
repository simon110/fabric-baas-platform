package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.ListResult;
import com.anhui.fabricbaascommon.response.UniqueResult;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.service.ChannelService;
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
@RequestMapping("/api/v1/channel")
@Api(tags = "通道管理模块", value = "通道管理相关接口")
public class ChannelController {
    @Autowired
    private ChannelService channelService;
    @Autowired
    private TTPChannelApi ttpChannelApi;

    @Secured({Authority.ADMIN})
    @PostMapping("/create")
    @ApiOperation("创建通道")
    public void create(@Valid @RequestBody ChannelCreateRequest request) throws Exception {
        channelService.create(request.getChannelName(), request.getNetworkName());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/updateAnchor")
    @ApiOperation("更新锚节点")
    public void updateAnchor(@Valid @RequestBody AnchorPeerUpdateRequest request) throws Exception {
        channelService.updateAnchor(request.getChannelName(), request.getPeerName());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/generateInvitationCode")
    @ApiOperation("生成邀请信息")
    public UniqueResult<String> generateInvitationCode(@Valid @RequestBody ChannelGenerateInvitationCodeRequest request) throws Exception {
        String invitationCode = ttpChannelApi.generateInvitationCode(request.getChannelName(), request.getInvitedOrganizationName());
        return new UniqueResult<>(invitationCode);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/submitInvitationCodes")
    @ApiOperation("生成邀请信息")
    public void submitInvitationCodes(@Valid @RequestBody ChannelSubmitInvitationCodesRequest request) throws Exception {
        ttpChannelApi.submitInvitationCodes(request.getChannelName(), request.getInvitationCodes());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/join")
    @ApiOperation("加入通道")
    public void join(@Valid @RequestBody ChannelJoinRequest request) throws Exception {
        channelService.join(request.getChannelName(), request.getPeerName());
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/getParticipatedChannels")
    @ApiOperation("查询当前组织已经加入的通道")
    public ListResult<Object> getParticipatedChannels() throws Exception {
        return new ListResult<>(channelService.getParticipatedChannels());
    }
}

