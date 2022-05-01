package com.anhui.fabricbaasorg.entity;


import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.constant.ParamRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Document(collection = "peer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "Peer节点信息")
public class PeerEntity {
    @Id
    @ApiModelProperty(value = "Peer名称", required = true)
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    private String name;

    @ApiModelProperty(value = "物理节点名称", required = true)
    @NotBlank
    private String kubeNodeName;

    @ApiModelProperty(value = "主端口（对应7051）", required = true)
    @Range(min = ParamRange.MIN_KUBERNETES_PORT, max = ParamRange.MAX_KUBERNETES_PORT, message = ParamRange.KUBERNETES_PORT_MSG)
    private Integer kubeNodePort;

    @ApiModelProperty(value = "事件监听端口（对应7053）", required = true)
    @Range(min = ParamRange.MIN_KUBERNETES_PORT, max = ParamRange.MAX_KUBERNETES_PORT, message = ParamRange.KUBERNETES_PORT_MSG)
    private Integer kubeEventNodePort;

    @JsonIgnore
    @ApiModelProperty(value = "Peer CA服务证书账号", required = true)
    @Pattern(regexp = ParamPattern.CA_USERNAME_REGEX, message = ParamPattern.CA_USERNAME_MSG)
    private String caUsername;

    @JsonIgnore
    @ApiModelProperty(value = "Peer CA服务证书密码", required = true)
    @Pattern(regexp = ParamPattern.CA_PASSWORD_REGEX, message = ParamPattern.CA_PASSWORD_MSG)
    private String caPassword;

    @ApiModelProperty(value = "Peer Couchdb账号", required = true)
    @JsonIgnore
    @Pattern(regexp = ParamPattern.COUCHDB_USERNAME_REGEX, message = ParamPattern.COUCHDB_USERNAME_MSG)
    private String couchDBUsername;

    @ApiModelProperty(value = "Peer Couchdb密码", required = true)
    @JsonIgnore
    @Pattern(regexp = ParamPattern.COUCHDB_PASSWORD_REGEX, message = ParamPattern.COUCHDB_PASSWORD_MSG)
    private String couchDBPassword;
}


