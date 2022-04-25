package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.KubernetesPort;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "Peer启动请求")
public class PeerStartRequest {
    @Pattern(regexp = ParamPattern.NODE_NAME_REGEX, message = ParamPattern.NODE_NAME_MSG)
    @ApiModelProperty(value = "Peer名称", required = true)
    private String name;

    @NotBlank
    @ApiModelProperty(value = "物理节点名称", required = true)
    private String kubeNodeName;

    @Range(min = KubernetesPort.MIN_VALUE, max = KubernetesPort.MAX_VALUE, message = "端口范围必须位于[30000, 32767]区间内")
    @ApiModelProperty(value = "主端口（对应7051）", required = true)
    private Integer kubeNodePort;

    @Range(min = KubernetesPort.MIN_VALUE, max = KubernetesPort.MAX_VALUE, message = "端口范围必须位于[30000, 32767]区间内")
    @ApiModelProperty(value = "为SDK提供服务的事件端口（对应7053）", required = true)
    private Integer kubeEventNodePort;
}
