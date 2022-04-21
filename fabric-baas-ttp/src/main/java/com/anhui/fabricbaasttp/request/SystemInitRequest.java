package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaasttp.entity.TTPEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ApiModel(value = "系统初始化请求")
public class SystemInitRequest {
    @Size(min = 8, message = "密码最小长度为8")
    @ApiModelProperty(value = "管理员密码", required = true)
    private String adminPassword;

    @NotNull
    @ApiModelProperty(value = "可信第三方信息", required = true)
    private TTPEntity ttp;
}