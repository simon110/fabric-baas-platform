package com.anhui.fabricbaasorg.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码安装请求")
public class ChaincodeInstallRequest extends BasePeerRequest {
    @NotBlank
    @ApiModelProperty(value = "打包时所用的标签", required = true)
    private String chaincodeLabel;
}
