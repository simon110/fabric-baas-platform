package com.anhui.fabricbaasorg.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "加入网络申请信息")
public class Participation {
    @ApiModelProperty(value = "网络名称")
    private String networkName;

    @ApiModelProperty(value = "申请处理状态", required = true)
    private int status;

    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;
}