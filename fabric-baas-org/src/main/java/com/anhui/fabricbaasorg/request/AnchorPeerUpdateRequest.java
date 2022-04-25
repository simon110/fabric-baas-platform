package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "锚节点更新请求")
public class AnchorPeerUpdateRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @Pattern(regexp = ParamPattern.NODE_NAME_REGEX, message = ParamPattern.NODE_NAME_MSG)
    @ApiModelProperty(value = "组织锚节点名称", required = true)
    private String peerName;
}
