package com.anhui.fabricbaasorg.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "加入网络申请信息")
public class Participation {
    @ApiModelProperty(value = "网络名称")
    private String networkName;

    @ApiModelProperty(value = "预计提供Orderer节点地址", required = true)
    private List<String> orderers;

    @ApiModelProperty(value = "申请处理状态", required = true)
    private int status;

    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;
}