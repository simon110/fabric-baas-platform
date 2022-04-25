package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

@ApiModel(value = "链码基本标识字段")
@Data
public class BasicChaincodeProperties {
    @Id
    @ApiModelProperty(value = "链码名称", required = true)
    private String name;

    @ApiModelProperty(value = "链码版本", required = true)
    private String version;

    @ApiModelProperty(value = "链码序号（默认从1开始，每次更新链码增加1）", required = true)
    private Integer sequence;
}

