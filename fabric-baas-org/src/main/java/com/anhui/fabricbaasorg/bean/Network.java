package com.anhui.fabricbaasorg.bean;

import com.anhui.fabricbaasorg.entity.OrdererEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class Network {
    @ApiModelProperty(value = "网络名称", required = true)
    private String name;

    @ApiModelProperty(value = "所有Orderer节点的地址（例如orderer.example.com:7050）", required = true)
    private List<OrdererEntity> orderers;

    @ApiModelProperty(value = "网络中所有的组织名称", required = true)
    private List<String> organizationNames;

    @ApiModelProperty(value = "联盟名称", required = true)
    private String consortiumName;
}