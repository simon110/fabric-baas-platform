package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.bean.ApprovedChaincode;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码生效请求")
public class ChaincodeCommitRequest extends ApprovedChaincode {
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String committerPeerName;

    @Valid
    @ApiModelProperty(value = "所有背书节点（这些节点上已经安装的相应链码也会生效）")
    private List<Node> endorserPeers;
}
