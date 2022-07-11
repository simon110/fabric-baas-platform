package com.anhui.fabricbaasttp.bean;

import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Peer节点信息")
@ToString(callSuper = true)
public class Peer extends Node {
    @ApiModelProperty(value = "自动生成的Peer名称（不一定是CA账户名称）")
    private String name;

    @ApiModelProperty(value = "Peer所属的组织")
    private String organizationName;
}

