package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamRange;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Orderer启动")
public class OrdererStartRequest extends BaseNetworkRequest {
    @Pattern(regexp = ParamPattern.NODE_NAME_REGEX, message = ParamPattern.NODE_NAME_MSG)
    @ApiModelProperty(value = "Orderer名称", required = true)
    private String name;

    @NotBlank
    @ApiModelProperty(value = "物理节点名称", required = true)
    private String kubeNodeName;

    @Range(min = ParamRange.MIN_KUBERNETES_PORT, max = ParamRange.MAX_KUBERNETES_PORT, message = ParamRange.KUBERNETES_PORT_MSG)
    @ApiModelProperty(value = "主端口（对应7050）", required = true)
    private int kubeNodePort;
}
