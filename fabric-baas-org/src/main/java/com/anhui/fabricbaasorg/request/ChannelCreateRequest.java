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
@ApiModel(value = "通道创建请求")
public class ChannelCreateRequest extends BaseChannelRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;
}
