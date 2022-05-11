package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseOrganizationRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "申请注册请求")
public class RegistrationApplyRequest extends BaseOrganizationRequest {
    @NotNull
    @Size(min = 8, message = "密码最小长度为8")
    @ApiModelProperty(value = "登录密码", required = true)
    private String password;

    @NotBlank(message = "描述信息不允许为空")
    @ApiModelProperty(value = "描述信息", required = true)
    private String description;

    @NotNull
    @Email(message = "请检查联系邮箱的格式是否正确")
    @ApiModelProperty(value = "联系邮箱", required = true)
    private String email;

    @NotNull
    @Pattern(regexp = ParamPattern.API_SERVER_REGEX, message = ParamPattern.API_SERVER_MSG)
    @ApiModelProperty(value = "服务服务端地址", required = true)
    private String apiServer;
}
