package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@ApiModel(value = "通道创建请求")
@Data
public class ChannelCreateRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称（一个网络可以包含多个通道）", required = true)
    private String channelName;
}
