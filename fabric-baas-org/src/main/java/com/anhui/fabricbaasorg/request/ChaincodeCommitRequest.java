package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.bean.ApprovedChaincode;
import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码生效请求")
public class ChaincodeCommitRequest extends ApprovedChaincode {
    @Valid
    @ApiModelProperty(value = "所有背书节点（这些节点上已经安装的相应链码也会生效）")
    private List<Node> endorserPeers;
}
