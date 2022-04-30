package com.anhui.fabricbaasorg.controller;

import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.response.EmptyResult;
import com.anhui.fabricbaasorg.request.*;
import com.anhui.fabricbaasorg.response.InvitationGenerateResult;
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

    @Secured({Authority.ADMIN})
    @PostMapping("/create")
    @ApiOperation("创建通道")
    public EmptyResult create(@Valid @RequestBody ChannelCreateRequest request) throws Exception {
        channelService.create(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/updateAnchor")
    @ApiOperation("更新锚节点")
    public EmptyResult updateAnchor(@Valid @RequestBody AnchorPeerUpdateRequest request) throws Exception {
        channelService.updateAnchor(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/generateInvitation")
    @ApiOperation("生成邀请信息")
    public InvitationGenerateResult generateInvitation(@Valid @RequestBody ChannelGenerateInvitationCodeRequest request) throws Exception {
        return channelService.generateInvitation(request);
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/submitInvitations")
    @ApiOperation("生成邀请信息")
    public EmptyResult submitInvitations(@Valid @RequestBody ChannelSubmitInvitationCodesRequest request) throws Exception {
        channelService.submitInvitations(request);
        return new EmptyResult();
    }

    @Secured({Authority.ADMIN})
    @PostMapping("/join")
    @ApiOperation("加入通道")
    public EmptyResult join(@Valid @RequestBody ChannelJoinRequest request) throws Exception {
        channelService.join(request);
        return new EmptyResult();
    }
}

