package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "已安装的链码信息")
public class InstalledChaincode {
    @ApiModelProperty(value = "安装链码后得到的唯一标识符", required = true)
    private String identifier;

    @ApiModelProperty(value = "安装链码时所填写的标签", required = true)
    private String label;
}
