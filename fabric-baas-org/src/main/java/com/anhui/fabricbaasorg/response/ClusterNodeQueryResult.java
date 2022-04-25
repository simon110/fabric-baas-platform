package com.anhui.fabricbaasorg.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "集群节点查询结果")
public class ClusterNodeQueryResult {
    @ApiModelProperty(value = "集群节点名称")
    private List<String> clusterNodes;
}
