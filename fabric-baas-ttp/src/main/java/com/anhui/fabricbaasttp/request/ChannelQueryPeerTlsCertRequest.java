package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "查询通道中Peer节点的TLS连接证书")
public class ChannelQueryPeerTlsCertRequest {
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @ApiModelProperty(value = "节点地址", required = true)
    private Node peer;
}
