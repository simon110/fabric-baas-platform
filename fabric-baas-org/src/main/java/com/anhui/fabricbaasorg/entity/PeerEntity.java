package com.anhui.fabricbaasorg.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "peer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "Peer节点信息")
public class PeerEntity {
    @Id
    @ApiModelProperty(value = "Peer名称", required = true)
    private String name;

    @JsonIgnore
    @ApiModelProperty(value = "Peer CA服务证书账号", required = true)
    private String caUsername;

    @JsonIgnore
    @ApiModelProperty(value = "Peer CA服务证书密码", required = true)
    private String caPassword;

    @ApiModelProperty(value = "物理节点名称", required = true)
    private String kubeNodeName;

    @ApiModelProperty(value = "主端口（对应7051）", required = true)
    private Integer kubeNodePort;

    @ApiModelProperty(value = "事件监听端口（对应7053）", required = true)
    private Integer kubeEventNodePort;

    @ApiModelProperty(value = "Peer 所属的组织名", required = true)
    private String organizationName;

    @ApiModelProperty(value = "Peer Couchdb账号", required = true)
    @JsonIgnore
    private String couchDBUsername;

    @ApiModelProperty(value = "Peer Couchdb密码", required = true)
    @JsonIgnore
    private String couchDBPassword;
}


