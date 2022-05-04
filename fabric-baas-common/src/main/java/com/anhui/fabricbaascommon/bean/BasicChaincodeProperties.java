package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@ApiModel(value = "链码基本属性")
@Data
public class BasicChaincodeProperties {
    @NotBlank
    @ApiModelProperty(value = "链码名称", required = true)
    private String name;

    @NotBlank
    @ApiModelProperty(value = "链码版本", required = true)
    private String version;

    @Min(value = 1, message = "链码序号不能小于1，且应该随着链码升级每次增加1")
    @ApiModelProperty(value = "链码序号", required = true)
    private Integer sequence;
}

