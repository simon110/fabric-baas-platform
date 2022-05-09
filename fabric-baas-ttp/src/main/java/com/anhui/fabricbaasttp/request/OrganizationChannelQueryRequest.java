package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "组织所在通道查询请求")
public class OrganizationChannelQueryRequest extends PaginationQueryRequest {
    @NotNull
    @Pattern(regexp = ParamPattern.ORGANIZATION_NAME_REGEX, message = ParamPattern.ORGANIZATION_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;
}
