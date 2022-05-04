package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.bean.ApprovedChaincode;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码投票请求")
public class ChaincodeApproveRequest extends ApprovedChaincode {
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String peerName;

    @NotBlank
    @ApiModelProperty(value = "链码的Package ID")
    private String installedChaincodeIdentifier;
}
