package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaasorg.entity.CommittedChaincodeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码投票请求")
public class ChaincodeApproveRequest extends CommittedChaincodeEntity {
    @NotBlank
    @ApiModelProperty(value = "链码的Package ID")
    private String installedChaincodeIdentifier;
}
