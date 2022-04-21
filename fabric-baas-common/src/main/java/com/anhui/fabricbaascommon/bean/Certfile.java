package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel(value = "证书信息")
public class Certfile {
    @ApiModelProperty(value = "CA服务账号", required = true)
    private String caUsername;

    @ApiModelProperty(value = "CA服务密码", required = true)
    private String caPassword;

    @ApiModelProperty(value = "证书类型（包括orderer、peer、admin、client）", required = true)
    private String caUsertype;
}

