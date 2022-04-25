package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.KubernetesPort;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "Orderer添加请求")
public class ChannelAddOrdererRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @Range(min = KubernetesPort.MIN_VALUE, max = KubernetesPort.MAX_VALUE, message = "端口范围必须位于[30000, 32767]区间内")
    @ApiModelProperty(value = "Orderer节点的端口（必须已经添加到网络）", required = true)
    private int ordererPort;
}
