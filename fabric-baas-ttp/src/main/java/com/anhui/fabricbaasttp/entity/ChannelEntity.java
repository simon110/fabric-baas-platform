package com.anhui.fabricbaasttp.entity;


import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.bean.Peer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "channel")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "通道信息")
public class ChannelEntity {
    @ApiModelProperty(value = "通道名称", required = true)
    @Id
    private String name;

    @ApiModelProperty(value = "通道所属网络名称", required = true)
    private String networkName;

    @ApiModelProperty(value = "所有Orderer节点的地址（例如orderer.example.com:7050），通道的Orderer和网络的Orderer并不总是一样的", required = true)
    private List<Orderer> orderers;

    @ApiModelProperty(value = "所有Peer节点的地址（例如peer.org.example.com:7051）", required = true)
    private List<Peer> peers;

    @ApiModelProperty(value = "通道中所有的组织名称", required = true)
    private List<String> organizationNames;
}