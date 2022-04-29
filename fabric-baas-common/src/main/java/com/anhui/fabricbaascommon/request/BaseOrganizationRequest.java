package com.anhui.fabricbaascommon.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "基本组织查询请求")
public class BaseOrganizationRequest {
    @Pattern(regexp = ParamPattern.ORG_NAME_REGEX, message = ParamPattern.ORG_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;
}
