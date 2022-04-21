package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@ApiModel(value = "提交邀请码加入通道请求")
public class ChannelSubmitInvitationCodesRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @NotEmpty
    @ApiModelProperty(value = "邀请信息（通道中所有组织的）", required = true)
    private List<String> invitationCodes;
}
