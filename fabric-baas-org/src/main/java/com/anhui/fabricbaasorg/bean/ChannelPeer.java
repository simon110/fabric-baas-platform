package com.anhui.fabricbaasorg.bean;

import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ChannelPeer extends Node {
    @ApiModelProperty(value = "Peer所属的组织")
    private String organizationName;
}
