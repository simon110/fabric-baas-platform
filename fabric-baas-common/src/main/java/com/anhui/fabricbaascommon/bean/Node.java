package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.constant.KubernetesPort;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "区块链节点基本信息")
public class Node {
    @Pattern(regexp = ParamPattern.HOST_REGEX, message = ParamPattern.HOST_MSG)
    @ApiModelProperty(value = "域名", required = true)
    private String host;

    @Range(min = KubernetesPort.MIN_VALUE, max = KubernetesPort.MAX_VALUE, message = "端口范围必须位于[30000, 32767]区间内")
    @ApiModelProperty(value = "端口", required = true)
    private int port;

    public String getAddr() {
        return host + ":" + port;
    }
}