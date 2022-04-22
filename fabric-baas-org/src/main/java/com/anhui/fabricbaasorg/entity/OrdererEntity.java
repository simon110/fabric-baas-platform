package com.anhui.fabricbaasorg.entity;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orderer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "Orderer节点信息")
public class OrdererEntity {
    @Id
    @ApiModelProperty(value = "Orderer名称", required = true)
    private String name;

    @ApiModelProperty(value = "物理节点名称", required = true)
    private String kubeNodeName;

    @ApiModelProperty(value = "主端口（对应7050）", required = true)
    private Integer kubeNodePort;
}
