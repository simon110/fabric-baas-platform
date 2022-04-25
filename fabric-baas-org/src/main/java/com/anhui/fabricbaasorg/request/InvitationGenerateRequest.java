package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "生成加入通道邀请请求")
public class InvitationGenerateRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @Pattern(regexp = ParamPattern.ORG_NAME_REGEX, message = ParamPattern.ORG_NAME_MSG)
    @ApiModelProperty(value = "允许加入的组织名称", required = true)
    private String invitedOrganizationName;
}
