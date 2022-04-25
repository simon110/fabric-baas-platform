package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "证书信息")
public class Certfile {
    @Pattern(regexp = ParamPattern.CA_USERNAME_REGEX, message = ParamPattern.CA_USERNAME_MSG)
    @ApiModelProperty(value = "CA服务账号", required = true)
    private String caUsername;

    @Pattern(regexp = ParamPattern.CA_PASSWORD_REGEX, message = ParamPattern.CA_PASSWORD_MSG)
    @ApiModelProperty(value = "CA服务密码", required = true)
    private String caPassword;

    @Pattern(regexp = ParamPattern.CA_USERTYPE_REGEX, message = ParamPattern.CA_USERTYPE_MSG)
    @ApiModelProperty(value = "证书类型（包括orderer、peer、admin、client）", required = true)
    private String caUsertype;
}

