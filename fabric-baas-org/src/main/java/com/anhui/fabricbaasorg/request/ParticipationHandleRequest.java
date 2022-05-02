package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "处理加入网络申请请求")
public class ParticipationHandleRequest extends BaseNetworkRequest {

    @Pattern(regexp = ParamPattern.ORGANIZATION_NAME_REGEX, message = ParamPattern.ORGANIZATION_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;

    @NotNull
    @ApiModelProperty(value = "是否通过加入网络申请", required = true)
    private boolean isAccepted;
}
