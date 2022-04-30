package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ApiModel(value = "系统初始化请求")
public class SystemInitRequest {
    @NotNull
    @ApiModelProperty(value = "组织信息", required = true)
    private CaEntity org;

    @NotNull
    @ApiModelProperty(value = "可信第三方账户信息", required = true)
    private RemoteUserEntity remoteUser;

    @Size(min = 8, message = "密码最小长度为8")
    @ApiModelProperty(value = "管理员密码", required = true)
    private String adminPassword;
}
