package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "设置组织的锚节点请求")
public class ChannelSetAnchorPeerRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @ApiModelProperty(value = "加入网络的Peer信息", required = true)
    private Node peer;
}
