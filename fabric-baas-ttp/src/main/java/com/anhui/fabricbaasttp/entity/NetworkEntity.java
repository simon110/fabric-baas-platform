package com.anhui.fabricbaasttp.entity;

import com.anhui.fabricbaasttp.bean.Orderer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "network")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "网络信息")
public class NetworkEntity {
    @ApiModelProperty(value = "网络名称", required = true)
    @Id
    private String name;

    @ApiModelProperty(value = "所有Orderer节点", required = true)
    private List<Orderer> orderers;

    @ApiModelProperty(value = "网络中所有的组织名称", required = true)
    private List<String> organizationNames;

    @ApiModelProperty(value = "联盟名称", required = true)
    private String consortiumName;
}
