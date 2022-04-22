package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "通道内的Peer查询请求")
public class ChannelQueryPeerRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;
}
