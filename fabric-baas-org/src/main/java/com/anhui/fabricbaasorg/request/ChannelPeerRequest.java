package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "通道相关的Peer节点操作请求")
public class ChannelPeerRequest extends BaseChannelRequest {
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "节点名称", required = true)
    private String peerName;
}
