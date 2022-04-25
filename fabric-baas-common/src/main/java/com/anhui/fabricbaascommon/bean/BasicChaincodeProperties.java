package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@ApiModel(value = "链码基本标识字段")
@Data
public class BasicChaincodeProperties {
    @Id
    @ApiModelProperty(value = "链码名称", required = true)
    private String name;

    @Pattern(regexp = ParamPattern.CHAINCODE_VERSION_REGEX, message = ParamPattern.CHAINCODE_VERSION_MSG)
    @ApiModelProperty(value = "链码版本", required = true)
    private String version;

    @Min(value = 1, message = "链码序号不能小于1，且应该随着链码升级每次增加1")
    @ApiModelProperty(value = "链码序号（默认从1开始，每次更新链码增加1）", required = true)
    private Integer sequence;
}

