package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "提交邀请码加入通道请求")
public class ChannelSubmitInvitationCodesRequest extends BaseChannelRequest {
    @NotEmpty
    @ApiModelProperty(value = "邀请信息（通道中所有组织的）", required = true)
    private List<String> invitationCodes;
}
