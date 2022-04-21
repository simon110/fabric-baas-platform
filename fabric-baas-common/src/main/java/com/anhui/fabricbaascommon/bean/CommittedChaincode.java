package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "已生效的链码")
public class CommittedChaincode extends ChaincodeBasicProperties {
    @ApiModelProperty(value = "链码所在的通道", required = true)
    private String channelName;
}

