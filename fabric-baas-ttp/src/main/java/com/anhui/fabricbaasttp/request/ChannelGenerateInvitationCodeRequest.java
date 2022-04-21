package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "获取邀请指定组织加入通道的邀请码请求")
public class ChannelGenerateInvitationCodeRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @Pattern(regexp = ParamPattern.ORG_NAME_REGEX, message = ParamPattern.ORG_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;
}
