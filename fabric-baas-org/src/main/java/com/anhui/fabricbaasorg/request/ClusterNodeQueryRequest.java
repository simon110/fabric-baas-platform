package com.anhui.fabricbaasorg.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "集群节点查询请求")
public class ClusterNodeQueryRequest {
    @NotBlank
    @ApiModelProperty(value = "节点名称", required = true)
    private String nodeName;
}
