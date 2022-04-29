package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Channel Peer 操作请求")
public class ChannelPeerOperateRequest extends BaseChannelRequest {
    @NotNull
    @ApiModelProperty(value = "Peer节点地址信息", required = true)
    private Node peer;
}
