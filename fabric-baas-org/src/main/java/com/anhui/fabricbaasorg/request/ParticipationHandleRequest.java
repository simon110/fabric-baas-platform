package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.request.NetworkOrganizationRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "处理加入网络申请请求")
public class ParticipationHandleRequest extends NetworkOrganizationRequest {

    @NotNull
    @ApiModelProperty(value = "是否通过加入网络申请", required = true)
    private boolean isAccepted;
}
