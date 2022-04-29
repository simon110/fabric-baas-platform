package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.constant.ParamRange;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "区块链节点基本信息")
public class Node {
    @Pattern(regexp = ParamPattern.HOST_REGEX, message = ParamPattern.HOST_MSG)
    @ApiModelProperty(value = "域名", required = true)
    private String host;

    @Range(min = ParamRange.MIN_KUBE_PORT, max = ParamRange.MAX_KUBE_PORT, message = ParamRange.KUBE_PORT_MSG)
    @ApiModelProperty(value = "端口", required = true)
    private int port;

    public String getAddr() {
        return host + ":" + port;
    }
}