package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "查询通道中Peer节点的TLS连接证书")
public class ChannelQueryPeerTlsCertRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @NotNull
    @ApiModelProperty(value = "节点地址", required = true)
    private Node peer;
}
