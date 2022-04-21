package com.anhui.fabricbaasttp.bean;

import com.anhui.fabricbaascommon.bean.Node;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Orderer节点信息")
public class Orderer extends Node {
    @JsonIgnore
    private String caUsername;

    @JsonIgnore
    private String caPassword;

    @ApiModelProperty(value = "所属组织名称")
    private String organizationName;
}
