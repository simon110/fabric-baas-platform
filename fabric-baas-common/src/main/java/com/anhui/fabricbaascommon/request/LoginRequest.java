package com.anhui.fabricbaascommon.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "登录请求")
public class LoginRequest extends BaseOrganizationRequest {
    @NotNull
    @Size(min = 8, message = "密码的长度最少为8")
    @ApiModelProperty(value = "登录密码", required = true)
    private String password;
}
