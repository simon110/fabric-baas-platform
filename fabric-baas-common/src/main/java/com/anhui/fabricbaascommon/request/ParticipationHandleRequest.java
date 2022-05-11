package com.anhui.fabricbaascommon.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "当前用户处理加入网络申请请求")
public class ParticipationHandleRequest extends NetworkOrganizationRequest {

    @NotNull
    @ApiModelProperty(value = "是否通过加入网络申请", required = true)
    private boolean allowed;
}
