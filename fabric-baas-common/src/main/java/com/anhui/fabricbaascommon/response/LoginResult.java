package com.anhui.fabricbaascommon.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "登录结果")
public class LoginResult {
    @ApiModelProperty(value = "令牌（默认验证Header Authorization）")
    private String token;
}