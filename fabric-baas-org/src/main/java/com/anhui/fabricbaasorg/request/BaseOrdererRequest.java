package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "基本Orderer请求")
public class BaseOrdererRequest {
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "Orderer节点名称", required = true)
    private String ordererName;
}
