package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "区块链节点基本信息")
public class Node {
    @Pattern(regexp = ParamPattern.HOST_REGEX, message = ParamPattern.HOST_MSG)
    @ApiModelProperty(value = "域名", required = true)
    private String host;

    @Min(message = "端口必须为正整数且不小于1000", value = 1000)
    @ApiModelProperty(value = "端口", required = true)
    private int port;

    public String getAddr() {
        return host + ":" + port;
    }
}