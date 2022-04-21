package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "当前用户处理加入网络申请请求")
public class ParticipationHandleRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @Pattern(regexp = ParamPattern.ORG_NAME_REGEX, message = ParamPattern.ORG_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;

    @ApiModelProperty(value = "是否通过加入网络申请", required = true)
    private boolean isAllowed;
}
