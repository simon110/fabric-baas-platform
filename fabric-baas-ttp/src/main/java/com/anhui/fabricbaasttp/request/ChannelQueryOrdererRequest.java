package com.anhui.fabricbaasttp.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "通道内的Orderer查询请求")
public class ChannelQueryOrdererRequest {
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;
}
