package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.KubernetesPort;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "Orderer添加请求")
public class NetworkAddOrdererRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @Range(min = KubernetesPort.MIN_VALUE, max = KubernetesPort.MAX_VALUE, message = "端口范围必须位于[30000, 32767]区间内")
    @ApiModelProperty(value = "Orderer节点端口", required = true)
    private int ordererPort;
}

